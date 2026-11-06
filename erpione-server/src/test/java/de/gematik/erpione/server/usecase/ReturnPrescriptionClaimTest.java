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

import de.gematik.bbriccs.fhir.codec.EmptyResource;
import de.gematik.bbriccs.fhir.codec.utils.FhirTestResourceUtil;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.erpione.data.error.EmptyData;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.hermes.error.ErpResponseExpectationError;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.util.TaskCreator;
import de.gematik.erpione.server.util.TestActorsFactory;
import de.gematik.test.erezept.client.usecases.TaskRejectCommand;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
      HermesServiceImpl.class,
      ErpEnvironmentConfig.class,
      ErpioneConfiguration.class,
      ErpionePersistenceImpl.class,
      ErpioneJpaConfig.class,
      TaskCreator.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    },
    properties = {"spring.profiles.active=rudev,offline,noauth"})
@EnableConfigurationProperties(DataSourceProperties.class)
class ReturnPrescriptionClaimTest {

  @Autowired private TestActorsFactory testActorsFactory;
  @Autowired private TaskCreator taskCreator;

  @Test
  void shouldRejectPrescription() {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    // pre-condition
    val task = taskCreator.createNewTask();
    actor.getRepository().save(task);

    val response =
        FhirBResponse.forPayload(EmptyResource.class, new EmptyResource())
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(TaskRejectCommand.class))).thenReturn(response);

    val prescriptionId = PrescriptionId.from(task.getTaskId());
    assertTrue(actor.getRepository().getPrescriptionById(task.getTaskId()).isPresent());

    val uc = ReturnPrescriptionClaim.forPrescription(prescriptionId.getValue());

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertInstanceOf(EmptyData.class, result);
  }

  @ParameterizedTest(name = "should delete prescription if status code is {0}")
  @ValueSource(ints = {404, 409, 410})
  void shouldDeletePrescriptionIfAlreadyGone() {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    // pre-condition
    val task = taskCreator.createNewTask();
    actor.getRepository().save(task);

    val response =
        FhirBResponse.forPayload(EmptyResource.class, FhirTestResourceUtil.createOperationOutcome())
            .withStatusCode(404)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(TaskRejectCommand.class))).thenReturn(response);

    val prescriptionId = PrescriptionId.from(task.getTaskId());
    assertTrue(actor.getRepository().getPrescriptionById(task.getTaskId()).isPresent());

    val uc = ReturnPrescriptionClaim.forPrescription(prescriptionId.getValue());

    assertThrows(ErpResponseExpectationError.class, () -> uc.performAs(actor));
    // prescription was in DB but is now deleted
    assertTrue(actor.getRepository().getPrescriptionById(task.getTaskId()).isEmpty());
  }
}
