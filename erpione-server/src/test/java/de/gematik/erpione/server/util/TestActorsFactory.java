/*
 * Copyright (Change Date see Readme) gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */

package de.gematik.erpione.server.util;

import static org.mockito.Mockito.mock;

import de.gematik.bbriccs.konnektor.Konnektor;
import de.gematik.bbriccs.konnektor.KonnektorImpl;
import de.gematik.bbriccs.konnektor.SofKonServicePort;
import de.gematik.bbriccs.konnektor.cfg.KonnektorContextConfiguration;
import de.gematik.bbriccs.rest.HttpBClient;
import de.gematik.bbriccs.smartcards.SmartcardArchive;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.actor.HealthcareProvider;
import de.gematik.erpione.server.actor.impl.ErpioneActorBuilder;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.test.erezept.client.ErpClient;
import java.util.List;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.data.util.Pair;

@TestComponent
public class TestActorsFactory {

  @Autowired private HermesService hermes;
  @Autowired private ErpionePersistence repository;
  private final SmartcardArchive sca = SmartcardArchive.fromResources();

  public Pair<ErpClient, HealthcareProvider> getTestDoctor(String mnemonic) {
    val erpClient = mock(ErpClient.class);
    val doctor =
        ErpioneActorBuilder.builderFor(mnemonic, InstitutionERole.HEALTH_FACILITY)
            .erpClient(erpClient)
            .hba(sca.getHba(0))
            .hermes(hermes)
            .konnektor(createKonnektor())
            .repository(repository)
            .build()
            .getSecond(); // basically throw away the institution object
    doctor.initialize();
    return Pair.of(erpClient, doctor);
  }

  public Pair<ErpClient, HealthcareInstitution> getTestPharmacy(String mnemonic) {
    val erpClient = mock(ErpClient.class);
    val institution =
        ErpioneActorBuilder.builderFor(mnemonic, InstitutionERole.PHARMACY)
            .erpClient(erpClient)
            .hba(sca.getHbaByICCSN("80276001011699901726"))
            .smcB(sca.getSmcbByICCSN("80276001011699902301"))
            .hermes(hermes)
            .konnektor(createKonnektor())
            .repository(repository)
            .buildInstitution();
    institution.initialize();
    return Pair.of(erpClient, institution);
  }

  public Pair<HttpBClient, HealthcareInstitution> getTestPharmacyForHealthCheck(String mnemonic) {
    val erpProbingClient = mock(HttpBClient.class);
    val erpClient = mock(ErpClient.class);
    val institution =
        ErpioneActorBuilder.builderFor(mnemonic, InstitutionERole.PHARMACY)
            .erpClient(erpClient)
            .erpProbingClient(erpProbingClient)
            .hba(sca.getHbaByICCSN("80276001011699901726"))
            .smcB(sca.getSmcbByICCSN("80276001011699902301"))
            .hermes(hermes)
            .konnektor(createKonnektor())
            .repository(repository)
            .buildInstitution();
    institution.initialize();
    return Pair.of(erpProbingClient, institution);
  }

  public Pair<ErpClient, HealthcareInstitution> getTestEuPharmacy(String mnemonic) {
    val erpClient = mock(ErpClient.class);
    val institution =
        ErpioneActorBuilder.builderFor(mnemonic, InstitutionERole.NCP_EH)
            .erpClient(erpClient)
            .smcB(sca.getSmcB(0))
            // TODO: EuPharmacyRequires an own class to avoid the need of having a konnektor!
            .konnektor(createKonnektor())
            .hermes(hermes)
            .repository(repository)
            .buildInstitution();
    institution.initialize();
    return Pair.of(erpClient, institution);
  }

  public Konnektor createKonnektor() {
    val konnektorContext = KonnektorContextConfiguration.getDefaultContextType();
    val softKon = new SofKonServicePort(sca);
    return new KonnektorImpl(konnektorContext, softKon, List.of());
  }
}
