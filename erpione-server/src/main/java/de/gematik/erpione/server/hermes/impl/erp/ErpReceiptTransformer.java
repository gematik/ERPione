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

package de.gematik.erpione.server.hermes.impl.erp;

import de.gematik.erpione.data.r4.etype.DocumentEType;
import de.gematik.erpione.data.r4.gem.ErpEReceiptOut;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.erp.ErxReceipt;
import lombok.val;

public class ErpReceiptTransformer implements FhirToJsonTransformer<ErpEReceiptOut, ErxReceipt> {

  @Override
  public ErpEReceiptOut toJson(ErxReceipt receipt) {
    val pid = receipt.getPrescriptionId().getValue();
    val docType = DocumentEType.fromString(receipt.getDocumentType().getCode());
    val signDate = receipt.getSignature().getWhen();
    val digest = receipt.getQesDigestBinary().getDataElement().getValueAsString();
    val qes = receipt.getSignature().getDataElement().getValueAsString();

    return new ErpEReceiptOut(pid, docType, signDate, digest, qes);
  }

  @Override
  public Class<ErpEReceiptOut> jsonType() {
    return ErpEReceiptOut.class;
  }

  @Override
  public Class<ErxReceipt> fhirType() {
    return ErxReceipt.class;
  }
}
