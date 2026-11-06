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

import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.bbriccs.fhir.de.value.TelematikID;
import de.gematik.erpione.data.r4.gem.com.CollectionReadyEType;
import de.gematik.erpione.data.r4.gem.com.CommunicationEDataIn;
import de.gematik.erpione.data.r4.gem.com.CommunicationEPayload;
import de.gematik.erpione.data.r4.gem.com.CommunicationEType;
import de.gematik.erpione.data.r4.gem.com.CommunicationPayloadEType;
import de.gematik.erpione.data.r4.gem.com.DeliverStatusEType;
import de.gematik.erpione.server.error.ImpossibleOperationError;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.erp.ErxComReplyBuilder;
import de.gematik.test.erezept.fhir.builder.erp.ErxCommunicationBuilder;
import de.gematik.test.erezept.fhir.extensions.erp.CommunicationPayloadType;
import de.gematik.test.erezept.fhir.extensions.erp.SupplyOptionsType;
import de.gematik.test.erezept.fhir.r4.erp.ErxCommunication;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.json.CommunicationReplyMessage;
import de.gematik.test.erezept.fhir.values.json.CommunicationReplyMessage.InTransportPosition;
import de.gematik.test.erezept.fhir.values.json.CommunicationReplyMessage.PaymentMethod;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class CommunicationReplyInputTransformer
    implements JsonToFhirTransformer<CommunicationEDataIn, ErxCommunication, ErxComReplyBuilder> {

  /**
   * @param data for the input of the fhir resource builder
   * @return
   */
  @Override
  public ErxComReplyBuilder toFhir(CommunicationEDataIn data) {
    val type = data.getType();
    if (type != CommunicationEType.REPLY) {
      throw new ImpossibleOperationError("Cannot send any other communication type than REPLY");
    }

    val message = createMessageFrom(data.getMessage());
    val builder = ErxCommunicationBuilder.asReply(message);

    val bpid =
        ofNullable(data.getReference())
            .flatMap(r -> ofNullable(r.getPrescriptionId()))
            .orElseGet(() -> PrescriptionId.random().getValueAsString());
    val bac =
        ofNullable(data.getReference())
            .flatMap(r -> ofNullable(r.getAccessCode()))
            .orElseGet(() -> AccessCode.random().getValueAsString());
    builder.basedOn(bpid, bac);

    ofNullable(data.getRecipient())
        .ifPresentOrElse(builder::receiver, () -> builder.receiver(KVNR.randomStringValue()));
    ofNullable(data.getSender())
        .ifPresentOrElse(
            builder::sender, () -> builder.sender(TelematikID.random().getValueAsString()));

    ofNullable(data.getOrderId()).ifPresent(builder::orderId);
    return builder;
  }

  private CommunicationReplyMessage createMessageFrom(@Nullable CommunicationEPayload payload) {
    payload = ofNullable(payload).orElseGet(() -> CommunicationEPayload.builder().build());
    int version = ofNullable(payload.getVersion()).orElse(3);

    if (version == 3) {
      return createMessageForVersion3(payload);
    } else if (version == 1) {
      return createMessageForVersion1(payload);
    } else {
      throw new ImpossibleOperationError("Unsupported communication version: " + version);
    }
  }

  private CommunicationReplyMessage createMessageForVersion3(CommunicationEPayload payload) {
    val builder = CommunicationReplyMessage.forV3();
    val payloadType = ofNullable(payload.getCommunicationType());

    payloadType
        .map(it -> CommunicationPayloadType.fromString(it.getDisplay()))
        .map(CommunicationPayloadType::getLabel)
        .ifPresent(builder::communicationType);

    ofNullable(payload.getTransactionID()).ifPresent(builder::transactionID);
    ofNullable(payload.getText())
        .ifPresentOrElse(
            builder::text,
            () -> {
              if (payloadType.filter(it -> it == CommunicationPayloadEType.TEXT).isPresent()) {
                builder.text(GemFaker.fakerCommunicationReplyMessage());
              }
            });
    ofNullable(payload.getUrl()).ifPresent(builder::url);
    ofNullable(payload.getPickupCodeHR()).ifPresent(builder::pickupCodeHR);
    ofNullable(payload.getPickupCodeDMC()).ifPresent(builder::pickupCodeDMC);
    ofNullable(payload.getReadyForCollection())
        .map(CollectionReadyEType::getDisplay)
        .ifPresent(builder::readyForCollecting);
    ofNullable(payload.getDeliveryStatus())
        .map(DeliverStatusEType::getDisplay)
        .ifPresent(builder::deliveryStatus);
    ofNullable(payload.getTotalAmount()).ifPresent(builder::totalAmount);
    ofNullable(payload.getInTransportPosition())
        .map(it -> new InTransportPosition(it.getLat(), it.getLon()))
        .ifPresent(builder::inTransportPosition);
    ofNullable(payload.getInTransportETA())
        .map(it -> new CommunicationReplyMessage.InTransportETA(it.getFrom(), it.getTo()))
        .ifPresent(builder::inTransportETA);

    val pms =
        ofNullable(payload.getPaymentMethods()).orElse(List.of()).stream()
            .map(pm -> new PaymentMethod(pm.getMethod().getDisplay(), null, pm.getUrl()))
            .toList();
    builder.paymentMethods(pms);

    return builder.build();
  }

  private CommunicationReplyMessage createMessageForVersion1(CommunicationEPayload payload) {
    val builder = CommunicationReplyMessage.forV1();

    ofNullable(payload.getText()).ifPresent(builder::infoText);
    ofNullable(payload.getPickupCodeHR()).ifPresent(builder::pickUpCodeHR);
    ofNullable(payload.getPickupCodeDMC()).ifPresent(builder::pickUpCodeDMC);
    ofNullable(payload.getUrl()).ifPresent(builder::url);
    ofNullable(payload.getSupplyOptionsType())
        .map(it -> SupplyOptionsType.getSupplyOptionType(it.getDisplay()))
        .ifPresent(builder::supplyOptionsType);

    return builder.build();
  }

  /**
   * @return
   */
  @Override
  public Class<CommunicationEDataIn> jsonType() {
    return CommunicationEDataIn.class;
  }

  /**
   * @return
   */
  @Override
  public Class<ErxCommunication> fhirType() {
    return ErxCommunication.class;
  }
}
