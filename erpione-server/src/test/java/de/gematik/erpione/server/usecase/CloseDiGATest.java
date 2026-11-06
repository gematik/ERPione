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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.AssertionsKt.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.fhir.codec.utils.FhirTestResourceUtil;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.error.ImpossibleOperationError;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.util.ExamplesReader;
import de.gematik.erpione.server.util.TaskCreator;
import de.gematik.erpione.server.util.TestActorsFactory;
import de.gematik.test.erezept.client.usecases.CloseTaskCommand;
import de.gematik.test.erezept.fhir.r4.erp.ErxReceipt;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import java.util.Map;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.Test;
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
      TaskCreator.class,
      HermesServiceImpl.class,
      ErpionePersistenceImpl.class,
      ErpioneJpaConfig.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    },
    properties = {"spring.profiles.active=rudev,offline,noauth"})
@EnableConfigurationProperties(DataSourceProperties.class)
class CloseDiGATest {

  @Autowired private TestActorsFactory testActorsFactory;
  @Autowired private TaskCreator taskCreator;
  @Autowired private ErpionePersistence repository;

  @ParameterizedTest
  @MethodSource("receiptExamples")
  void shouldApproveDiGA(String content) {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val prescriptionId = PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_162);
    repository.save(taskCreator.createNewTask(prescriptionId));

    val receipt = actor.decode(ErxReceipt.class, content);
    receipt.setIdentifier(prescriptionId.asIdentifier()); // change prescriptionId to yours

    val closeResponse =
        FhirBResponse.forPayload(ErxReceipt.class, receipt)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(CloseTaskCommand.class))).thenReturn(closeResponse);

    val uc = CloseDiGA.approve(prescriptionId.getValue());

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertEquals(prescriptionId.getValue(), result.getPrescriptionId());
  }

  @ParameterizedTest
  @MethodSource("receiptExamples")
  void shouldDeclineDiGA(String content) {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val prescriptionId = PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_162);
    repository.save(taskCreator.createNewTask(prescriptionId));

    val receipt = actor.decode(ErxReceipt.class, content);
    receipt.setIdentifier(prescriptionId.asIdentifier()); // change prescriptionId to yours

    val closeResponse =
        FhirBResponse.forPayload(ErxReceipt.class, receipt)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(CloseTaskCommand.class))).thenReturn(closeResponse);

    val uc = CloseDiGA.decline(prescriptionId.getValue());

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertEquals(prescriptionId.getValue(), result.getPrescriptionId());
  }

  @Test
  void shouldDetectInvalidFlowTypes() {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val actor = pair.getSecond();

    val prescriptionId = PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_160);
    repository.save(taskCreator.createNewTask(prescriptionId));

    val uc = CloseDiGA.approve(prescriptionId.getValue());

    assertThrows(ImpossibleOperationError.class, () -> uc.performAs(actor));
  }

  @Test
  void shouldDetectMissingHAR() {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val actor = pair.getSecond();

    val prescriptionId = PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_162);
    val erpTask = taskCreator.createNewTask(prescriptionId);
    erpTask.getPrescription().setHealthAppRequest(null);
    repository.save(erpTask);

    val uc = CloseDiGA.approve(prescriptionId.getValue());

    assertThrows(ImpossibleOperationError.class, () -> uc.performAs(actor));
  }

  static Stream<Arguments> receiptExamples() {
    return ExamplesReader.readExamples("classpath:/fhir/valid/erp/**/receiptbundle/*.json").stream()
        .map(Arguments::of);
  }
}
