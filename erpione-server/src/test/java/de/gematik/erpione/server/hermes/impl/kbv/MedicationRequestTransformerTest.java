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

import de.gematik.erpione.data.r4.etype.AccidentEType;
import de.gematik.erpione.data.r4.kbv.KbvAccidentEData;
import de.gematik.erpione.data.r4.kbv.KbvEMedicationRequestIn;
import de.gematik.erpione.data.r4.kbv.KbvEMedicationRequestOut;
import de.gematik.erpione.data.r4.kbv.KbvMvoEData;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedicationRequest;
import de.gematik.test.erezept.fhir.valuesets.AccidentCauseType;
import java.util.Date;
import java.util.Objects;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class MedicationRequestTransformerTest {

  @Autowired private HermesServiceImpl hermes;

  @Test
  void shouldGenerateMvoWhenGiven() {
    val data = new KbvEMedicationRequestIn();
    data.setMvo(new KbvMvoEData());

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpMedicationRequest.class, data));
    assertNotNull(r4);
    assertTrue(r4.isMultiple());

    val vr = hermes.validate(r4);
    assertTrue(
        vr.isSuccessful(),
        "Validation should be successful, but had some issues: " + vr.getMessages().size());

    val data2 = assertDoesNotThrow(() -> hermes.toJson(KbvEMedicationRequestOut.class, r4));
    assertNotNull(data.getMvo());
    assertNotNull(data2.getMvo());
    assertEquals(data.getMvo().getStartDate(), data2.getMvo().getStartDate());
  }

  @Test
  void shouldGenerateAccidentExtensionWhenGiven() {
    val dataIn = new KbvEMedicationRequestIn();
    dataIn.setAccident(new KbvAccidentEData());

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpMedicationRequest.class, dataIn));
    assertNotNull(r4);
    assertTrue(r4.hasAccidentExtension());

    val vr = hermes.validate(r4);
    assertTrue(
        vr.isSuccessful(),
        "Validation should be successful, but had some issues: " + vr.getMessages().size());

    val dataOut = assertDoesNotThrow(() -> hermes.toJson(KbvEMedicationRequestOut.class, r4));

    val accidentIn = dataIn.getAccident();
    val accidentOut = dataOut.getAccident();
    assertNotNull(accidentIn);
    assertNotNull(accidentOut);
    assertEquals(accidentIn.getType(), accidentOut.getType());
    if (accidentIn.getDate() != null) {
      assertEquals(accidentIn.getDate(), accidentOut.getDate());
    }
    assertEquals(accidentIn.getOrganization(), accidentOut.getOrganization());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "2", "4"})
  void shouldHaveCorrectAccidentCause(String accidentType) {
    val dataIn = new KbvEMedicationRequestIn();
    val accident = new KbvAccidentEData();
    accident.setType(AccidentEType.fromString(accidentType));
    accident.setDate(new Date());
    dataIn.setAccident(accident);

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpMedicationRequest.class, dataIn));
    assertNotNull(r4);
    assertTrue(r4.hasAccidentExtension());

    val expectedCause =
        switch (accidentType) {
          case "1" -> AccidentCauseType.ACCIDENT;
          case "2" -> AccidentCauseType.ACCIDENT_AT_WORK;
          case "4" -> AccidentCauseType.OCCUPATIONAL_DISEASE;
          default -> throw new IllegalStateException("Unexpected value: " + accidentType);
        };
    assertTrue(r4.getAccidentCause().map(cause -> cause.equals(expectedCause)).orElse(false));

    val vr = hermes.validate(r4);
    assertTrue(
        vr.isSuccessful(),
        "Validation should be successful, but had some issues: " + vr.getMessages().size());

    val dataOut = assertDoesNotThrow(() -> hermes.toJson(KbvEMedicationRequestOut.class, r4));

    assertEquals(
        Objects.requireNonNull(dataIn.getAccident()).getType(),
        AccidentEType.fromString(accidentType));
    assertEquals(
        Objects.requireNonNull(dataOut.getAccident()).getType(),
        AccidentEType.fromString(accidentType));
  }

  @Test
  void shouldNotHaveAccidentExtensionByDefault() {
    val dataIn = new KbvEMedicationRequestIn();

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpMedicationRequest.class, dataIn));
    assertNotNull(r4);
    assertFalse(r4.hasAccidentExtension());

    val vr = hermes.validate(r4);
    assertTrue(
        vr.isSuccessful(),
        "Validation should be successful, but had some issues: " + vr.getMessages().size());

    val dataOut = assertDoesNotThrow(() -> hermes.toJson(KbvEMedicationRequestOut.class, r4));

    assertNull(dataIn.getAccident());
    assertNull(dataOut.getAccident());
  }

  @Test
  void shouldGenerateWithEmpIdentifier() {
    val data = new KbvEMedicationRequestIn();
    data.setEmpIdentifier("123456789");

    val r4 = assertDoesNotThrow(() -> hermes.toFhirResource(KbvErpMedicationRequest.class, data));
    assertNotNull(r4);
    assertTrue(r4.hasBasedOn());

    val vr = hermes.validate(r4);
    assertTrue(
        vr.isSuccessful(),
        "Validation should be successful, but had some issues: " + vr.getMessages().size());

    val data2 = assertDoesNotThrow(() -> hermes.toJson(KbvEMedicationRequestOut.class, r4));
    assertEquals(data.getEmpIdentifier(), data2.getEmpIdentifier());
  }

  @Test
  void shouldTreatEmptyStringsLikeNull() {
    val data = new KbvEMedicationRequestIn();
    data.setDosage("");

    val encoded = hermes.toFhirString(KbvErpMedicationRequest.class, data);

    val resource = assertDoesNotThrow(() -> hermes.decode(KbvErpMedicationRequest.class, encoded));
    assertNotNull(resource.getDosageInstructionAsText());
    assertFalse(resource.getDosageInstructionAsText().isBlank());
  }
}
