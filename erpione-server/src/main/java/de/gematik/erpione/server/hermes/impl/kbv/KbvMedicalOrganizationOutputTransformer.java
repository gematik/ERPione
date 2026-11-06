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
import de.gematik.bbriccs.fhir.de.DeBasisProfilNamingSystem;
import de.gematik.erpione.data.r4.kbv.KbvEOrganizationOut;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.kbv.KbvMedicalOrganization;
import lombok.val;
import org.hl7.fhir.r4.model.Identifier;

public class KbvMedicalOrganizationOutputTransformer
    implements FhirToJsonTransformer<KbvEOrganizationOut, KbvMedicalOrganization> {
  @Override
  public KbvEOrganizationOut toJson(KbvMedicalOrganization resource) {

    val bsnr = extractBsnr(resource);
    val city = resource.getCity();
    val street = resource.getStreet();
    val postal = resource.getPostalCode();
    val phone = resource.getPhone();
    val email = resource.getMail();
    val name = resource.getName();

    return new KbvEOrganizationOut(name, bsnr, phone, email, city, postal, street);
  }

  /**
   * TODO: KbvMedicalOrganization cannot extract KZV-Abrechnungsnummer and other identifiers This is
   * a helper function which overcomes this limitation by extracting the BSNR or any other
   * identifier which is possible from the resource and returning it as a string. We will call all
   * of them BSNR while technically in FHIR these are encoded differently
   *
   * @return the 'BSNR'
   */
  private String extractBsnr(KbvMedicalOrganization resource) {
    return resource
        .getBsnr()
        .map(SemanticValue::getValue)
        .orElseGet(
            () ->
                resource.getIdentifier().stream()
                    .filter(DeBasisProfilNamingSystem.KZBV_KZVA_ABRECHNUNGSNUMMER_SID::matches)
                    .map(Identifier::getValue)
                    .findFirst()
                    .orElse("n/a"));
  }

  @Override
  public Class<KbvEOrganizationOut> jsonType() {
    return KbvEOrganizationOut.class;
  }

  @Override
  public Class<KbvMedicalOrganization> fhirType() {
    return KbvMedicalOrganization.class;
  }
}
