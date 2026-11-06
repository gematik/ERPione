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

package de.gematik.erpione.server.actor;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.actor.error.ActorNotFoundError;
import de.gematik.erpione.server.actor.impl.ErpioneActorBuilder;
import de.gematik.erpione.server.actor.impl.MnemonicUtils;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ErpioneActorServiceTest {

  @Test
  void shouldGetHealthcareProviderInformationByRole() {
    val doc = ErpioneActorBuilder.doctor("doc").build();
    val pharm = ErpioneActorBuilder.pharmacist("pharm").build();
    val ktr = ErpioneActorBuilder.healthInsurance("ktr").buildInstitution();

    val actorService =
        ErpioneActorService.builder()
            .healthcareProvider(List.of(doc))
            .healthcareProvider(List.of(pharm))
            .withKtrs(List.of(ktr))
            .build(false);

    val docInfo = actorService.getInstitutionByRole(InstitutionERole.HEALTH_FACILITY);
    assertFalse(docInfo.isEmpty());

    val pharmInfo = actorService.getInstitutionByRole(InstitutionERole.PHARMACY);
    assertFalse(pharmInfo.isEmpty());

    val ktrInfo = actorService.getInstitutionByRole(InstitutionERole.HEALTH_INSURANCE);
    assertFalse(ktrInfo.isEmpty());
  }

  @ParameterizedTest
  @MethodSource
  void shouldFindActorsByMnemonicId(String mnemonic) {
    val docLei = ErpioneActorBuilder.doctor(mnemonic).build();
    val actorService =
        ErpioneActorService.builder().healthcareProvider(List.of(docLei)).build(false);

    assertDoesNotThrow(() -> actorService.getInstitutionOrThrow(docLei.getFirst().getMnemonic()));
    assertDoesNotThrow(
        () -> actorService.getHealthcareProviderOrThrow(docLei.getFirst().getMnemonic()));

    val lm = mnemonic.toLowerCase();
    assertDoesNotThrow(() -> actorService.getInstitutionOrThrow(lm));
    assertDoesNotThrow(() -> actorService.getHealthcareProviderOrThrow(lm));

    val um = mnemonic.toUpperCase();
    assertDoesNotThrow(() -> actorService.getInstitutionOrThrow(um));
    assertDoesNotThrow(() -> actorService.getHealthcareProviderOrThrow(um));

    val id = MnemonicUtils.mnemonicToId(mnemonic);
    assertDoesNotThrow(() -> actorService.getInstitutionOrThrow(id));
    assertDoesNotThrow(() -> actorService.getInstitutionOrThrow(docLei.getSecond().getId()));
    assertDoesNotThrow(() -> actorService.getHealthcareProviderOrThrow(id));
    assertDoesNotThrow(() -> actorService.getHealthcareProviderOrThrow(docLei.getFirst().getId()));
  }

  static Stream<Arguments> shouldFindActorsByMnemonicId() {
    return IntStream.range(0, 10)
        .mapToObj(
            i -> {
              val mnemonic = GemFaker.getFaker().ancient().god();
              return Arguments.of(mnemonic);
            });
  }

  @Test
  void shouldThrowOnUnknownDoctorActor() {
    val actorServiceBuilder = ErpioneActorService.builder().build(false);

    val name = "doc";
    assertThrows(
        ActorNotFoundError.class, () -> actorServiceBuilder.getHealthcareProviderOrThrow(name));
    assertThrows(ActorNotFoundError.class, () -> actorServiceBuilder.getInstitutionOrThrow(name));
  }
}
