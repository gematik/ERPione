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

import de.gematik.erpione.data.r4.HumanENameOut;
import de.gematik.erpione.data.r4.PhysicianENumberOut;
import de.gematik.erpione.data.r4.etype.QualificationEType;
import de.gematik.erpione.data.r4.kbv.KbvEPractitionerOut;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.kbv.KbvPractitioner;
import lombok.val;

public class KbvPractitionerOutputTransformer
    implements FhirToJsonTransformer<KbvEPractitionerOut, KbvPractitioner> {

  @Override
  public KbvEPractitionerOut toJson(KbvPractitioner resource) {

    val anr =
        resource
            .getANR()
            .map(
                it -> {
                  val type = it.getType().name();
                  val value = it.getValue();
                  return new PhysicianENumberOut(type, value);
                })
            .orElse(null);

    val name = extractName(resource);
    val qtype = QualificationEType.fromString(resource.getQualificationType().getCode());
    val qualifications = resource.getAdditionalQualifications();

    return new KbvEPractitionerOut(name, qtype, anr, qualifications);
  }

  private HumanENameOut extractName(KbvPractitioner resource) {
    val name = resource.getNameFirstRep();
    val prefix = name.getPrefixAsSingleString();
    val given = name.getGivenAsSingleString();
    val family = name.getFamily();
    return new HumanENameOut(prefix, given, family);
  }

  @Override
  public Class<KbvEPractitionerOut> jsonType() {
    return KbvEPractitionerOut.class;
  }

  @Override
  public Class<KbvPractitioner> fhirType() {
    return KbvPractitioner.class;
  }
}
