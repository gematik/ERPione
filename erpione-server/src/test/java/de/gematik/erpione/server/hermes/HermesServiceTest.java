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

package de.gematik.erpione.server.hermes;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.data.ErpioneOutputData;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.kbv.*;
import de.gematik.erpione.server.hermes.error.HermesTransformerNotFoundError;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.util.ExamplesReader;
import de.gematik.test.erezept.fhir.builder.kbv.*;
import de.gematik.test.erezept.fhir.r4.kbv.KbvBaseBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.testutil.ValidatorUtil;
import de.gematik.test.erezept.fhir.values.LANR;
import de.gematik.test.erezept.fhir.valuesets.QualificationType;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.val;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class HermesServiceTest {

  @Autowired private HermesServiceImpl hermes;
  @Autowired private ObjectMapper objectMapper;

  static Stream<Arguments> shouldTransformToResponseData() {
    return Stream.of(
        Arguments.of(
            KbvECoverageOut.class, (Supplier<Resource>) () -> KbvCoverageFaker.builder().fake()),
        Arguments.of(
            KbvEPrescriptionOut.class,
            (Supplier<Resource>)
                () ->
                    KbvErpBundleFaker.builder() // buggy faker!
                        .withPractitioner(
                            KbvPractitionerFaker.builder()
                                .withQualificationType(QualificationType.DOCTOR)
                                .withAnr(LANR.random())
                                .fake())
                        .fake()),
        Arguments.of(
            EMedication.class,
            (Supplier<Resource>) () -> KbvErpMedicationPZNFaker.builder().fake()),
        Arguments.of(
            KbvEOrganizationOut.class,
            (Supplier<Resource>) () -> KbvMedicalOrganizationFaker.medicalPractice().fake()),
        Arguments.of(
            KbvEMedicationRequestOut.class,
            (Supplier<Resource>) () -> KbvErpMedicationRequestFaker.builder().fake()),
        Arguments.of(
            KbvEPatientOut.class, (Supplier<Resource>) () -> KbvPatientFaker.builder().fake()),
        Arguments.of(
            KbvEPractitionerOut.class,
            (Supplier<Resource>) () -> KbvPractitionerFaker.builder().fake()),
        // TODO: KbvErpBundleFaker messes around with Dentists
        Arguments.of(
            KbvEPrescriptionOut.class,
            (Supplier<Resource>)
                () ->
                    KbvErpBundleFaker.builder()
                        .withPractitioner(
                            KbvPractitionerFaker.builder()
                                .withQualificationType(QualificationType.DOCTOR)
                                .fake())
                        .withCustodian(KbvMedicalOrganizationFaker.medicalPractice().fake())
                        .fake())
        //    Arguments.of(
        //            KbvEPrescription.class, (Supplier<Resource>) () -> KbvErpBundleFaker.builder()
        //
        // .withPractitioner(KbvPractitionerFaker.builder().withQualificationType(QualificationType.DENTIST).fake())
        //
        // .withCustodian(KbvMedicalOrganizationFaker.dentalPractice().toBuilder().kzva(KZVA.random()).build()).fake())
        );
  }

  static Stream<Arguments> shouldTransformToPrescription() {
    return ExamplesReader.readExampleFiles("classpath:/examples/prescription/evdga/*.json").stream()
        .map(it -> Arguments.of(KbvErpBundle.class, it.getSecond()));
  }

  @ParameterizedTest
  @MethodSource
  void shouldTransformToResponseData(
      Class<? extends ErpioneOutputData> expectedData, Supplier<Resource> r4Supplier) {
    val r4 = r4Supplier.get();
    val data = assertDoesNotThrow(() -> hermes.toJson(expectedData, r4));
    assertNotNull(data);
  }

  @Test
  void shouldTransformFromPharmaceuticalPrescription() {
    val kbvBundle = KbvErpBundleFaker.builder().fake();
    val data = assertDoesNotThrow(() -> hermes.toJson(KbvEPrescriptionOut.class, kbvBundle));
    assertNotNull(data);
  }

  @ParameterizedTest
  @MethodSource
  <R extends KbvBaseBundle> void shouldTransformToPrescription(Class<R> r4Class, String input)
      throws IOException {
    val data = objectMapper.readValue(input, KbvEPrescriptionIn.class);

    val encoded = hermes.toFhirString(r4Class, data);
    val vr = hermes.validate(encoded);
    if (!vr.isSuccessful()) {
      ValidatorUtil.printValidationResult(vr);
    }
    assertTrue(vr.isSuccessful());

    val resource = assertDoesNotThrow(() -> hermes.decode(r4Class, encoded));
    assertNotNull(resource);
  }

  @Test
  void shouldThrowOnUnknownTransformer01() {
    val data = new KbvECoverageIn();
    assertThrows(
        HermesTransformerNotFoundError.class,
        () -> hermes.toFhirResource(KbvErpBundle.class, data),
        "Expected to throw because no transformer is registered for such combination");

    assertThrows(
        HermesTransformerNotFoundError.class,
        () -> hermes.toFhirBuilder(data, KbvErpBundle.class, KbvErpBundleBuilder.class),
        "Expected to throw because no transformer is registered for such combination");

    assertThrows(
        HermesTransformerNotFoundError.class,
        () -> hermes.toFhirString(KbvErpBundle.class, data),
        "Expected to throw because no transformer is registered for such combination");
  }

  @Test
  void shouldThrowOnUnknownTransformer02() {
    val resource = new KbvErpBundle();
    assertThrows(
        HermesTransformerNotFoundError.class,
        () -> hermes.toJson(KbvECoverageOut.class, resource),
        "Expected to throw because no transformer is registered for such combination");
  }
}
