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

import de.gematik.erpione.data.r4.gem.MedicationEDispenseOut;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.erp.ErxMedicationDispense;
import lombok.val;
import org.hl7.fhir.r4.model.Annotation;

public class MedicationDispenseOutputTransformer
    implements FhirToJsonTransformer<MedicationEDispenseOut, ErxMedicationDispense> {

  /**
   * Transforms a FHIR resource to a JSON object.
   *
   * @param resource the FHIR resource to be transformed
   * @return a JSON object containing the data from the FHIR resource
   */
  @Override
  public MedicationEDispenseOut toJson(ErxMedicationDispense resource) {
    val whenHandedOver = resource.getWhenHandedOver();
    val whenPrepared = resource.getWhenPrepared();
    val dosageInstructions = resource.getDosageInstructionText();
    val notes = resource.getNote().stream().map(Annotation::getText).toList();

    return new MedicationEDispenseOut(whenHandedOver, whenPrepared, dosageInstructions, notes);
  }

  /**
   * @return
   */
  @Override
  public Class<MedicationEDispenseOut> jsonType() {
    return MedicationEDispenseOut.class;
  }

  /**
   * @return
   */
  @Override
  public Class<ErxMedicationDispense> fhirType() {
    return ErxMedicationDispense.class;
  }
}
