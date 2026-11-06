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

import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.fhir.codec.utils.FhirTestResourceUtil;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.error.ImpossibleOperationError;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.util.ExamplesReader;
import de.gematik.erpione.server.util.JsonEPrinter;
import de.gematik.erpione.server.util.TaskCreator;
import de.gematik.erpione.server.util.TestActorsFactory;
import de.gematik.test.erezept.client.usecases.ChargeItemGetByIdCommand;
import de.gematik.test.erezept.client.usecases.ChargeItemPostCommand;
import de.gematik.test.erezept.client.usecases.ChargeItemPutCommand;
import de.gematik.test.erezept.fhir.builder.dav.DavPkvAbgabedatenFaker;
import de.gematik.test.erezept.fhir.builder.erp.ErxChargeItemFaker;
import de.gematik.test.erezept.fhir.r4.dav.DavPkvAbgabedatenBundle;
import de.gematik.test.erezept.fhir.r4.erp.ErxChargeItem;
import de.gematik.test.erezept.fhir.r4.erp.ErxChargeItemBundle;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.Secret;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
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
class ChargeItemUsecaseTest {

  @Autowired private TestActorsFactory testActorsFactory;
  @Autowired private TaskCreator taskCreator;
  @Autowired private ErpionePersistence repository;

  @ParameterizedTest
  @MethodSource("provideChargeItemExamples")
  void shouldCreateChargeItem(String fileName, String content) {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val pid = PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_200);

    val task = taskCreator.createNewTask(pid);
    task.setStatus(PrescriptionEStatus.COMPLETED);
    task.setSecret(Secret.random().getValue());
    task.setReceiptReference(format("Bundle/{0}", UUID.randomUUID().toString()));
    repository.save(task);

    val abgabedaten = actor.getHermes().decode(DavPkvAbgabedatenBundle.class, content);
    val signed = actor.signDocument(abgabedaten);
    val invoice = abgabedaten.getInvoice();
    val uc = ChargeItemUsecase.createForPrescription(pid.getValue(), invoice);

