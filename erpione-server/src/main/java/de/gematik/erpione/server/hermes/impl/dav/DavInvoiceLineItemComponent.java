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

import de.gematik.bbriccs.fhir.coding.exceptions.MissingFieldException;
import de.gematik.test.erezept.fhir.profiles.definitions.AbdaErpBasisStructDef;
import de.gematik.test.erezept.fhir.r4.dav.DavInvoice;
import lombok.val;
import org.hl7.fhir.r4.model.Invoice;

// TODO: this custom class belongs into erp-fhir
public class DavInvoiceLineItemComponent extends Invoice.InvoiceLineItemComponent {

  /**
   * Value-added-tax / Mehrwertsteuer
   *
   * @return Tax percentage
   */
  public float getVAT() {
    return this.getPriceComponentFirstRep().getExtension().stream()
        .filter(AbdaErpBasisStructDef.MWST_SATZ::matches)
        .findFirst()
        .map(ext -> Float.parseFloat(ext.getValue().primitiveValue()))
        .orElseThrow(
            () -> new MissingFieldException(DavInvoice.class, AbdaErpBasisStructDef.MWST_SATZ));
  }

  public static DavInvoiceLineItemComponent fromComponent(
      Invoice.InvoiceLineItemComponent adaptee) {
    if (adaptee instanceof DavInvoiceLineItemComponent component) {
      return component;
    }
    val component = new DavInvoiceLineItemComponent();
    adaptee.copyValues(component);
    return component;
  }
}
