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

package de.gematik.erpione.server.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.fhir.codec.utils.FhirTestResourceUtil;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.util.ExamplesReader;
import de.gematik.erpione.server.util.TestActorsFactory;
import de.gematik.test.erezept.client.usecases.TaskActivateCommand;
import de.gematik.test.erezept.client.usecases.TaskCreateCommand;
import de.gematik.test.erezept.fhir.builder.kbv.*;
import de.gematik.test.erezept.fhir.r4.erp.ErxTask;
import java.util.Map;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest(
    classes = {
      TestActorsFactory.class,
      ErpionePersistenceImpl.class,
      ErpioneConfiguration.class,
      ErpEnvironmentConfig.class,
      ErpioneJpaConfig.class,
      HermesServiceImpl.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    },
    properties = {"spring.profiles.active=rudev,offline,noauth"})
@EnableConfigurationProperties(DataSourceProperties.class)
class PrescribeUsecaseTest {

  @Autowired private TestActorsFactory testActorsFactory;

  static Stream<Arguments> readyTaskExamples() {
    val draftTask =
        ExamplesReader.readExamples(
                "classpath:/fhir/valid/erp/1.5.0/task/Task-b12eb5f7-91ce-4887-93c7-800454601377.json.xml")
            .stream()
            .findFirst()
            .orElseThrow();

    val readyTask =
        ExamplesReader.readExamples(
                "classpath:/fhir/valid/erp/1.5.0/task/Task-607255ed-ce41-47fc-aad3-cfce1c39963f.json.xml")
            .stream()
            .findFirst()
            .orElseThrow();

    return Stream.of(Arguments.of(draftTask, readyTask));
  }

  @ParameterizedTest
  @MethodSource("readyTaskExamples")
  void shouldPrescribePharmaceutical(String draftTaskContent, String readyTaskContent) {
    val pair = testActorsFactory.getTestDoctor("Machaon");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val draftTask = actor.decode(ErxTask.class, draftTaskContent);
    val createResponse =
        FhirBResponse.forPayload(ErxTask.class, draftTask)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());

    val readyTask = actor.decode(ErxTask.class, readyTaskContent);
    val activateResponse =
        FhirBResponse.forPayload(ErxTask.class, readyTask)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());

    when(erpClient.request(any(TaskCreateCommand.class))).thenReturn(createResponse);
    when(erpClient.request(any(TaskActivateCommand.class))).thenReturn(activateResponse);

    val prescriptionId = readyTask.getPrescriptionId();
    val bundle = KbvErpBundleFaker.builder().fake();
    val uc = PrescribeUsecase.forPharmaceutical(bundle, false);

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertEquals(PrescriptionEStatus.READY, result.getStatus());

    val dbEntity = actor.getRepository().getPrescriptionById(prescriptionId.getValue());
    assertTrue(dbEntity.isPresent());
  }

  @ParameterizedTest
  @MethodSource("readyTaskExamples")
  void shouldPrescribeEvdga(String draftTaskContent, String readyTaskContent) {
    val pair = testActorsFactory.getTestDoctor("Machaon");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val draftTask = actor.decode(ErxTask.class, draftTaskContent);
    val createResponse =
        FhirBResponse.forPayload(ErxTask.class, draftTask)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());

    val readyTask = actor.decode(ErxTask.class, readyTaskContent);
    val activateResponse =
        FhirBResponse.forPayload(ErxTask.class, readyTask)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());

    when(erpClient.request(any(TaskCreateCommand.class))).thenReturn(createResponse);
    when(erpClient.request(any(TaskActivateCommand.class))).thenReturn(activateResponse);

    val prescriptionId = readyTask.getPrescriptionId();

    val patient = KbvPatientFaker.builder().fake();
    val bundle =
        KbvEvdgaBundleBuilder.forPrescription(prescriptionId)
            .healthAppRequest(KbvHealthAppRequestFaker.forPatient(patient).fake())
            .patient(patient)
            .medicalOrganization(KbvMedicalOrganizationFaker.builder().fake())
            .insurance(KbvCoverageFaker.builder().fake())
            .practitioner(KbvPractitionerFaker.builder().fake())
            .build();
    val uc = PrescribeUsecase.forDiGA(bundle);

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertEquals(PrescriptionEStatus.READY, result.getStatus());

    val dbEntity = actor.getRepository().getPrescriptionById(prescriptionId.getValue());
    assertTrue(dbEntity.isPresent());
  }
}
