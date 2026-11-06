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

package de.gematik.erpione.server.hermes.impl.kbv;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.bbriccs.fhir.de.DeBasisProfilNamingSystem;
import de.gematik.erpione.data.r4.PhysicianENumberIn;
import de.gematik.erpione.data.r4.kbv.*;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.values.AsvTeamNumber;
import lombok.val;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class KbvErpBundleTransformerTest {

  @Autowired private HermesServiceImpl hermes;

  @Test
  void shouldHavePractitionerRole() {
    val dataIn = new KbvEPrescriptionIn();
    dataIn.setPractitionerRole(new KbvEPractitionerRoleIn(AsvTeamNumber.random().getValue()));

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpBundle.class, dataIn));
    assertNotNull(r4);
    assertNotNull(r4.getPractitioner());
    assertNotNull(r4.getPractitioner().getAsvFachgruppennummer());

    val vr = hermes.validate(r4);
    assertTrue(
        vr.isSuccessful(),
        "Validation should be successful, but had some issues: " + vr.getMessages().size());

    val dataOut = assertDoesNotThrow(() -> hermes.toJson(KbvEPrescriptionOut.class, r4));
    assertNotNull(dataIn.getPractitionerRole());
    assertNotNull(dataIn.getPractitionerRole().getAsvnr());

    assertNotNull(dataOut.getPractitionerRole());
    assertNotNull(dataOut.getPractitionerRole().getAsvnr());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ZANR", "LANR"})
  void shouldFakeAnrWhenMissing(String anrType) {
    // prepare input data with a ZANR and a BSNR
    val dataIn = new KbvEPrescriptionIn();

    val practitioner = new KbvEPractitionerIn();
    val anr = new PhysicianENumberIn(anrType, "");
    practitioner.setAnr(anr);

    dataIn.setPractitioner(practitioner);

    // transform to FHIR resource
    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpBundle.class, dataIn));

    val r4Practitioner = r4.getPractitioner();

    assertTrue(r4Practitioner.getANR().isPresent());
    val r4Anr = r4Practitioner.getANR().get();
    assertEquals(anrType, r4Anr.getType().name());
    assertNotNull(r4Anr.getValue());
    assertFalse(r4Anr.getValue().isBlank());
  }

  @Test
  void shouldMapKzvAbrechnungsnummerCorrectly() {
    // prepare input data with a ZANR and a BSNR
    val dataIn = new KbvEPrescriptionIn();

    val org = new KbvEOrganizationIn();
    val bsnr = "105508890";
    // note: KZV-Abrechnungsnummer is also a BSNR for us
    org.setBsnr(bsnr);

    val practitioner = new KbvEPractitionerIn();
    val anr = new PhysicianENumberIn("ZANR", "157307391");
    practitioner.setAnr(anr);

    dataIn.setOrganization(org);
    dataIn.setPractitioner(practitioner);

    // transform to FHIR resource
    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpBundle.class, dataIn));
    val orgR4 = r4.getMedicalOrganization();
    assertFalse(orgR4.getBsnr().isPresent());
    assertTrue(
        orgR4.getIdentifier().stream()
            .anyMatch(DeBasisProfilNamingSystem.KZBV_KZVA_ABRECHNUNGSNUMMER_SID::matches));
    val kzva =
        orgR4.getIdentifier().stream()
            .filter(DeBasisProfilNamingSystem.KZBV_KZVA_ABRECHNUNGSNUMMER_SID::matches)
            .findFirst()
            .map(Identifier::getValue)
            .orElse(null);
    assertEquals(bsnr, kzva);

    val vr = hermes.validate(r4);
    assertTrue(
        vr.isSuccessful(),
        "Validation should be successful, but had some issues: " + vr.getMessages().size());

    // transform back to Erpione data
    val dataOut = assertDoesNotThrow(() -> hermes.toJson(KbvEPrescriptionOut.class, r4));
    assertEquals(bsnr, dataOut.getOrganization().getBsnr());
  }
}
