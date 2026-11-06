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

import de.gematik.bbriccs.fhir.de.value.IKNR;
import de.gematik.bbriccs.fhir.de.valueset.InsuranceTypeDe;
import de.gematik.erpione.data.r4.etype.InsuranceEType;
import de.gematik.erpione.data.r4.etype.PayorEType;
import de.gematik.erpione.data.r4.kbv.KbvECoverageIn;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.kbv.KbvCoverageBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvCoverageFaker;
import de.gematik.test.erezept.fhir.r4.kbv.KbvCoverage;
import de.gematik.test.erezept.fhir.valuesets.PayorType;
import de.gematik.test.erezept.fhir.valuesets.PersonGroup;
import de.gematik.test.erezept.fhir.valuesets.VersichertenStatus;
import de.gematik.test.erezept.fhir.valuesets.Wop;
import java.util.Optional;
import lombok.val;

public class KbvCoverageInputTransformer
    implements JsonToFhirTransformer<KbvECoverageIn, KbvCoverage, KbvCoverageBuilder> {

  @Override
  public KbvCoverageBuilder toFhir(KbvECoverageIn data) {
    val faker = KbvCoverageFaker.builder();

    if (data.getType() == null && data.getPayorType() == null) {
      data.setType(InsuranceEType.GKV);
    }

    faker.withInsuranceStatus(VersichertenStatus.fromCode(data.getInsurantState().getCode()));
    faker.withPersonGroup(PersonGroup.fromCode(data.getPersonGroup().getCode()));

    Optional.ofNullable(data.getWop()).ifPresent(it -> faker.withWop(Wop.fromCode(it.getCode())));

    // handle differences between insurance type and payor type
    val builder = faker.toBuilder();

    Optional.ofNullable(data.getIknr()).map(IKNR::asSidIknr).ifPresent(builder::iknr);
    Optional.ofNullable(data.getName()).ifPresent(builder::name);

    // first step: set the insurance type if given
    Optional.ofNullable(data.getType())
        .map(InsuranceEType::getCode)
        .map(InsuranceTypeDe::fromCode)
        .ifPresent(builder::insuranceType);
    // second step: if payor type is given, override insurance type accordingly because both at the
    // same time is impossible
    Optional.ofNullable(data.getPayorType())
        .map(PayorEType::getCode)
        .map(PayorType::fromCode)
        .ifPresent(builder::insuranceType);
    return builder;
  }

  @Override
  public Class<KbvECoverageIn> jsonType() {
    return KbvECoverageIn.class;
  }

  @Override
  public Class<KbvCoverage> fhirType() {
    return KbvCoverage.class;
  }
}
