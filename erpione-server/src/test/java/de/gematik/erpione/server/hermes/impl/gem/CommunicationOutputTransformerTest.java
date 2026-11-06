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

import static de.gematik.erpione.server.util.JsonEPrinter.printAsJson;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.gematik.bbriccs.fhir.de.value.IKNR;
import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.bbriccs.fhir.de.value.TelematikID;
import de.gematik.bbriccs.utils.ResourceLoader;
import de.gematik.erpione.data.r4.gem.com.CommunicationEDataOut;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.test.erezept.fhir.builder.erp.ErxCommunicationBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpMedicationPZNFaker;
import de.gematik.test.erezept.fhir.r4.erp.ErxCommunication;
import de.gematik.test.erezept.fhir.testutil.ErpFhirBuildingTest;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class CommunicationOutputTransformerTest extends ErpFhirBuildingTest {

  @Autowired private HermesService hermes;

  static Stream<Arguments> shouldTransformFromFhirToJson() {
    return Stream.of(
            "fhir/valid/erp/1.6.0/xml/Communication-2be1c6ac-5d10-47f6-84ee-8318b2c22c76.json.xml",
            "fhir/valid/erp/1.6.0/xml/Communication-140f716f-f649-44fe-9a4e-157eb3c8adf3.json.xml",
            "fhir/valid/erp/1.6.0/xml/Communication-7977a4ab-97a9-4d95-afb3-6c4c1e2ac596.json.xml",
            "fhir/valid/erp/1.6.0/xml/Communication-a218a36e-f2fd-4603-ba67-c827acfef01b.json.xml",
            "fhir/valid/erp/1.5.0/communication/Communication-2be1c6ac-5d10-47f6-84ee-8318b2c22c76.json.xml",
            "fhir/valid/erp/1.5.0/communication/Communication-140f716f-f649-44fe-9a4e-157eb3c8adf3.json.xml",
            "fhir/valid/erp/1.5.0/communication/Communication-7977a4ab-97a9-4d95-afb3-6c4c1e2ac596.json.xml",
            "fhir/valid/erp/1.5.0/communication/Communication-a218a36e-f2fd-4603-ba67-c827acfef01b.json.xml",
            "fhir/valid/erp/1.4.0/communication/2be1c6ac-5d10-47f6-84ee-8318b2c22c76.json",
            "fhir/valid/erp/1.4.0/communication/8ca3c379-ac86-470f-bc12-178c9008f5c9.json",
            "fhir/valid/erp/1.4.0/communication/7977a4ab-97a9-4d95-afb3-6c4c1e2ac596.json",
            "fhir/valid/erp/1.4.0/communication/a218a36e-f2fd-4603-ba67-c827acfef01b.json")
        .map(ResourceLoader::readFileFromResource)
        .map(Arguments::arguments);
  }

  static Stream<Arguments> shouldTransformFromBuilderToJson() {
    val ac = AccessCode.random();
    val kvnr = KVNR.random().getValueAsString();
    val tid = TelematikID.random().getValueAsString();
    val iknr = IKNR.random();

    return Stream.of(
            ErxCommunicationBuilder.forDiGADispenseRequest()
                .basedOn(
                    PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_162).getValueAsString(),
                    ac.getValueAsString())
                .sender(kvnr)
                .receiver(tid)
                .flowType(PrescriptionFlowType.FLOW_TYPE_162)
                .build(),
            ErxCommunicationBuilder.forChargeItemChangeRequest("ChargeItem bitte ändern")
                .basedOn(
                    PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_200).getValueAsString())
                .sender(kvnr)
                .receiver(tid)
                .build(),
            ErxCommunicationBuilder.forChargeItemChangeReply("ChargeItem geändert")
                .basedOn(
                    PrescriptionId.random(PrescriptionFlowType.FLOW_TYPE_200).getValueAsString())
                .sender(kvnr)
                .receiver(tid)
                .build(),
            ErxCommunicationBuilder.forInfoRequest("Medikament vorrätig?")
                .basedOn(PrescriptionId.random().getValueAsString())
                .sender(kvnr)
                .receiver(tid)
                .insurance(iknr)
                .medication(KbvErpMedicationPZNFaker.builder().fake())
                .flowType(PrescriptionFlowType.FLOW_TYPE_160)
                .build())
        .map(Arguments::arguments);
  }

  @ParameterizedTest
  @MethodSource
  @SneakyThrows
  void shouldTransformFromFhirToJson(String content) {
    val resource = hermes.decode(ErxCommunication.class, content);

    val data = assertDoesNotThrow(() -> hermes.toJson(CommunicationEDataOut.class, resource));
    assertNotNull(data);
    printAsJson(data);
  }

  @ParameterizedTest
  @MethodSource
  void shouldTransformFromBuilderToJson(ErxCommunication resource) {
    val data = assertDoesNotThrow(() -> hermes.toJson(CommunicationEDataOut.class, resource));
    assertNotNull(data);
    printAsJson(data);
  }
}
