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

import de.gematik.erpione.data.r4.kbv.KbvEOrganizationIn;
import de.gematik.erpione.server.hermes.impl.HermesUtils;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.kbv.KbvMedicalOrganizationBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvMedicalOrganizationFaker;
import de.gematik.test.erezept.fhir.r4.kbv.KbvMedicalOrganization;
import lombok.val;

public class KbvMedicalOrganizationInputTransformer
    implements JsonToFhirTransformer<
        KbvEOrganizationIn, KbvMedicalOrganization, KbvMedicalOrganizationBuilder> {

  @Override
  public KbvMedicalOrganizationBuilder toFhir(KbvEOrganizationIn data) {
    // TODO: check the type of organization first
    val faker = KbvMedicalOrganizationFaker.medicalPractice();

    val city = HermesUtils.requireNonNullElseGet(data.getCity(), GemFaker::fakerCity);
    val postal = HermesUtils.requireNonNullElseGet(data.getPostal(), GemFaker::fakerZipCode);
    // fullstreet with secondary info not accepted by FD
    val street =
        HermesUtils.requireNonNullElseGet(data.getStreet(), () -> GemFaker.fullStreetName(false));
    faker.withAddress(city, postal, street);

    HermesUtils.ofNullable(data.getBsnr()).ifPresent(faker::withBsnr);
    HermesUtils.ofNullable(data.getOfficeName()).ifPresent(faker::withName);
    HermesUtils.ofNullable(data.getPhone()).ifPresent(faker::withPhoneNumber);
    HermesUtils.ofNullable(data.getEmail()).ifPresent(faker::withEmail);

    return faker.toBuilder();
  }

  @Override
  public Class<KbvEOrganizationIn> jsonType() {
    return KbvEOrganizationIn.class;
  }

  @Override
  public Class<KbvMedicalOrganization> fhirType() {
    return KbvMedicalOrganization.class;
  }
}
