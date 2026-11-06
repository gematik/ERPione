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

package de.gematik.erpione.data.r4.gem.com

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import de.gematik.erpione.data.ErpioneValueSet

enum class CommunicationPayloadEType(@JsonValue override val display: String) : ErpioneValueSet<CommunicationPayloadEType> {
    TEXT("text"),
    LINK("link"),
    PICKUP_CODE_HR("pickupCodeHR"),
    PICKUP_CODE_DMC("pickupCodeDMC"),
    PAYMENT_INFO("paymentInfo"),
    DELIVERY_STATUS("deliveryStatus"),
    RESERVATION_STATUS("reservationStatus"),
    ORDER("order"),
    ;

    override val code: String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): CommunicationPayloadEType = ErpioneValueSet.fromString(CommunicationPayloadEType::class.java, input)
            ?: throw IllegalArgumentException("Unknown CommunicationPayloadEType: $input")
    }
}

enum class CollectionReadyEType(@JsonValue override val display: String) : ErpioneValueSet<CollectionReadyEType> {
    IMMEDIATELY("immediately"),
    SAME_DAY("sameDay"),
    NEXT_DAY("nextDay"),
    NEXT_DAY_AM("nextDayAM"),
    NEXT_DAY_PM("nextDayPM"),
    UNKNOWN("unknown"),
    NA("notAvailable"),
    ;

    override val code: String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): CollectionReadyEType = ErpioneValueSet.fromString(CollectionReadyEType::class.java, input)
            ?: throw IllegalArgumentException("Unknown CommunicationCollectionEType: $input")
    }
}

enum class DeliverStatusEType(@JsonValue override val display: String) : ErpioneValueSet<DeliverStatusEType> {
    PREPARED_WAITING("preparedWaiting"),
    IN_TRANSPORT("inTransport"),
    DELIVERED("delivered"),
    INCIDENT("incident"),
    ;

    override val code: String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): DeliverStatusEType = ErpioneValueSet.fromString(DeliverStatusEType::class.java, input)
            ?: throw IllegalArgumentException("Unknown CommunicationDeliveryStatusEType: $input")
    }
}

enum class PaymentMethodEType(@JsonValue override val display: String) : ErpioneValueSet<PaymentMethodEType> {
    CASH("cash"),
    BANK_ACCOUNT("bankaccount"),
    CREDIT_CARD("creditcard"),
    PAYPAL("paypal"),
    ;

    override val code: String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): PaymentMethodEType = ErpioneValueSet.fromString(PaymentMethodEType::class.java, input)
            ?: throw IllegalArgumentException("Unknown CommunicationPaymentMethodEType: $input")
    }
}

enum class SupplyOptionsEType(@JsonValue override val display: String) : ErpioneValueSet<SupplyOptionsEType> {
    ON_PREMISE("onPremise"),
    SHIPMENT("shipment"),
    DELIVERY("delivery"),
    ;

    override val code: String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): SupplyOptionsEType = ErpioneValueSet.fromString(SupplyOptionsEType::class.java, input)
            ?: throw IllegalArgumentException("Unknown CommunicationSupplyOptionsEType: $input")
    }
}
