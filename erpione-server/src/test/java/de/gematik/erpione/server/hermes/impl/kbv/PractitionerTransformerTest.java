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

import de.gematik.erpione.data.r4.HumanENameIn;
import de.gematik.erpione.data.r4.PhysicianENumberIn;
import de.gematik.erpione.data.r4.kbv.KbvEPractitionerIn;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.test.erezept.fhir.r4.kbv.KbvPractitioner;
import de.gematik.test.erezept.fhir.values.BaseANR;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class PractitionerTransformerTest {

  @Autowired private HermesService hermes;

  @Test
  void shouldUseGivenValues() {
    val anr = new PhysicianENumberIn("LANR", "582369858");
    val data = new KbvEPractitionerIn();
    val name = new HumanENameIn("Dr.", "Emilia", "Mustermann");
    data.setName(name);
    data.setAnr(anr);
    data.addQualifications("Fachärztin für Psychiatrie und Psychotherapie");

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvPractitioner.class, data));

    assertEquals(name.asFullName(), r4.getFullName());
    assertEquals(anr.getValue(), r4.getANR().map(BaseANR::getValue).orElse(null));
    assertEquals(anr.getType(), r4.getANR().map(it -> it.getType().name()).orElse(null));
  }

  @Test
  void shouldNotThrowOnSingleFirstname() {
    val data = new KbvEPractitionerIn();
    data.setName(new HumanENameIn(null, "Emilia", null));
    assertDoesNotThrow(() -> hermes.toFhirString(KbvPractitioner.class, data));
  }
}
