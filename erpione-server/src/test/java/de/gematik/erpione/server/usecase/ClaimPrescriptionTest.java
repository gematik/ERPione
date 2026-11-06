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
import de.gematik.test.erezept.client.usecases.TaskAcceptCommand;
import de.gematik.test.erezept.fhir.r4.erp.ErxAcceptBundle;
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
      ErpioneConfiguration.class,
      ErpEnvironmentConfig.class,
      TestActorsFactory.class,
      HermesServiceImpl.class,
      ErpionePersistenceImpl.class,
      ErpioneJpaConfig.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    },
    properties = {"spring.profiles.active=rudev,offline,noauth"})
@EnableConfigurationProperties(DataSourceProperties.class)
class ClaimPrescriptionTest {

  @Autowired private TestActorsFactory testActorsFactory;

  @ParameterizedTest
  @MethodSource("acceptBundleExamples")
  void shouldAcceptPharmaceuticalPrescription(String content) {
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val erpClient = pair.getFirst();
    val actor = pair.getSecond();

    val acceptBundle = actor.decode(ErxAcceptBundle.class, content);
    val acceptResponse =
        FhirBResponse.forPayload(ErxAcceptBundle.class, acceptBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    when(erpClient.request(any(TaskAcceptCommand.class))).thenReturn(acceptResponse);

    val prescriptionId = acceptBundle.getTaskId().toPrescriptionId();
    val secret = "secret";
    assertTrue(actor.getRepository().getPrescriptionById(prescriptionId.getValue()).isEmpty());

    val uc = ClaimPrescription.with(prescriptionId.getValue(), secret);

    val result = assertDoesNotThrow(() -> uc.performAs(actor));
    assertEquals(PrescriptionEStatus.INPROGRESS, result.getStatus());

    val dbEntity = actor.getRepository().getPrescriptionById(prescriptionId.getValue());
    assertTrue(dbEntity.isPresent());
  }

  static Stream<Arguments> acceptBundleExamples() {
    return ExamplesReader.readExamples("classpath:/fhir/valid/erp/**/acceptbundle/*.xml").stream()
        .map(Arguments::of);
  }
}
