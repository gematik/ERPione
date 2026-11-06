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
import de.gematik.erpione.data.r4.kbv.KbvEPatientOut;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.kbv.KbvPatient;
import lombok.val;

public class KbvPatientOutputTransformer
    implements FhirToJsonTransformer<KbvEPatientOut, KbvPatient> {

  @Override
  public KbvEPatientOut toJson(KbvPatient patient) {
    val kvnr = patient.getKvnr().getValue();
    val name = extractName(patient);
    val city = patient.getAddressCity();
    val street = patient.getAddressStreet();
    val postal = patient.getAddressPostalCode();
    val birthDate = patient.getBirthDate();

    return new KbvEPatientOut(kvnr, name, birthDate, city, postal, street);
  }

  private HumanENameOut extractName(KbvPatient resource) {
    val prefix = resource.getNameFirstRep().getPrefixAsSingleString();
    val given = resource.getGivenName();
    val family = resource.getFamilyName();
    return new HumanENameOut(prefix, given, family);
  }

  @Override
  public Class<KbvEPatientOut> jsonType() {
    return KbvEPatientOut.class;
  }

  @Override
  public Class<KbvPatient> fhirType() {
    return KbvPatient.class;
  }
}
