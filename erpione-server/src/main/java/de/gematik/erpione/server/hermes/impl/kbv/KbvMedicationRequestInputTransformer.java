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

package de.gematik.erpione.server.hermes.impl.kbv;

import de.gematik.erpione.data.r4.etype.AccidentEType;
import de.gematik.erpione.data.r4.kbv.KbvAccidentEData;
import de.gematik.erpione.data.r4.kbv.KbvEMedicationRequestIn;
import de.gematik.erpione.server.hermes.impl.HermesUtils;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpMedicationRequestBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpMedicationRequestFaker;
import de.gematik.test.erezept.fhir.extensions.kbv.AccidentExtension;
import de.gematik.test.erezept.fhir.extensions.kbv.MultiplePrescriptionExtension;
import de.gematik.test.erezept.fhir.extensions.kbv.MultiplePrescriptionIdExtension;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedicationRequest;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import lombok.val;
import org.apache.logging.log4j.util.Strings;

public class KbvMedicationRequestInputTransformer
    implements JsonToFhirTransformer<
        KbvEMedicationRequestIn, KbvErpMedicationRequest, KbvErpMedicationRequestBuilder> {

  @Override
  public KbvErpMedicationRequestBuilder toFhir(KbvEMedicationRequestIn data) {
    val faker = KbvErpMedicationRequestFaker.builder();

    HermesUtils.ofNullable(data.getDosage()).ifPresent(faker::withDosageInstruction);
    Optional.ofNullable(data.getPackageQuantity()).ifPresent(faker::withDispenseQuantity);

    Optional.ofNullable(data.getEmergencyFee()).ifPresent(faker::withEmergencyServiceFee);
    Optional.ofNullable(data.getSubstitutionAllowed()).ifPresent(faker::withSubstitution);

    Optional.ofNullable(data.getMvo())
        .map(
            it -> {
              val numerator = Objects.requireNonNullElse(it.getNumerator(), 1);
              val denominator = Objects.requireNonNullElse(it.getDenominator(), 4);
              val mvoId =
                  HermesUtils.ofNullable(it.getMvoId())
                      .map(MultiplePrescriptionIdExtension::withId)
                      .orElseGet(MultiplePrescriptionIdExtension::randomId);

              return MultiplePrescriptionExtension.asMultiple(numerator, denominator)
                  .withId(mvoId)
                  .validThrough(it.getStartDate(), it.getEndDate());
            })
        .ifPresent(faker::withMvo);

    Optional.ofNullable(data.getAccident()).map(this::toExtension).ifPresent(faker::withAccident);

    // chuck norris easter egg
    HermesUtils.ofNullable(data.getNote())
        .ifPresentOrElse(
            faker::withNote, () -> faker.withNote(GemFaker.getFaker().chuckNorris().fact()));

    // Note: faker does not have withEmpIdentifier, so we need to use the builder directly
    val builder = faker.toBuilder();
    Optional.ofNullable(data.getEmpIdentifier())
        .filter(Strings::isNotBlank)
        .ifPresent(builder::basedOnEMP);

    return builder;
  }

  /**
   * @param data the incoming AccidentData
   * @return a Kbv accident extension corresponding to the input fields Sets default values if no
   *     data for those fields are provided
   */
  private AccidentExtension toExtension(KbvAccidentEData data) {
    val accType = Objects.requireNonNullElse(data.getType(), AccidentEType.fromString("1"));
    val accOrg = Objects.requireNonNullElse(data.getOrganization(), "Arbeitsplatz");
    val accDate =
        accType != AccidentEType.OCCUPATIONAL_DISEASE
            ? Objects.requireNonNullElse(data.getDate(), new Date())
            : null;

    if (accType == AccidentEType.ACCIDENT) {
      return AccidentExtension.accident(accDate);
    } else if (accType == AccidentEType.WORK_ACCIDENT) {
      return AccidentExtension.accidentAtWork(accDate).atWorkplace(accOrg);
    } else {
      return AccidentExtension.occupationalDisease();
    }
  }

  @Override
  public Class<KbvEMedicationRequestIn> jsonType() {
    return KbvEMedicationRequestIn.class;
  }

  @Override
  public Class<KbvErpMedicationRequest> fhirType() {
    return KbvErpMedicationRequest.class;
  }
}
