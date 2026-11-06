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

import de.gematik.bbriccs.fhir.de.value.PZN;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.etype.StandardESize;
import de.gematik.erpione.server.hermes.impl.HermesUtils;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.kbv.*;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedication;
import de.gematik.test.erezept.fhir.valuesets.Darreichungsform;
import de.gematik.test.erezept.fhir.valuesets.MedicationCategory;
import de.gematik.test.erezept.fhir.valuesets.StandardSize;
import java.util.List;
import java.util.Optional;
import lombok.val;
import org.jetbrains.annotations.Nullable;

public class KbvMedicationInputTransformer
    implements JsonToFhirTransformer<EMedication, KbvErpMedication, HermesKbvMedicationBuilder> {

  private static final List<String> PACKING_UNITS =
      List.of("g", "mg", "{tbl}", "ml", "Stück", "Tabletten", "Diskus");

  @Override
  public HermesKbvMedicationBuilder toFhir(EMedication data) {
    val mt = data.getType();

    val builder =
        switch (mt) {
          case PZN -> toFhirAsPznBuilder(data);
          case FREETEXT -> toFhirAsFreetextBuilder(data);
          case INGREDIENT -> toFhirAsIngredientBuilder(data);
          case COMPOUNDING -> toFhirAsCompoundingBuilder(data);
        };

    return new HermesKbvMedicationBuilder(setCommons(data, builder));
  }

  private KbvErpMedicationCompoundingBuilder toFhirAsCompoundingBuilder(EMedication data) {
    val faker = KbvErpMedicationCompoundingFaker.builder();

    Optional.ofNullable(data.getSupplyForm())
        .ifPresent(it -> faker.withDosageForm(it.getDisplay()));

    val name = HermesUtils.ofNullable(data.getName()).orElseGet(GemFaker::fakerDrugName);
    val pzn =
        HermesUtils.ofNullable(data.getPzn()).orElseGet(() -> PZN.random().getValueAsString());
    val freeText = data.getFreeText();
    HermesUtils.ofNullable(data.getName())
        .ifPresent(it -> faker.withMedicationIngredient(pzn, name, freeText));

    HermesUtils.ofNullable(data.getPackaging()).ifPresent(faker::withPackaging);

    return faker.toBuilder();
  }

  private KbvErpMedicationIngredientBuilder toFhirAsIngredientBuilder(EMedication data) {
    val faker = KbvErpMedicationIngredientFaker.builder();

    HermesUtils.ofNullable(data.getName()).ifPresent(faker::withDrugName);
    Optional.ofNullable(data.getSupplyForm()).ifPresent(it -> faker.withDosageForm(it.getCode()));
    Optional.ofNullable(data.getStandardSize())
        .ifPresent(it -> faker.withStandardSize(StandardSize.fromCode(it.getCode())));

    return faker.toBuilder();
  }

  private KbvErpMedicationFreeTextBuilder toFhirAsFreetextBuilder(EMedication data) {
    val faker = KbvErpMedicationFreeTextFaker.builder();

    HermesUtils.ofNullable(data.getFreeText()).ifPresent(faker::withFreeText);
    Optional.ofNullable(data.getSupplyForm())
        .ifPresent(it -> faker.withDosageForm(it.getDisplay()));

    return faker.toBuilder();
  }

  private KbvErpMedicationPZNBuilder toFhirAsPznBuilder(EMedication data) {
    val pzn = HermesUtils.requireNonNullElseGet(data.getPzn(), () -> PZN.random().getValue());
    val name = HermesUtils.requireNonNullElseGet(data.getName(), GemFaker::fakerDrugName);

    val packagingSize = convertAmount(data.getPackagingSize());
    val packagingUnit =
        Optional.ofNullable(data.getPackagingUnit()).orElse(GemFaker.randomElement(PACKING_UNITS));

    val faker =
        KbvErpMedicationPZNFaker.builder()
            .withPznMedication(pzn, name)
            .withAmount(packagingSize, packagingUnit);

    faker.withStandardSize(convertStandardsize(data.getStandardSize()));

    Optional.ofNullable(data.getSupplyForm())
        .ifPresent(it -> faker.withSupplyForm(Darreichungsform.fromCode(it.getCode())));

    return faker.toBuilder();
  }

  private KbvErpMedicationBaseBuilder<?> setCommons(
      EMedication source, KbvErpMedicationBaseBuilder<?> builder) {
    Optional.ofNullable(source.getCategory())
        .ifPresent(it -> builder.category(MedicationCategory.fromCode(it.getCode())));

    return builder.isVaccine(source.getVaccine());
  }

  /**
   * StandardSize aka Normgröße
   *
   * @param standardSize which was set in EMedication
   * @return according StandardSize value or a random one if null
   */
  private StandardSize convertStandardsize(@Nullable StandardESize standardSize) {
    return Optional.ofNullable(standardSize)
        .map(it -> StandardSize.fromCode(it.getCode()))
        .orElseGet(() -> GemFaker.fakerValueSet(StandardSize.class));
  }

  private int convertAmount(@Nullable String packagingSize) {
    val pss =
        Optional.ofNullable(packagingSize)
            .orElse(String.valueOf(GemFaker.getFaker().random().nextInt(1, 500)));
    return Integer.parseInt(pss);
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
