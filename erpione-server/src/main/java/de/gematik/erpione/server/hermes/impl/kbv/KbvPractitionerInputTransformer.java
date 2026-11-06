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

import de.gematik.erpione.data.r4.kbv.KbvEPractitionerIn;
import de.gematik.erpione.server.hermes.impl.HermesUtils;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.kbv.KbvPractitionerBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvPractitionerFaker;
import de.gematik.test.erezept.fhir.r4.kbv.KbvPractitioner;
import de.gematik.test.erezept.fhir.values.BaseANR;
import de.gematik.test.erezept.fhir.values.BaseANR.ANRType;
import de.gematik.test.erezept.fhir.valuesets.QualificationType;
import lombok.val;

public class KbvPractitionerInputTransformer
    implements JsonToFhirTransformer<KbvEPractitionerIn, KbvPractitioner, KbvPractitionerBuilder> {

  @Override
  public KbvPractitionerBuilder toFhir(KbvEPractitionerIn data) {
    val faker =
        KbvPractitionerFaker.builder().withQualificationType(QualificationType.DOCTOR); // default

    val name = HermesUtils.completeName(data.getName());
    faker.withName(name.getPrefix(), name.getGiven(), name.getFamily());

    ofNullable(data.getAnr())
        .ifPresent(
            anr -> {
              val anrType = ofNullable(anr.getType()).map(ANRType::fromCode).orElse(ANRType.LANR);
              val qt =
                  anrType == ANRType.ZANR ? QualificationType.DENTIST : QualificationType.DOCTOR;
              val value =
                  HermesUtils.requireNonNullElseGet(
                      anr.getValue(), () -> BaseANR.randomFromQualification(qt).getValue());
              faker.withAnr(BaseANR.from(anrType, value));
            });

    /*
     * Note: the QualificationType is intentionally skipped because the faker will
     * always try to ensure that the qualification type is aligned with the ANR
     */
    val builder = faker.toBuilder();
    data.getQualifications().forEach(builder::addQualification);

    return builder;
  }

  @Override
  public Class<KbvEPractitionerIn> jsonType() {
    return KbvEPractitionerIn.class;
  }

  @Override
  public Class<KbvPractitioner> fhirType() {
    return KbvPractitioner.class;
  }
}
