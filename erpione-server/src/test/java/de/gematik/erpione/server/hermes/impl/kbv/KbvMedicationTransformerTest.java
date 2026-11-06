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

import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.etype.MedicationECategory;
import de.gematik.erpione.data.r4.etype.MedicationEType;
import de.gematik.erpione.data.r4.etype.StandardESize;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpMedicationIngredientFaker;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpMedicationPZNFaker;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedication;
import de.gematik.test.erezept.fhir.valuesets.MedicationCategory;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class KbvMedicationTransformerTest {

  @Autowired private HermesServiceImpl hermes;

  @Test
  void shouldTransformFromKbvPznMedication() {
    val resource = KbvErpMedicationPZNFaker.builder().fake();
    val data = assertDoesNotThrow(() -> hermes.toJson(EMedication.class, resource));

    val fPzn = resource.getPznFirstRep();
    val fName = resource.getMedicationName();
    val fDarreichungsform = resource.getDarreichungsform().orElseThrow();

    assertEquals(fPzn, data.getPzn());
    assertEquals(fName, data.getName());
    assertNotNull(data.getSupplyForm());
    assertEquals(fDarreichungsform.getCode(), data.getSupplyForm().getCode());
  }

  @Test
  void shouldTransformFromKbvIngredientMedication() {
    val resource = KbvErpMedicationIngredientFaker.builder().fake();
    val data = assertDoesNotThrow(() -> hermes.toJson(EMedication.class, resource));

    assertTrue(resource.getPznOptional().isEmpty());
    assertTrue(resource.getIngredientText().isPresent());
    assertNull(data.getPzn());

    val fName = resource.getIngredientText().orElseThrow();

    assertEquals(fName, data.getName());
    assertNull(data.getSupplyForm());
  }

  @Test
  void shouldTransformToKbvPznMedication() {
    val data = new EMedication();
    data.setType(MedicationEType.PZN);

    val fhir = assertDoesNotThrow(() -> hermes.toFhirString(KbvErpMedication.class, data));
    assertTrue(hermes.validate(fhir).isSuccessful());
  }

  @Test
  void shouldTransformToKbvPznMedicationForTRezept() {
    val data = new EMedication();
    data.setType(MedicationEType.PZN);
    data.setCategory(MedicationECategory.C_02);

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpMedication.class, data));
    assertTrue(hermes.validate(r4).isSuccessful());
    assertEquals(MedicationCategory.C_02, r4.getCategoryFirstRep());
  }

  @Test
  void shouldTransformToKbvFreetextMedication() {
    val data = new EMedication();
    data.setType(MedicationEType.FREETEXT);
    data.setFreeText("Metformin 850mg Tabletten N3");

    val fhir = assertDoesNotThrow(() -> hermes.toFhirString(KbvErpMedication.class, data));
    assertTrue(hermes.validate(fhir).isSuccessful());
  }

  @Test
  void shouldTransformToKbvIngredientMedication() {
    val data = new EMedication();
    data.setType(MedicationEType.INGREDIENT);
    data.setName("Metformin");
    data.setStandardSize(StandardESize.N3);

    val fhir = assertDoesNotThrow(() -> hermes.toFhirString(KbvErpMedication.class, data));
    assertTrue(hermes.validate(fhir).isSuccessful());
  }

  @Test
  void shouldTransformToKbvCompoundingMedication() {
    val data = new EMedication();
    data.setType(MedicationEType.COMPOUNDING);
    data.setName("Metformin");
    data.setStandardSize(StandardESize.N3);

    val fhir = assertDoesNotThrow(() -> hermes.toFhirString(KbvErpMedication.class, data));
    assertTrue(hermes.validate(fhir).isSuccessful());
  }
}
