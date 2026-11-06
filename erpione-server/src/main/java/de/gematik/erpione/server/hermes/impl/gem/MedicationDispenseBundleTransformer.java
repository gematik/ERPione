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

import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.MedicationEBatchOut;
import de.gematik.erpione.data.r4.gem.MedicationDispenseEBundleOut;
import de.gematik.erpione.data.r4.gem.MedicationDispenseEPackageOut;
import de.gematik.erpione.data.r4.gem.MedicationEDispenseOut;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.erp.ErxMedicationDispenseBundle;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class MedicationDispenseBundleTransformer
    implements FhirToJsonTransformer<MedicationDispenseEBundleOut, ErxMedicationDispenseBundle> {

  private final HermesService hermes;

  /**
   * Transforms a FHIR resource to a JSON object.
   *
   * @param resource the FHIR resource to be transformed
   * @return a JSON object containing the data from the FHIR resource
   */
  @Override
  public MedicationDispenseEBundleOut toJson(ErxMedicationDispenseBundle resource) {
    val prescriptionId = resource.getMedicationDispenses().getFirst().getPrescriptionId();
    val dispenseData =
        resource.getDispensePairBy(prescriptionId).stream()
            .map(
                pair -> {
                  // original FHIR structures
                  val medDispFhir = pair.getLeft();
                  val gemErpMed = pair.getRight();

                  val dispenseInfo = hermes.toJson(MedicationEDispenseOut.class, medDispFhir);
                  val medInfo = hermes.toJson(EMedication.class, gemErpMed);

                  // extract the lot number and expiration date from the gemErpMed
                  val batch =
                      gemErpMed
                          .getBatchLotNumber()
                          .map(
                              lotNumber -> {
                                val expiry = gemErpMed.getBatch().getExpirationDate();
                                return new MedicationEBatchOut(lotNumber, expiry);
                              })
                          .orElse(null);
                  return new MedicationDispenseEPackageOut(medInfo, batch, dispenseInfo);
                })
            .toList();

    return new MedicationDispenseEBundleOut(dispenseData);
  }

  /**
   * @return
   */
  @Override
  public Class<MedicationDispenseEBundleOut> jsonType() {
    return MedicationDispenseEBundleOut.class;
  }

  /**
   * @return
   */
  @Override
  public Class<ErxMedicationDispenseBundle> fhirType() {
    return ErxMedicationDispenseBundle.class;
  }
}
