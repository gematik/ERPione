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

import static java.util.Optional.ofNullable;

import de.gematik.bbriccs.fhir.de.value.PZN;
import de.gematik.erpione.data.r4.chargeitem.*;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.dav.DavInvoiceBuilder;
import de.gematik.test.erezept.fhir.builder.dav.PriceComponentBuilder;
import de.gematik.test.erezept.fhir.r4.dav.DavInvoice;
import de.gematik.test.erezept.fhir.util.Currency;
import de.gematik.test.erezept.fhir.valuesets.dav.InvoiceType;
import de.gematik.test.erezept.fhir.valuesets.dav.KostenVersicherterKategorie;
import java.util.List;
import lombok.val;
import org.hl7.fhir.r4.model.Invoice;

public class DavInvoiceDataInputTransformer
    implements JsonToFhirTransformer<InvoiceEDataIn, DavInvoice, DavInvoiceBuilder> {
  /**
   * transforms a Hermes input object to a FHIR resource builder
   *
   * @param data for the input of the fhir resource builder
   * @return a FHIR resource builder containing the data from the JSON object
   */
  @Override
  public DavInvoiceBuilder toFhir(InvoiceEDataIn data) {
    val builder =
        DavInvoiceBuilder.builder(InvoiceType.ABRECHNUNGSZEILEN)
            .status(Invoice.InvoiceStatus.ISSUED)
            .currency(Currency.EUR);

    // there is always at least one line item required
    if (data.getItems().isEmpty()) {
      data.setItems(List.of(new InvoiceEDataLineIn()));
    }

    data.getItems()
        .forEach(
            item -> {
              val pcin = ofNullable(item.getPriceComponent()).orElseGet(PriceEComponentIn::new);
              val insurantCost = ofNullable(pcin.getInsurantCost()).orElse(0.0f);
              val totalCost = ofNullable(pcin.getTotalCost()).orElseGet(GemFaker::cost);
              val factor = ofNullable(pcin.getFactor()).orElse(1L);
              val name = ofNullable(item.getName()).orElseGet(GemFaker::fakerDrugName);
              // TODO: for now only PZN work
              val pzn = ofNullable(item.getCode()).orElseGet(() -> PZN.random().getValue());

              val pcc =
                  PriceComponentBuilder.builder(KostenVersicherterKategorie.ZUZAHLUNG)
                      .currency(Currency.EUR)
                      .type(Invoice.InvoicePriceComponentType.INFORMATIONAL)
                      .insurantCost(insurantCost)
                      .factor(factor)
                      .totalCost(totalCost)
                      .build();
              builder.addPriceComponent(pcc, pzn, name);
            });

    return builder;
  }

  /**
   * @return
   */
  @Override
  public Class<InvoiceEDataIn> jsonType() {
    return InvoiceEDataIn.class;
  }

  /**
   * @return
   */
  @Override
  public Class<DavInvoice> fhirType() {
    return DavInvoice.class;
  }
}
