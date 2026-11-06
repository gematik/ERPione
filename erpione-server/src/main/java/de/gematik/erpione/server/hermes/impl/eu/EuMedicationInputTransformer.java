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

package de.gematik.erpione.server.hermes.impl.eu;

import de.gematik.bbriccs.fhir.de.value.PZN;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.server.hermes.impl.HermesUtils;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.eu.EuMedicationBuilder;
import de.gematik.test.erezept.fhir.builder.eu.EuMedicationPZNFaker;
import de.gematik.test.erezept.fhir.r4.eu.EuMedication;
import de.gematik.test.erezept.fhir.valuesets.Darreichungsform;
import de.gematik.test.erezept.fhir.valuesets.StandardSize;
import java.util.List;
import java.util.Optional;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

public class EuMedicationInputTransformer
    implements JsonToFhirTransformer<EMedication, EuMedication, EuMedicationBuilder> {

  private static final List<String> PACKING_UNITS =
      List.of("g", "mg", "{tbl}", "ml", "Stück", "Tabletten", "Diskus");

  /**
   * transforms a Hermes input object to a FHIR resource builder
   *
   * @param data for the input of the fhir resource builder
   * @return a FHIR resource builder containing the data from the JSON object
   */
  @Override
  public EuMedicationBuilder toFhir(EMedication data) {
    val faker = EuMedicationPZNFaker.faker();

    val name = HermesUtils.ofNullable(data.getName()).orElseGet(GemFaker::fakerDrugName);
    val pzn = HermesUtils.ofNullable(data.getPzn()).map(PZN::from).orElseGet(PZN::random);
    faker.withPzn(pzn, name);

    Optional.ofNullable(data.getSupplyForm())
        .map(it -> Darreichungsform.fromCode(it.getCode()))
        .ifPresent(faker::withDarreichungsform);
    Optional.ofNullable(data.getStandardSize())
        .ifPresent(it -> faker.withStandardSize(StandardSize.fromCode(it.getCode())));

    val packagingSize =
        Optional.ofNullable(data.getPackagingSize())
            .filter(StringUtils::isNumeric)
            .map(Integer::decode)
            .orElse(GemFaker.getFaker().random().nextInt(1, 500));
    val packagingUnit =
        Optional.ofNullable(data.getPackagingUnit()).orElse(GemFaker.randomElement(PACKING_UNITS));

    // Note: amount and packagingsize probably wrong her
    faker.withAmount(packagingSize, packagingUnit);

    return faker.toBuilder();
  }

  @Override
  public Class<EMedication> jsonType() {
    return EMedication.class;
  }

  @Override
  public Class<EuMedication> fhirType() {
    return EuMedication.class;
  }
}
