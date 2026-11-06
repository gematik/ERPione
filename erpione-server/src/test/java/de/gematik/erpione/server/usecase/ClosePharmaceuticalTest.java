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
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.util.ExamplesReader;
import de.gematik.erpione.server.util.TaskCreator;
import de.gematik.erpione.server.util.TestActorsFactory;
import de.gematik.test.erezept.client.usecases.CloseTaskCommand;
import de.gematik.test.erezept.client.usecases.TaskGetByIdCommand;
import de.gematik.test.erezept.fhir.builder.erp.ErxMedicationDispenseFaker;
import de.gematik.test.erezept.fhir.builder.erp.GemErpMedicationFaker;
import de.gematik.test.erezept.fhir.builder.erp.GemOperationInputParameterBuilder;
import de.gematik.test.erezept.fhir.profiles.definitions.ErpWorkflowStructDef;
import de.gematik.test.erezept.fhir.r4.erp.ErxPrescriptionBundle;
import de.gematik.test.erezept.fhir.r4.erp.ErxReceipt;
import de.gematik.test.erezept.fhir.r4.erp.ErxTask;
import java.util.Map;
import java.util.stream.Stream;
import lombok.val;
import org.hl7.fhir.r4.model.InstantType;
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
      ErpioneConfiguration.class,
      ErpEnvironmentConfig.class,
      TestActorsFactory.class,
      HermesServiceImpl.class,
      ErpionePersistenceImpl.class,
      ErpioneJpaConfig.class,
      TaskCreator.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    },
    properties = {"spring.profiles.active=rudev,offline,noauth"})
@EnableConfigurationProperties(DataSourceProperties.class)
class ClosePharmaceuticalTest {

  @Autowired private TestActorsFactory testActorsFactory;
  @Autowired private TaskCreator taskCreator;
  @Autowired private ErpionePersistence repository;

  @ParameterizedTest
  @MethodSource("responseExamplesWithLastMedicationDispense")
  void shouldClosePharmaceuticalWithoutBody(
      String fileName, String responseContent, boolean hasLastMedicationDispense) {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    // precondition: prepare a task in db for dispensing
    val receipt = actor.decode(ErxReceipt.class, responseContent);
    val prescriptionId = receipt.getPrescriptionId();
    repository.save(taskCreator.createNewTask(prescriptionId));

    // precondition: pretend having no lastMedicationDispense on FD
    val task = new ErxTask();
    task.getMeta().addProfile(ErpWorkflowStructDef.TASK.getCanonicalUrl());
    if (hasLastMedicationDispense) {
      task.addExtension(
          ErpWorkflowStructDef.LAST_MEDICATION_DISPENSE.asExtension(InstantType.now()));
    }
    val prescriptionBundle = new ErxPrescriptionBundle();
    prescriptionBundle.addEntry().setResource(task);
    val taskByIdResponse =
        FhirBResponse.forPayload(ErxPrescriptionBundle.class, prescriptionBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(TaskGetByIdCommand.class))).thenReturn(taskByIdResponse);

    // precondition: prepare the erp-client to mock responses
    val dispenseResponse =
        FhirBResponse.forPayload(ErxReceipt.class, receipt)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(CloseTaskCommand.class))).thenReturn(dispenseResponse);

    val uc = ClosePharmaceutical.with(prescriptionId.getValue()).using(null);

    assertDoesNotThrow(() -> uc.performAs(actor));

    val dbEntity = actor.getRepository().getPrescriptionById(prescriptionId.getValue());
    assertTrue(dbEntity.isPresent());
  }

  @ParameterizedTest
  @MethodSource("responseExamples")
  void shouldCloseWithCustomPharmaceutical(String fileName, String responseContent) {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    // precondition: prepare the request body for $dispense operation
    // Note: no example file for this in erp-fhir! use the builder instead
    val medication = GemErpMedicationFaker.forPznMedication().fake();
    val medDispense = ErxMedicationDispenseFaker.builder().withMedication(medication).fake();
    val closeOperation =
        GemOperationInputParameterBuilder.forClosingPharmaceuticals()
            .with(medDispense, medication)
            .build();

    // precondition: prepare a task in db for dispensing
    val receipt = actor.decode(ErxReceipt.class, responseContent);
    val prescriptionId = receipt.getPrescriptionId();
    repository.save(taskCreator.createNewTask(prescriptionId));

    // precondition: prepare the erp-client to mock responses
    val dispenseResponse =
        FhirBResponse.forPayload(ErxReceipt.class, receipt)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(CloseTaskCommand.class))).thenReturn(dispenseResponse);

    val uc = ClosePharmaceutical.with(prescriptionId.getValue()).using(closeOperation);

    assertDoesNotThrow(() -> uc.performAs(actor));

    val dbEntity = actor.getRepository().getPrescriptionById(prescriptionId.getValue());
    assertTrue(dbEntity.isPresent());
  }

  static Stream<Arguments> responseExamples() {
    return ExamplesReader.readExampleFiles("classpath:/fhir/valid/erp/**/receiptbundle/*.xml")
        .stream()
        .map(p -> Arguments.of(p.getFirst(), p.getSecond()));
  }

  static Stream<Arguments> responseExamplesWithLastMedicationDispense() {
    return ExamplesReader.readExampleFiles("classpath:/fhir/valid/erp/**/receiptbundle/*.xml")
        .stream()
        .flatMap(
            pair ->
                Stream.of(
                    Arguments.of(pair.getFirst(), pair.getSecond(), true),
                    Arguments.of(pair.getFirst(), pair.getSecond(), false)));
  }
}
