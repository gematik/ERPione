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

import de.gematik.erpione.data.r4.kbv.KbvEOrganizationIn;
import de.gematik.erpione.data.r4.kbv.KbvEOrganizationOut;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.test.erezept.fhir.r4.kbv.KbvMedicalOrganization;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class MedicalEOrganizationTransformerTest {

  @Autowired private HermesServiceImpl hermes;

  @Test
  void shouldUseGivenValues() {
    val data = new KbvEOrganizationIn();
    data.setOfficeName("Arztpraxis Bloch-Bauer");

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvMedicalOrganization.class, data));
    assertNotNull(r4);
    val vr = hermes.validate(r4);
    assertTrue(
        vr.isSuccessful(),
        "Validation should be successful, but had some issues: " + vr.getMessages().size());

    val data2 = assertDoesNotThrow(() -> hermes.toJson(KbvEOrganizationOut.class, r4));
    assertEquals(data.getOfficeName(), data2.getOfficeName());
  }
}
