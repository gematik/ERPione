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

package de.gematik.erpione.server.actor.impl;

import de.gematik.bbriccs.konnektor.Konnektor;
import de.gematik.bbriccs.rest.HttpBClient;
import de.gematik.bbriccs.smartcards.Hba;
import de.gematik.bbriccs.smartcards.SmcB;
import de.gematik.erpione.data.actor.ActorERole;
import de.gematik.erpione.data.actor.ErpioneActorData;
import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.actor.HealthcareProvider;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.fhir.valuesets.IsoCountryCodeNCPeH;
import java.util.Optional;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.util.Pair;

@Data
@Accessors(fluent = true)
public class ErpioneActorBuilder implements InstitutionBuilder, HealthcareProviderBuilder {
  private final String mnemonic;
  private final ErpioneInstitutionData info;

  private IsoCountryCodeNCPeH countryCode = IsoCountryCodeNCPeH.DE;
  @Nullable private String customName;
  private Hba hba;
  private SmcB smcB;
  private ErpClient erpClient;
  private HttpBClient erpProbingClient;
  private Konnektor konnektor;

  private HermesService hermes;
  private ErpionePersistence repository;

  private ErpioneActorBuilder(String mnemonic, InstitutionERole role) {
    this.mnemonic = mnemonic;
    val id = MnemonicUtils.mnemonicToId(mnemonic);
    this.info = new ErpioneInstitutionData(id, mnemonic, "n/a", role);
  }

  public ErpioneActorBuilder hba(Hba hba) {
    this.hba = hba;
    val name = MnemonicUtils.extractCommonNameFrom(this.hba);
    val hbaInfo = MnemonicUtils.extractInfo(this.hba);
    val role =
        Optional.ofNullable(this.info().getRole().getEmployeeType())
            .orElse(ActorERole.DOCTOR); // fallback should never happen
    val actorInfo = new ErpioneActorData(name, role, hbaInfo);
    this.info.setPractitioner(actorInfo);
    return this;
  }

  public ErpioneActorBuilder smcB(SmcB smcB) {
    return this.smcB(smcB, smcB.getOwnerData().getCountry());
  }

  public ErpioneActorBuilder smcB(SmcB smcB, @Nullable String countryCode) {
    this.smcB = smcB;
    this.info.setSmcb(MnemonicUtils.extractInfo(this.smcB));
    this.info.setName(MnemonicUtils.extractCommonNameFrom(this.smcB));

    val cc2 = Optional.ofNullable(countryCode).filter(it -> !it.isEmpty()).orElse("n/a");
    this.info.setCountryCode(cc2);
    return this;
  }

  public HealthcareInstitution buildInstitution() {
    Optional.ofNullable(this.customName).ifPresent(this.info::setName);
    return new LEInstitution(this);
  }

  public Pair<HealthcareInstitution, HealthcareProvider> build() {
    val lei = buildInstitution();
    val le = new LEActor(this);
    return Pair.of(lei, le);
  }

  // TODO: EuPharmacyRequires an own class to avoid the need of having a konnektor!

  public static ErpioneActorBuilder doctor(String mnemonic) {
    return builderFor(mnemonic, InstitutionERole.HEALTH_FACILITY);
  }

  public static ErpioneActorBuilder pharmacist(String mnemonic) {
    return builderFor(mnemonic, InstitutionERole.PHARMACY);
  }

  public static ErpioneActorBuilder healthInsurance(String mnemonic) {
    return builderFor(mnemonic, InstitutionERole.HEALTH_INSURANCE);
  }

  public static ErpioneActorBuilder builderFor(String mnemonic, InstitutionERole role) {
    return new ErpioneActorBuilder(mnemonic, role);
  }
}
