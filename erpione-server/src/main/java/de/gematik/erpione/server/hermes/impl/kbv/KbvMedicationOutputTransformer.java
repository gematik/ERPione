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

import de.gematik.bbriccs.fhir.coding.SemanticValue;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.etype.MedicationECategory;
import de.gematik.erpione.data.r4.etype.MedicationEType;
import de.gematik.erpione.data.r4.etype.StandardESize;
import de.gematik.erpione.data.r4.etype.SupplyEForm;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedication;
import lombok.val;

public class KbvMedicationOutputTransformer
    implements FhirToJsonTransformer<EMedication, KbvErpMedication> {

  @Override
  public EMedication toJson(KbvErpMedication resource) {
    val medicationType =
        resource
            .getMedicationType()
            .map(it -> MedicationEType.fromString(it.getCode()))
            .orElse(MedicationEType.PZN);

    val data = createBaseMedicationCommons(medicationType, resource);
    if (medicationType == MedicationEType.PZN) {
      data.setName(resource.getMedicationName());
    }
    return data;
  }

  private EMedication createBaseMedicationCommons(MedicationEType type, KbvErpMedication resource) {
    val data = new EMedication();

    data.setType(type);
    data.setCategory(MedicationECategory.fromString(resource.getCategoryFirstRep().getCode()));
    resource.getIngredientText().ifPresent(data::setName);
    resource.getPznOptional().map(SemanticValue::getValue).ifPresent(data::setPzn);

    resource.getIngredientText().ifPresent(data::setName);
    resource
        .getDarreichungsform()
        .map(df -> SupplyEForm.fromString(df.getCode()))
        .ifPresent(data::setSupplyForm);

    resource.getPackagingSize().ifPresent(data::setPackagingSize);
    resource.getPackagingUnit().ifPresent(data::setPackagingUnit);
    resource.getFreeTextOptional().ifPresent(data::setFreeText);

    // if not given Normgröße will default to KA (keine Angabe) therefore no need for optional
    data.setStandardSize(StandardESize.fromString(resource.getStandardSize().getCode()));

    return data;
  }

  @Override
  public Class<EMedication> jsonType() {
    return EMedication.class;
  }

  @Override
  public Class<KbvErpMedication> fhirType() {
    return KbvErpMedication.class;
  }
}
