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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.data.r4.kbv.KbvEPractitionerRoleIn;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionIn;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionOut;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.test.erezept.fhir.r4.kbv.KbvEvdgaBundle;
import de.gematik.test.erezept.fhir.values.AsvTeamNumber;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class KbvEvdgaBundleTransformerTest {

  @Autowired private HermesServiceImpl hermes;

  @Test
  void shouldHavePractitionerRole() {
    val dataIn = new KbvEPrescriptionIn();
    dataIn.setPractitionerRole(new KbvEPractitionerRoleIn(AsvTeamNumber.random().getValue()));

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvEvdgaBundle.class, dataIn));
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
}
