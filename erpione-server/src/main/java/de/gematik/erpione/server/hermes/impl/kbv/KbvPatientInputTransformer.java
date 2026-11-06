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

import static de.gematik.erpione.server.hermes.impl.HermesUtils.completeName;
import static de.gematik.erpione.server.hermes.impl.HermesUtils.requireNonNullElseGet;

import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.bbriccs.fhir.de.valueset.InsuranceTypeDe;
import de.gematik.erpione.data.r4.kbv.KbvEPatientIn;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.kbv.KbvPatientBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvPatientFaker;
import de.gematik.test.erezept.fhir.r4.kbv.KbvPatient;
import java.util.Optional;
import lombok.val;

public class KbvPatientInputTransformer
    implements JsonToFhirTransformer<KbvEPatientIn, KbvPatient, KbvPatientBuilder> {

  @Override
  public KbvPatientBuilder toFhir(KbvEPatientIn data) {
    val kvnrValue = requireNonNullElseGet(data.getKvnr(), KVNR::randomStringValue);

    val name = completeName(data.getName());
    val city = requireNonNullElseGet(data.getCity(), GemFaker::fakerCity);
    val postal = requireNonNullElseGet(data.getPostal(), GemFaker::fakerZipCode);
    // Note: fullstreet with secondary info not accepted by FD
    val street = requireNonNullElseGet(data.getStreet(), () -> GemFaker.fullStreetName(false));

    val faker =
        KbvPatientFaker.builder()
            .withName(name.getPrefix(), name.getGiven(), name.getFamily())
            .withAddress(city, postal, street);

    faker.withKvnrAndInsuranceType(KVNR.asGkv(kvnrValue), InsuranceTypeDe.GKV);

    Optional.ofNullable(data.getBirthDate()).ifPresent(faker::withBirthDate);

    return faker.toBuilder();
  }

  @Override
  public Class<KbvEPatientIn> jsonType() {
    return KbvEPatientIn.class;
  }

  @Override
  public Class<KbvPatient> fhirType() {
    return KbvPatient.class;
  }
}
