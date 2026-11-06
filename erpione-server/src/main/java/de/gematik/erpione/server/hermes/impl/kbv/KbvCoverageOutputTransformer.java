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
import de.gematik.erpione.data.r4.etype.*;
import de.gematik.erpione.data.r4.kbv.KbvECoverageOut;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.kbv.KbvCoverage;
import lombok.val;

public class KbvCoverageOutputTransformer
    implements FhirToJsonTransformer<KbvECoverageOut, KbvCoverage> {

  @Override
  public KbvECoverageOut toJson(KbvCoverage resource) {
    val insurantStatus = InsurantEStatus.fromString(resource.getInsurantState().getCode());
    val personGroup = PersonEGroup.fromString(resource.getPersonGroup().getCode());
    val iknr = resource.getIknr().map(SemanticValue::getValue).orElse(null);
    val name = resource.getName();

    val insuranceType =
        resource
            .getInsuranceKindOptional()
            .map(it -> InsuranceEType.fromString(it.getCode()))
            .orElse(null);
    val payorType =
        resource.getPayorType().map(it -> PayorEType.fromString(it.getCode())).orElse(null);
    val wop = resource.getWop().map(it -> WopEType.fromString(it.getCode())).orElse(null);

    return new KbvECoverageOut(
        insuranceType, payorType, iknr, name, wop, insurantStatus, personGroup);
  }

  @Override
  public Class<KbvECoverageOut> jsonType() {
    return KbvECoverageOut.class;
  }

  @Override
  public Class<KbvCoverage> fhirType() {
    return KbvCoverage.class;
  }
}
