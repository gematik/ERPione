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

import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.bbriccs.fhir.de.value.TelematikID;
import de.gematik.erpione.data.r4.gem.MedicationEDispenseIn;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.erp.ErxMedicationDispenseBuilder;
import de.gematik.test.erezept.fhir.r4.erp.ErxMedicationDispense;
import de.gematik.test.erezept.fhir.values.PrescriptionId;

public class MedicationDispenseInputTransformer
    implements JsonToFhirTransformer<
        MedicationEDispenseIn, ErxMedicationDispense, ErxMedicationDispenseBuilder> {

  /**
   * transforms a JSON object to a FHIR resource builder
   *
   * @param data for the input of the fhir resource builder
   * @return a FHIR resource builder containing the data from the JSON object
   */
  @Override
  public ErxMedicationDispenseBuilder toFhir(MedicationEDispenseIn data) {
    // TODO
    return ErxMedicationDispenseBuilder.forKvnr(KVNR.random())
        .prescriptionId(PrescriptionId.random())
        //                .batch(data.getBatch().getLotNumber(), data.getBatch().getExpiryDate())
        .performerId(TelematikID.random());
  }

  /**
   * @return
   */
  @Override
  public Class<MedicationEDispenseIn> jsonType() {
    return MedicationEDispenseIn.class;
  }

  /**
   * @return
   */
  @Override
  public Class<ErxMedicationDispense> fhirType() {
    return ErxMedicationDispense.class;
  }
}
