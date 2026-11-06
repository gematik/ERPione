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

package de.gematik.erpione.server.hermes.impl.gem;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.bbriccs.utils.ResourceLoader;
import de.gematik.erpione.data.ErpioneInputData;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.test.erezept.fhir.r4.erp.GemErpMedication;
import java.util.stream.Stream;
import lombok.val;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    classes = {
      ErpioneConfiguration.class,
      ErpEnvironmentConfig.class,
      ErpionePersistenceImpl.class,
      ErpioneJpaConfig.class,
      HermesServiceImpl.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    },
    properties = {"spring.profiles.active=rudev,offline,noauth"})
@EnableConfigurationProperties(DataSourceProperties.class)
class GemErpMedicationInputTransformerTest {

  @Autowired private HermesServiceImpl hermes;
  @Autowired private ObjectMapper objectMapper;

  static Stream<Arguments> shouldTransformJsonExampleToFhir() {
    return ResourceLoader.readFilesFromDirectory("examples/medication").stream()
        .flatMap(
            input -> Stream.of(Arguments.of(EMedication.class, GemErpMedication.class, input)));
  }

  @ParameterizedTest
  @MethodSource
  void shouldTransformJsonExampleToFhir(
      Class<? extends ErpioneInputData> dClass, Class<Resource> rClass, String input) {

    val data = objectMapper.readValue(input, dClass);

    val encoded = hermes.toFhirString(rClass, data);
    val vr = hermes.validate(encoded);
    assertTrue(vr.isSuccessful());

    val resource = assertDoesNotThrow(() -> hermes.decode(rClass, encoded));
    assertNotNull(resource);
  }
}
