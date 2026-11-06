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
import de.gematik.erpione.data.error.InvalidCommunicationPayloadError;
import de.gematik.erpione.data.r4.gem.com.CommunicationEDataOut;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.util.TaskCreator;
import de.gematik.erpione.server.util.TestActorsFactory;
import de.gematik.test.erezept.client.usecases.CommunicationPostCommand;
import de.gematik.test.erezept.fhir.builder.erp.ErxComReplyBuilder;
import de.gematik.test.erezept.fhir.r4.erp.ErxCommunication;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
class SendCommunicationReplyTest {

  @Autowired private TestActorsFactory testActorsFactory;
  @Autowired private TaskCreator taskCreator;

  @Test
  void shouldSendReplyCommunication() {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    // pre-condition
    val task = taskCreator.createNewTask();
    actor.getRepository().save(task);

    val requestCom =
        ErxComReplyBuilder.asReply("hello world")
            .basedOn(task.getTaskId(), task.getAccessCode())
            .receiver(task.getPrescription().getPatient().getKvnr())
            .sender(actor.getTelematikId())
            .build();

    val responseCom = ErxCommunication.fromCommunication(requestCom);

    val response =
        FhirBResponse.forPayload(ErxCommunication.class, responseCom)
            .withStatusCode(201)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(CommunicationPostCommand.class))).thenReturn(response);

    val uc = SendCommunicationReply.withCommunicationTemplate(requestCom);

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertInstanceOf(CommunicationEDataOut.class, result);
  }

  @Test
  void shouldThrowOnInvalidCommunicationPayload() {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    // pre-condition
    val task = taskCreator.createNewTask();
    actor.getRepository().save(task);

    val requestCom =
        ErxComReplyBuilder.asReply("{'hello': 'world'")
            .basedOn(task.getTaskId(), task.getAccessCode())
            .receiver(task.getPrescription().getPatient().getKvnr())
            .sender(actor.getTelematikId())
            .build();

    val responseCom = ErxCommunication.fromCommunication(requestCom);

    val response =
        FhirBResponse.forPayload(ErxCommunication.class, responseCom)
            .withStatusCode(201)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(CommunicationPostCommand.class))).thenReturn(response);

    val uc = SendCommunicationReply.withCommunicationTemplate(requestCom);

    assertThrows(InvalidCommunicationPayloadError.class, () -> uc.performAs(actor));
  }

  @Test
  void shouldSendOrderIdAsIdentifier() {
    val posted = sendReply("ORDER-123");
    val orderId = posted.getOrderId();
    assertTrue(orderId.isPresent());
    assertEquals("ORDER-123", orderId.get());
  }

  @Test
  void shouldSendNoIdentifierWithoutOrderId() {
    val posted = sendReply(null);
    assertTrue(posted.getOrderId().isEmpty());
    assertTrue(posted.getIdentifier().isEmpty());
  }

  private ErxCommunication sendReply(String orderId) {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val task = taskCreator.createNewTask();
    actor.getRepository().save(task);

    val requestCom =
        ErxComReplyBuilder.asReply("hello world")
            .basedOn(task.getTaskId(), task.getAccessCode())
            .receiver(task.getPrescription().getPatient().getKvnr())
            .sender(actor.getTelematikId())
            .orderId(orderId)
            .build();

    val response =
        FhirBResponse.forPayload(
                ErxCommunication.class, ErxCommunication.fromCommunication(requestCom))
            .withStatusCode(201)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    val command = ArgumentCaptor.forClass(CommunicationPostCommand.class);
    when(erpClient.request(command.capture())).thenReturn(response);

    assertDoesNotThrow(
        () -> SendCommunicationReply.withCommunicationTemplate(requestCom).performAs(actor));
    return command.getValue().getRequestBody();
  }
}
