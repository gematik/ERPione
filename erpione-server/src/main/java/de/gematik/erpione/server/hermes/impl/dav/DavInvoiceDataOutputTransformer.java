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

package de.gematik.erpione.server.hermes.impl.dav;

import de.gematik.erpione.data.r4.chargeitem.*;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.profiles.definitions.AbdaErpBasisStructDef;
import de.gematik.test.erezept.fhir.r4.dav.DavInvoice;
import java.util.Optional;
import lombok.val;

public class DavInvoiceDataOutputTransformer
    implements FhirToJsonTransformer<InvoiceEDataOut, DavInvoice> {
  /**
   * Transforms a FHIR resource to a Hermes output object.
   *
   * @param resource the FHIR resource to be transformed
   * @return a JSON object containing the data from the FHIR resource
   */
  @Override
  public InvoiceEDataOut toJson(DavInvoice resource) {

    val lineItems =
        resource.getLineItem().stream()
            .map(DavInvoiceLineItemComponent::fromComponent)
            .map(
                li -> {
                  // TODO: missing getters in erp-fhir
                  val pc = li.getPriceComponentFirstRep();
                  val category =
                      pc.getExtension().stream()
                          .filter(AbdaErpBasisStructDef.KOSTEN_VERSICHERTER::matches)
                          .map(ext -> ext.getExtensionByUrl("Kategorie").getValue())
                          .map(type -> type.castToCodeableConcept(type).getCodingFirstRep())
                          .map(coding -> CostECategory.fromString(coding.getCode()))
                          .findFirst()
                          .orElse(CostECategory.SERVICE);
                  val type = pc.getType().getDisplay();
                  val factor = pc.getFactor().longValue();
                  val insurantCost =
                      Optional.ofNullable(pc.getExtensionByUrl("Kostenbetrag"))
                          .map(ext -> ext.getValue().castToMoney(ext.getValue()))
                          .map(money -> money.getValue().floatValue())
                          .orElse(0.0f);
                  val totalCost = pc.getAmount().getValue().floatValue();
                  val vat = li.getVAT();
                  val priceEComponent =
                      new PriceEComponentOut(category, type, factor, insurantCost, totalCost, vat);

                  val chargeItemCC = li.getChargeItemCodeableConcept();
                  val name = chargeItemCC.getText();
                  val code = chargeItemCC.getCodingFirstRep().getCode();
                  val system = chargeItemCC.getCodingFirstRep().getSystem();
                  val sequence = li.getSequence();

                  return new InvoiceEDataLineOut(sequence, name, system, code, priceEComponent);
                })
            .toList();

    val type = InvoiceEType.ABRECHNUNGSZEILEN; // TODO: missing getter
    val currency = resource.getCurrency();
    val total = resource.getTotalPrice();
    val copayment = resource.getTotalCoPayment();
    val tg = new TotalEGrossOut(copayment, total);
    return new InvoiceEDataOut(currency, type, tg, lineItems);
  }

  /**
   * @return
   */
  @Override
  public Class<InvoiceEDataOut> jsonType() {
    return InvoiceEDataOut.class;
  }

  /**
   * @return
   */
  @Override
  public Class<DavInvoice> fhirType() {
    return DavInvoice.class;
  }
}
