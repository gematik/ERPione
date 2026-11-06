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

import static java.util.Optional.ofNullable;

import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.MedicationEBatchIn;
import de.gematik.erpione.data.r4.gem.MedicationDispenseEBundleIn;
import de.gematik.erpione.data.r4.gem.MedicationDispenseEPackageIn;
import de.gematik.erpione.data.r4.gem.MedicationEDispenseIn;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.erp.ErxMedicationDispenseBuilder;
import de.gematik.test.erezept.fhir.builder.erp.GemDispenseCloseOperationPharmaceuticalsBuilder;
import de.gematik.test.erezept.fhir.r4.erp.ErxMedicationDispense;
import de.gematik.test.erezept.fhir.r4.erp.GemErpMedication;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.data.util.Pair;

/**
 * Dispensing ($dispense and $close) operation parameters transformer which transform
 *
 * @param <P>
 */
@RequiredArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract sealed class DispOpParametersTransformer<P extends Parameters>
    implements JsonToFhirTransformer<
        MedicationDispenseEBundleIn, P, GemDispenseCloseOperationPharmaceuticalsBuilder<P>>
    permits DispenseParametersTransformer, CloseParametersTransformer {

  protected final HermesService hermes;

  protected void extractDataTo(
      MedicationDispenseEBundleIn data,
      GemDispenseCloseOperationPharmaceuticalsBuilder<P> builder) {

    if (data.getDispensation().isEmpty()) {
      // make sure at least on entry is present to avoid generating an empty bundle which is not
      // allowed by the FHIR spec
      data =
          new MedicationDispenseEBundleIn(
              List.of(
                  new MedicationDispenseEPackageIn(
                      new EMedication(), new MedicationEBatchIn(), new MedicationEDispenseIn())));
    }

    data.getDispensation().stream()
        .filter(it -> it.getMedication() != null)
        .map(
            it -> {
              val hermesMedication = ofNullable(it.getMedication()).orElseGet(EMedication::new);
              val hermesBatch = ofNullable(it.getBatch()).orElseGet(MedicationEBatchIn::new);

              val fhirMedicationBuilder =
                  hermes.toFhirBuilder(
                      hermesMedication, GemErpMedication.class, HermesGemMedicationBuilder.class);

              // Note: theoretically we can encode the lot number here and in tne MedicationDispense
              // This makes sure that the lot number is always identical in both resources
              val lotNumber =
                  ofNullable(hermesBatch.getLotNumber()).orElseGet(GemFaker::fakerLotNumber);
              val expiryDate =
                  ofNullable(hermesBatch.getExpiryDate())
                      .orElseGet(GemFaker::fakerFutureExpirationDate);

              val fhirMedication = fhirMedicationBuilder.lotNumber(lotNumber).build();
              val hermesMedDisp =
                  ofNullable(it.getDispense()).orElseGet(MedicationEDispenseIn::new);
              val fhirMedDisp =
                  hermes
                      .toFhirBuilder(
                          hermesMedDisp,
                          ErxMedicationDispense.class,
                          ErxMedicationDispenseBuilder.class)
                      .medication(fhirMedication)
                      .batch(lotNumber, expiryDate)
                      .build();
              return Pair.of(fhirMedDisp, fhirMedication);
            })
        .forEach(pair -> builder.with(pair.getFirst(), pair.getSecond()));
  }
}