    val ci = ErxChargeItemFaker.builder().withAbgabedatensatz(abgabedaten.getId(), signed).fake();
    val chargeItemResponse =
        FhirBResponse.forPayload(ErxChargeItem.class, ci)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(ChargeItemPostCommand.class))).thenReturn(chargeItemResponse);

    val response = assertDoesNotThrow(() -> uc.performAs(actor));
    JsonEPrinter.printAsJson(response);
  }

  @ParameterizedTest
  @MethodSource("provideTaskManipulator")
  void shouldThrowOnMissingPrecondition(Consumer<ErpETask> taskConsumer) {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val actor = pair.getSecond();

    val pid = PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_200);

    // initialize the task properly first
    val task = taskCreator.createNewTask(pid);
    task.setStatus(PrescriptionEStatus.COMPLETED);
    task.setSecret(Secret.random().getValue());
    task.setReceiptReference(format("Bundle/{0}", UUID.randomUUID().toString()));
    // and now let the consumer manipulate for negative behaviour
    taskConsumer.accept(task);
    repository.save(task);

    val abgabedaten = DavPkvAbgabedatenFaker.builder(pid).fake();
    val invoice = abgabedaten.getInvoice();
    val uc = ChargeItemUsecase.createForPrescription(pid.getValue(), invoice);
    val ex = assertThrows(ImpossibleOperationError.class, () -> uc.performAs(actor));
    System.out.println(ex.getMessage());
  }

  @Test
  void shouldGetChargeItem() {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val pid = PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_200);
    val accessCode = AccessCode.random();

    val task = taskCreator.createNewTask(pid);
    task.setStatus(PrescriptionEStatus.COMPLETED);
    task.setSecret(Secret.random().getValue());
    task.setReceiptReference(format("Bundle/{0}", UUID.randomUUID().toString()));
    repository.save(task);

    val abgabedaten = DavPkvAbgabedatenFaker.builder(pid).fake();
    val signed = actor.signDocument(abgabedaten);
    val ci =
        ErxChargeItemFaker.builder()
            .withPrescriptionId(pid)
            .withAccessCode(accessCode)
            .withAbgabedatensatz(abgabedaten.getId(), signed)
            .fake();

    val ciBundle = new ErxChargeItemBundle();
    ciBundle.addEntry().setResource(ci);
    ciBundle.addEntry().setResource(abgabedaten);

    val chargeItemResponse =
        FhirBResponse.forPayload(ErxChargeItemBundle.class, ciBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(ChargeItemGetByIdCommand.class))).thenReturn(chargeItemResponse);

    val uc = ChargeItemUsecase.getForPrescription(pid.getValue(), accessCode.getValue());
    val response = assertDoesNotThrow(() -> uc.performAs(actor));

    assertNotNull(response);
    assertEquals(pid.getValue(), response.getPrescriptionId());
    assertEquals(ci.getEntererTelematikId().getValue(), response.getEntererTelematikId());
    assertNotNull(response.getInvoice());
  }

  @Test
  void shouldPutChargeItem() {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val pid = PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_200);
    val accessCode = AccessCode.random();

    val task = taskCreator.createNewTask(pid);
    task.setStatus(PrescriptionEStatus.COMPLETED);
    task.setSecret(Secret.random().getValue());
    task.setReceiptReference(format("Bundle/{0}", UUID.randomUUID().toString()));
    repository.save(task);

    val originalAbgabe = DavPkvAbgabedatenFaker.builder(pid).fake();
    val originalSigned = actor.signDocument(originalAbgabe);
    val originalChargeItem =
        ErxChargeItemFaker.builder()
            .withPrescriptionId(pid)
            .withAccessCode(accessCode)
            .withAbgabedatensatz(originalAbgabe.getId(), originalSigned)
            .fake();

    val getBundle = new ErxChargeItemBundle();
    getBundle.addEntry().setResource(originalChargeItem);
    getBundle.addEntry().setResource(originalAbgabe);

    val getResponse =
        FhirBResponse.forPayload(ErxChargeItemBundle.class, getBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(ChargeItemGetByIdCommand.class))).thenReturn(getResponse);

    val updatedAbgabe = DavPkvAbgabedatenFaker.builder(pid).fake();
    val updatedSigned = actor.signDocument(updatedAbgabe);
    val updatedChargeItem =
        ErxChargeItemFaker.builder()
            .withPrescriptionId(pid)
            .withAccessCode(accessCode)
            .withAbgabedatensatz(updatedAbgabe.getId(), updatedSigned)
            .fake();

    val putResponse =
        FhirBResponse.forPayload(ErxChargeItem.class, updatedChargeItem)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(ChargeItemPutCommand.class))).thenReturn(putResponse);

    val uc =
        ChargeItemUsecase.updateForPrescription(
            pid.getValue(), updatedAbgabe.getInvoice(), accessCode.getValue());
    val response = assertDoesNotThrow(() -> uc.performAs(actor));

    assertNotNull(response);
    assertEquals(pid.getValue(), response.getPrescriptionId());
    assertEquals(
        updatedChargeItem.getEntererTelematikId().getValue(), response.getEntererTelematikId());
    assertNotNull(response.getInvoice());
  }

  static Stream<Arguments> provideChargeItemExamples() {
    return ExamplesReader.readExampleFiles("classpath:/fhir/valid/dav/**/*.xml").stream()
        .flatMap(
            pair ->
                Stream.of(
                    Arguments.of(pair.getFirst(), pair.getSecond(), true),
                    Arguments.of(pair.getFirst(), pair.getSecond(), false)));
  }

  static Stream<Arguments> provideTaskManipulator() {
    return Stream.of(
            (Consumer<ErpETask>) t -> t.setSecret(null),
            t -> t.setReceiptReference(null),
            t -> t.setPrescriptionReference(null))
        .map(Arguments::of);
  }
}
