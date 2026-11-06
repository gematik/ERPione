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

package de.gematik.erpione.server.controller.faker;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.etype.MedicationEType;
import de.gematik.erpione.data.r4.kbv.*;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.util.ExamplesReader;
import de.gematik.test.erezept.fhir.r4.kbv.*;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hl7.fhir.r4.model.ResourceType;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class KbvFakerControllerTest {

  @Autowired private HermesService hermes;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private KbvFakerController kbvFakerController;

  static Stream<Arguments> shouldFakePharmaceuticalPrescription() {
    return ExamplesReader.readExampleFiles("classpath:/examples/prescription/*.json").stream()
        .map(p -> Arguments.of(p.getSecond()));
  }

  static Stream<Arguments> shouldFakeEvdgaPrescription() {
    return ExamplesReader.readExampleFiles("classpath:/examples/prescription/evdga/*.json").stream()
        .map(p -> Arguments.of(p.getSecond()));
  }

  static Stream<Arguments> shouldFakeKbvPatient() {
    return ExamplesReader.readExampleFiles("classpath:/examples/patient/*.json").stream()
        .map(p -> Arguments.of(p.getSecond()));
  }

  static Stream<Arguments> shouldFakeKbvMedication() {
    return ExamplesReader.readExampleFiles("classpath:/examples/medication/*.json").stream()
        .map(p -> Arguments.of(p.getSecond()));
  }

  private <T> T readExample(@Nullable String input, Class<T> clazz) {
    return Optional.ofNullable(input)
        .filter(it -> !it.isEmpty())
        .map(
            it -> {
              try {
                return objectMapper.readValue(it, clazz);
              } catch (Exception e) {
                log.error("Error reading example file", e);
                return null;
              }
            })
        .orElse(null);
  }

  @ParameterizedTest
  @MethodSource
  @NullSource
  void shouldFakePharmaceuticalPrescription(String input) {
    assertDoesNotThrow(
        () -> {
          val prescription = this.readExample(input, KbvEPrescriptionIn.class);
          val fhirString = kbvFakerController.createPharmaceuticalsPrescription(prescription);
          assertNotNull(fhirString);

          val resource = hermes.decode(KbvErpBundle.class, fhirString);
          assertEquals(ResourceType.Bundle, resource.getResourceType());
        });
  }

  @ParameterizedTest
  @MethodSource
  @NullSource
  void shouldFakeEvdgaPrescription(String input) {
    assertDoesNotThrow(
        () -> {
          val prescription = this.readExample(input, KbvEPrescriptionIn.class);
          val fhirString = kbvFakerController.createEvdgaPrescription(prescription);
          assertNotNull(fhirString);

          val resource = hermes.decode(KbvEvdgaBundle.class, fhirString);
          assertEquals(ResourceType.Bundle, resource.getResourceType());
        });
  }

  @ParameterizedTest
  @MethodSource
  @NullSource
  void shouldFakeKbvPatient(String input) {
    assertDoesNotThrow(
        () -> {
          val patient = this.readExample(input, KbvEPatientIn.class);
          val fhirString = kbvFakerController.createPatient(patient);
          assertNotNull(fhirString);

          val resource = hermes.decode(KbvPatient.class, fhirString);
          assertEquals(ResourceType.Patient, resource.getResourceType());
        });
  }

  @ParameterizedTest
  @MethodSource
  @NullSource
  void shouldFakeKbvMedication(String input) {
    assertDoesNotThrow(
        () -> {
          val medication = this.readExample(input, EMedication.class);
          val fhirString = kbvFakerController.createMedication(MedicationEType.PZN, medication);
          assertNotNull(fhirString);

          val resource = hermes.decode(KbvErpMedication.class, fhirString);
          assertEquals(ResourceType.Medication, resource.getResourceType());
        });
  }

  @Test
  void shouldFakeRandomMedicationRequest() {
    assertDoesNotThrow(
        () -> {
          KbvEMedicationRequestIn mr = null;
          val fhirString = kbvFakerController.createMedicationRequest(mr);
          assertNotNull(fhirString);

          val resource = hermes.decode(KbvErpMedicationRequest.class, fhirString);
          assertEquals(ResourceType.MedicationRequest, resource.getResourceType());
        });
  }

  @Test
  void shouldFakeRandomCoverage() {
    assertDoesNotThrow(
        () -> {
          KbvECoverageIn coverage = null;
          val fhirString = kbvFakerController.createCoverage(coverage);
          assertNotNull(fhirString);

          val resource = hermes.decode(KbvCoverage.class, fhirString);
          assertEquals(ResourceType.Coverage, resource.getResourceType());
        });
  }

  @Test
  void shouldFakeRandomPractitioner() {
    assertDoesNotThrow(
        () -> {
          KbvEPractitionerIn coverage = null;
          val fhirString = kbvFakerController.createPractitioner(coverage);
          assertNotNull(fhirString);

          val resource = hermes.decode(KbvPractitioner.class, fhirString);
          assertEquals(ResourceType.Practitioner, resource.getResourceType());
        });
  }

  @Test
  void shouldFakeRandomOrganization() {
    assertDoesNotThrow(
        () -> {
          KbvEOrganizationIn coverage = null;
          val fhirString = kbvFakerController.createOrganization(coverage);
          assertNotNull(fhirString);

          val resource = hermes.decode(KbvMedicalOrganization.class, fhirString);
          assertEquals(ResourceType.Organization, resource.getResourceType());
        });
  }
}
