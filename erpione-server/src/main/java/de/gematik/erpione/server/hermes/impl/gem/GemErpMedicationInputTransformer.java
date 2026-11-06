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

package de.gematik.erpione.server.hermes.impl.gem;

import static java.util.Optional.ofNullable;

import de.gematik.bbriccs.fhir.de.value.PZN;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.etype.SupplyEForm;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.eml.fhir.valuesets.EpaDrugCategory;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.erp.*;
import de.gematik.test.erezept.fhir.r4.erp.GemErpMedication;
import de.gematik.test.erezept.fhir.valuesets.Darreichungsform;
import de.gematik.test.erezept.fhir.valuesets.StandardSize;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.val;

public class GemErpMedicationInputTransformer
    implements JsonToFhirTransformer<EMedication, GemErpMedication, HermesGemMedicationBuilder> {

  /**
   * @return
   */
  @Override
  public Class<EMedication> jsonType() {
    return EMedication.class;
  }

  /**
   * @return
   */
  @Override
  public Class<GemErpMedication> fhirType() {
    return GemErpMedication.class;
  }

  /**
   * transforms a JSON object to a FHIR resource builder
   *
   * @param data for the input of the fhir resource builder
   * @return a FHIR resource builder containing the data from the JSON object
   */
  @Override
  public HermesGemMedicationBuilder toFhir(EMedication data) {
    val type = data.getType();

    if (data.getSupplyForm() == SupplyEForm.KPG) {
      return new HermesGemMedicationBuilder(toCombiPackage(data));
    } else {
      val builder =
          switch (type) {
            case PZN -> toPZN(data);
            case FREETEXT -> toFreeText(data);
            case INGREDIENT -> toIngredient(data);
            case COMPOUNDING -> toCompounding(data);
          };
      return new HermesGemMedicationBuilder(builder);
    }
  }

  private GemErpMedicationKombiPkgBuilder toCombiPackage(EMedication data) {
    val faker = GemErpMedicationFaker.forKombiPkg().withVaccine(data.getVaccine());

    faker.withPzn(ofNullable(data.getPzn()).map(PZN::from).orElse(PZN.random()));

    val builder = setCommons(faker.toBuilder(), data);

    setPackagingSizeFrom(data, builder::packagingSize, builder::amount);
    ofNullable(data.getFreeText()).ifPresent(builder::codeText);

    return builder;
  }

  private GemErpMedicationPZNBuilder toPZN(EMedication data) {
    val faker = GemErpMedicationFaker.forPznMedication().withVaccineFlag(data.getVaccine());
    faker.withPzn(
        ofNullable(data.getPzn()).map(PZN::from).orElse(PZN.random()),
        ofNullable(data.getName()).orElseGet(GemFaker::fakerDrugName));

    ofNullable(data.getStandardSize())
        .map(it -> StandardSize.fromCode(it.getCode()))
        .ifPresent(faker::withStandardSize);

    ofNullable(data.getSupplyForm())
        .map(it -> Darreichungsform.fromCode(it.getCode()))
        .ifPresent(faker::withDarreichungsform);

    // Note: faker is missing some of the setters, so DIY
    val builder = setCommons(faker.toBuilder(), data);

    setPackagingSizeFrom(data, builder::packagingSize, builder::amount);
    ofNullable(data.getFreeText()).ifPresent(builder::codeText);

    return builder;
  }

  private GemErpMedFreeTextBuilder toFreeText(EMedication data) {
    val faker = GemErpMedicationFaker.forFreeTextMedication();

    val builder = setCommons(faker.toBuilder(), data);
    ofNullable(data.getFreeText()).ifPresent(builder::formText);
    return builder;
  }

  private GemErpMedicationIngredientBuilder toIngredient(EMedication data) {
    val faker = GemErpMedicationFaker.forMedicationIngredient();

    ofNullable(data.getStandardSize())
        .map(it -> StandardSize.fromCode(it.getCode()))
        .ifPresent(faker::withStandardSize);

    ofNullable(data.getFreeText()).ifPresent(faker::withFreeText);

    val builder = setCommons(faker.toBuilder(), data);

    setPackagingSizeFrom(data, builder::packagingSize, builder::amount);

    return builder;
  }

  private GemErpMedicationCompoundingBuilder toCompounding(EMedication data) {
    val faker = GemErpMedicationFaker.forMedicationCompounding();

    val builder = setCommons(faker.toBuilder(), data);

    setPackagingSizeFrom(data, builder::packagingSize, builder::amount);

    ofNullable(data.getFreeText()).ifPresent(builder::formText);

    return builder;
  }

  private <B extends GemErpMedicationBuilder<B>> B setCommons(B builder, EMedication data) {
    builder.isVaccine(data.getVaccine());

    ofNullable(data.getCategory())
        .map(c -> EpaDrugCategory.fromCode(c.getCode()))
        .ifPresent(builder::category);

    return builder;
  }

  /**
   * unfortunately the builders have no common methods to set packaging size, amount and unit.
   * Therefore, use the consumers to deaul with these setters.
   *
   * @param data where the domain information is coming from
   * @param pSizeConsumer for setting the packaging size
   * @param amountConsumer for setting the amount and the unit
   */
  private void setPackagingSizeFrom(
      EMedication data,
      Consumer<String> pSizeConsumer,
      BiConsumer<Integer, String> amountConsumer) {
    ofNullable(data.getPackagingSize())
        .ifPresent(
            ps -> {
              // Note: amount is required when packaginSize is set
              val amount = GemFaker.fakerAmount(1, 20);
              val unit = ofNullable(data.getPackagingUnit()).orElse("Stück");
              pSizeConsumer.accept(ps);
              amountConsumer.accept(amount, unit);
            });
  }
}
