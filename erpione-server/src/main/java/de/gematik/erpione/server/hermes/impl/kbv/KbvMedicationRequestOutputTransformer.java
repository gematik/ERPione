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

import static java.util.Optional.ofNullable;

import de.gematik.erpione.data.r4.etype.AccidentEType;
import de.gematik.erpione.data.r4.kbv.KbvAccidentEData;
import de.gematik.erpione.data.r4.kbv.KbvEMedicationRequestOut;
import de.gematik.erpione.data.r4.kbv.KbvMvoEData;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedicationRequest;
import lombok.val;

public class KbvMedicationRequestOutputTransformer
    implements FhirToJsonTransformer<KbvEMedicationRequestOut, KbvErpMedicationRequest> {

  @Override
  public KbvEMedicationRequestOut toJson(KbvErpMedicationRequest resource) {
    val quantity = resource.getDispenseQuantity();
    val dosage =
        resource.getRenderedDosageInstruction().orElseGet(resource::getDosageInstructionAsText);
    val bvg = resource.isBvg();
    val emergencyFee = resource.hasEmergencyServiceFee();
    val substitutionAllowed = resource.isSubstitutionAllowed();
    val note = resource.getNoteText().orElse("");

    // map MVO
    KbvMvoEData mvo = null;
    if (resource.isMultiple()) {
      mvo = new KbvMvoEData();
      resource.getDemoninator().ifPresent(mvo::setDenominator);
      resource.getNumerator().ifPresent(mvo::setNumerator);
      resource.getMvoStart().ifPresent(mvo::setStartDate);
      resource.getMvoEnd().ifPresent(mvo::setEndDate);
    }

    KbvAccidentEData acc =
        resource
            .getAccident()
            .map(
                ext -> {
                  val newAcc = new KbvAccidentEData();
                  ofNullable(ext.accidentDay()).ifPresent(newAcc::setDate);
                  ofNullable(ext.accidentCauseType())
                      .map(cause -> AccidentEType.fromString(cause.getCode()))
                      .ifPresent(newAcc::setType);
                  ofNullable(ext.workplace()).ifPresent(newAcc::setOrganization);
                  return newAcc;
                })
            .orElse(null);
    val empIdentifier = resource.getEmpIdentifier().orElse(null);

    return new KbvEMedicationRequestOut(
        dosage, quantity, note, empIdentifier, substitutionAllowed, bvg, emergencyFee, mvo, acc);
  }

  @Override
  public Class<KbvEMedicationRequestOut> jsonType() {
    return KbvEMedicationRequestOut.class;
  }

  @Override
  public Class<KbvErpMedicationRequest> fhirType() {
    return KbvErpMedicationRequest.class;
  }
}
