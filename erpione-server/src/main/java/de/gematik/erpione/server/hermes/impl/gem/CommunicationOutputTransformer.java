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

import com.google.common.base.Strings;
import de.gematik.erpione.data.error.InvalidCommunicationPayloadError;
import de.gematik.erpione.data.r4.gem.com.CommunicationEDataOut;
import de.gematik.erpione.data.r4.gem.com.CommunicationEPayload;
import de.gematik.erpione.data.r4.gem.com.CommunicationEReferenceOut;
import de.gematik.erpione.data.r4.gem.com.CommunicationEType;
import de.gematik.erpione.data.r4.gem.com.CommunicationPayloadEType;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.erp.ErxCommunication;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class CommunicationOutputTransformer
    implements FhirToJsonTransformer<CommunicationEDataOut, ErxCommunication> {

  private final ObjectMapper jsonMapper;

  @Override
  public CommunicationEDataOut toJson(ErxCommunication resource) {
    val fhirType = resource.getType();
    val type = CommunicationEType.fromString(fhirType.name());
    val id = resource.getUnqualifiedId();
    val sender = resource.getSenderId();
    val recipient = resource.getRecipientId();

    val message = readPayload(resource.getMessage());

    val pid = resource.getBasedOnReferenceId().getValue();
    val ac = resource.getBasedOnAccessCodeString().orElse(null);
    val basedOn = new CommunicationEReferenceOut(pid, ac);

    // sent is not allowed to be null!
    val sent = Optional.ofNullable(resource.getSent()).orElseGet(() -> new Date(0));
    val received = resource.getReceived();
    val orderId = resource.getOrderId().orElse(null);
    return new CommunicationEDataOut(
        type, id, sender, recipient, message, basedOn, sent, received, orderId);
  }

  private CommunicationEPayload readPayload(String message) {
    if (Strings.isNullOrEmpty(message) || !message.startsWith("{")) {
      return CommunicationEPayload.builder()
          .type(CommunicationPayloadEType.TEXT)
          .text(message)
          .build();
    }

    try {
      return jsonMapper.readValue(message, CommunicationEPayload.class);
    } catch (Exception e) {
      throw new InvalidCommunicationPayloadError("Failed to read payload", e);
    }
  }

  /**
   * @return
   */
  @Override
  public Class<CommunicationEDataOut> jsonType() {
    return CommunicationEDataOut.class;
  }

  /**
   * @return
   */
  @Override
  public Class<ErxCommunication> fhirType() {
    return ErxCommunication.class;
  }
}
