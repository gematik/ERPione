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

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.bbriccs.utils.ResourceLoader;
import de.gematik.erpione.data.ErpioneInputData;
import de.gematik.erpione.data.r4.gem.com.CommunicationEDataIn;
import de.gematik.erpione.data.r4.gem.com.CommunicationEType;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.error.ImpossibleOperationError;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistenceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.test.erezept.fhir.r4.erp.ErxCommunication;
import de.gematik.test.erezept.fhir.values.json.CommunicationPayloadValidation;
import de.gematik.test.erezept.fhir.values.json.CommunicationReplyMessage;
import java.io.File;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
class CommunicationReplyInputTransformerTest {

  @Autowired private HermesServiceImpl hermes;
  @Autowired private ObjectMapper objectMapper;

  private final CommunicationPayloadValidation payloadValidatorV3 =
      new CommunicationPayloadValidation("erpcom/CommunicationReplyPayloadV3.json");

  private final CommunicationPayloadValidation payloadValidatorV1 =
      new CommunicationPayloadValidation("erpcom/CommunicationReplyPayloadV1.json");

  @ParameterizedTest
  @MethodSource
  void shouldTransformJsonV3ExampleToFhir(
      File file,
      Class<? extends ErpioneInputData> dClass,
      Class<ErxCommunication> rClass,
      String input) {

    val data = objectMapper.readValue(input, dClass);

    val encoded = hermes.toFhirString(rClass, data);

    val vr = hermes.validate(encoded);
    assertTrue(vr.isSuccessful());

    val resource = assertDoesNotThrow(() -> hermes.decode(rClass, encoded));
    assertNotNull(resource);

    val payload = objectMapper.readValue(resource.getMessage(), CommunicationReplyMessage.class);
    val pvr = payloadValidatorV3.validate(payload);
    assertTrue(pvr.isEmpty(), "Payload validation failed: " + pvr);
  }

  @ParameterizedTest
  @MethodSource
  void shouldTransformJsonV1ExampleToFhir(
      File file,
      Class<? extends ErpioneInputData> dClass,
      Class<ErxCommunication> rClass,
      String input) {

    val data = objectMapper.readValue(input, dClass);

    val encoded = hermes.toFhirString(rClass, data);

    val vr = hermes.validate(encoded);
    assertTrue(vr.isSuccessful());

    val resource = assertDoesNotThrow(() -> hermes.decode(rClass, encoded));
    assertNotNull(resource);

    val payload = objectMapper.readValue(resource.getMessage(), CommunicationReplyMessage.class);
    val pvr = payloadValidatorV1.validate(payload);
    assertTrue(pvr.isEmpty(), "Payload validation failed: " + pvr);
  }

  @ParameterizedTest
  @ValueSource(strings = {"lon", "long"})
  void shouldFixTheLonLongIssue(String longitudeField) {

    // Note: inTransportPosition has lon instead of long
    val input =
        format(
            """
{
  "recipient": "L982864321",
  "reference": {
    "prescriptionId": "160.051.239.685.270.45",
    "accessCode": "dbedd1bbc1983e6fb139dfbdd2134b21abbb15e01a2121a489a2156b9b08fb17"
  },
  "message": {
    "version": 3,
    "text": "Some Text to state your request",
    "transactionID": "ABCD-EFGH-IJKL-MNOP",
    "communicationType": "deliveryStatus",
    "deliveryStatus": "inTransport",
    "inTransportPosition": {
      "%s": 13.387595793605172,
      "lat": 52.522529939635795
    },
    "inTransportETA": {
      "from": 1735736400,
      "to": 1735741800
    }
  }
}
""",
            longitudeField);

    val data = objectMapper.readValue(input, CommunicationEDataIn.class);

    val encoded = hermes.toFhirString(ErxCommunication.class, data);

    val vr = hermes.validate(encoded);
    assertTrue(vr.isSuccessful());

    val resource = assertDoesNotThrow(() -> hermes.decode(ErxCommunication.class, encoded));
    assertNotNull(resource);

    val payload = objectMapper.readValue(resource.getMessage(), CommunicationReplyMessage.class);

    // Note: validator does NOT detect the lon/long issue
    val pvr = payloadValidatorV3.validate(payload);
    assertTrue(pvr.isEmpty(), "Payload validation failed: " + pvr);

    // check by hand, that this value is always long and not lon
    val rawMessage = resource.getMessage();
    val transportPosition = objectMapper.readTree(rawMessage).path("inTransportPosition");
    assertFalse(transportPosition.has("lon"), "lon must be renamed to long in:\n" + rawMessage);
    assertTrue(
        transportPosition.has("long"), "long missing in:\n" + rawMessage); // with append g -> long
    assertEquals("13.387595793605172", transportPosition.get("long").asString());
  }

  @ParameterizedTest
  @EnumSource(
      value = CommunicationEType.class,
      names = {"REPLY"},
      mode = EnumSource.Mode.EXCLUDE)
  void shouldThrowOnUnsupportedType(CommunicationEType type) {
    val input = new CommunicationEDataIn(type, null, null, null, null, null);
    assertThrows(
        ImpossibleOperationError.class, () -> hermes.toFhirResource(ErxCommunication.class, input));
  }

  @Test
  void shouldDefaultTypeToReply() {
    val input = "{}";
    val data = objectMapper.readValue(input, CommunicationEDataIn.class);
    assertEquals(CommunicationEType.REPLY, data.getType());
  }

  static Stream<Arguments> shouldTransformJsonExampleToFhir(String directory) {
    return ResourceLoader.getResourceFilesInDirectory(directory).stream()
        .flatMap(
            file ->
                Stream.of(
                    Arguments.of(
                        file,
                        CommunicationEDataIn.class,
                        ErxCommunication.class,
                        ResourceLoader.readString(file))));
  }

  static Stream<Arguments> shouldTransformJsonV1ExampleToFhir() {
    return shouldTransformJsonExampleToFhir("examples/communication/reply/v1");
  }

  static Stream<Arguments> shouldTransformJsonV3ExampleToFhir() {
    return shouldTransformJsonExampleToFhir("examples/communication/reply/v3");
  }
}
