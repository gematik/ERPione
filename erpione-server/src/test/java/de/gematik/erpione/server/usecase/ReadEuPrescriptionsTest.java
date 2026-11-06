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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.fhir.codec.utils.FhirTestResourceUtil;
import de.gematik.bbriccs.fhir.coding.SemanticValue;
import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.util.ExamplesReader;
import de.gematik.erpione.server.util.TaskCreator;
import de.gematik.erpione.server.util.TestActorsFactory;
import de.gematik.test.erezept.client.usecases.eu.EuGetPrescriptionPostCommand;
import de.gematik.test.erezept.fhir.r4.eu.EuPrescriptionBundle;
import de.gematik.test.erezept.fhir.values.EuAccessCode;
import java.util.Map;
import java.util.function.Supplier;
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
      TestActorsFactory.class,
      TaskCreator.class,
      HermesServiceImpl.class,
      ErpEnvironmentConfig.class,
      ErpioneConfiguration.class,
      ErpionePersistenceImpl.class,
      ErpioneJpaConfig.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    },
    properties = {"spring.profiles.active=rudev,offline,noauth"})
@EnableConfigurationProperties(DataSourceProperties.class)
class ReadEuPrescriptionsTest {

  @Autowired private TestActorsFactory testActorsFactory;
  @Autowired private TaskCreator taskCreator;

  static Stream<Arguments> shouldReadDemographicsOrPrescriptionList() {
    return Stream.of(
        Arguments.of(
            (Supplier<ReadEuPrescriptions>)
                () ->
                    ReadEuPrescriptions.demographicsFor(
                        KVNR.randomStringValue(), EuAccessCode.random().getValue())),
        Arguments.of(
            (Supplier<ReadEuPrescriptions>)
                () ->
                    ReadEuPrescriptions.prescriptionsListFor(
                        KVNR.randomStringValue(), EuAccessCode.random().getValue())));
  }

  @ParameterizedTest
  @MethodSource
  void shouldReadDemographicsOrPrescriptionList(Supplier<ReadEuPrescriptions> usecaseSupplier) {
    val pair = testActorsFactory.getTestEuPharmacy("Vaduz");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val content =
        ExamplesReader.readExamples("classpath:/fhir/valid/eu/demographics-data-01.xml").stream()
            .findFirst()
            .orElseThrow();
    val bundle = actor.decode(EuPrescriptionBundle.class, content);
    val response =
        FhirBResponse.forPayload(EuPrescriptionBundle.class, bundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());

    when(erpClient.request(any(EuGetPrescriptionPostCommand.class))).thenReturn(response);

    val uc = usecaseSupplier.get();

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertEquals(1, result.getTotal());
  }

  @Transactional
  @Test
  void shouldUpdatePrescriptionStatusOnSuccessfulClaim() {
    val pair = testActorsFactory.getTestEuPharmacy("Vaduz");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val repository = actor.getRepository();

    val content =
        ExamplesReader.readExamples("classpath:/fhir/valid/eu/demographics-data-01.xml").stream()
            .findFirst()
            .orElseThrow();
    val bundle = actor.decode(EuPrescriptionBundle.class, content);
    val pids = bundle.getPrescriptionIds().stream().map(SemanticValue::getValue).toList();
    pids.forEach(pid -> repository.save(taskCreator.createNewTask(pid)));

    val response =
        FhirBResponse.forPayload(EuPrescriptionBundle.class, bundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());

    when(erpClient.request(any(EuGetPrescriptionPostCommand.class))).thenReturn(response);

    val kvnr = KVNR.randomStringValue();
    val ac = EuAccessCode.random().getValue();
    val uc = ReadEuPrescriptions.claimPrescriptions(kvnr, ac, pids);

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertEquals(1, result.getTotal());

    pids.forEach(
        pid -> {
          val updatedTask = repository.getPrescriptionById(pid).orElseThrow();
          assertEquals(PrescriptionEStatus.INPROGRESS, updatedTask.getStatus());
        });
  }
}
