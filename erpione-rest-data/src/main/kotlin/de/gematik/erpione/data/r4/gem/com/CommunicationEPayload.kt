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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class CommunicationEPayload(
    val version: Int? = 3,
    val communicationType: CommunicationPayloadEType? = CommunicationPayloadEType.TEXT,
    val transactionID: String? = UUID.randomUUID().toString(),
    @param:JsonProperty("text")
    @param:JsonAlias("info_text")
    @get:JsonProperty("text")
    val text: String? = null,
    val hint: String? = null,
    val url: String? = null,
    val readyForCollection: CollectionReadyEType? = null,
    @param:JsonProperty("pickupCodeHR")
    @param:JsonAlias("pickUpCodeHR")
    @get:JsonProperty("pickupCodeHR")
    val pickupCodeHR: String? = null,
    @param:JsonProperty("pickupCodeDMC")
    @param:JsonAlias("pickUpCodeDMC")
    @get:JsonProperty("pickupCodeDMC")
    val pickupCodeDMC: String? = null,
    val deliveryStatus: DeliverStatusEType? = null,
    val inTransportPosition: TransportEPosition? = null,
    val inTransportETA: TransportETime? = null,
    val totalAmount: Int? = null,
    val paymentMethods: List<PaymentEMethod>? = listOf(),
    val supplyOptionsType: SupplyOptionsEType? = null,
    val phone: String? = null,
    val firstname: String? = null,
    @param:JsonProperty("lastname")
    @param:JsonAlias("name")
    @get:JsonProperty("lastname")
    val lastname: String? = null,

    // this is required because in v1 address is a list, while in v3 it is a single string.
    // This allows for backward compatibility with v1 payloads.
    @field:JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val address: List<String> = listOf(),
    val postcode: String? = null,
    val city: String? = null,
    val country: String? = null,
) {
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var version: Int? = 3
        private var type: CommunicationPayloadEType? = CommunicationPayloadEType.TEXT
        private var transactionId: String? = UUID.randomUUID().toString()
        private var text: String? = null
        private var hint: String? = null
        private var url: String? = null
        private var readyForCollection: CollectionReadyEType? = null
        private var pickupCodeHR: String? = null
        private var pickupCodeDMC: String? = null
        private var deliveryStatus: DeliverStatusEType? = null
        private var inTransportPosition: TransportEPosition? = null
        private var inTransportETA: TransportETime? = null
        private var totalAmount: Int? = null
        private var paymentMethods: List<PaymentEMethod>? = listOf()
        private var supplyOptionsType: SupplyOptionsEType? = null
        private var phone: String? = null
        private var firstname: String? = null
        private var lastname: String? = null
        private var address: MutableList<String> = mutableListOf()
        private var postcode: String? = null
        private var city: String? = null
        private var country: String? = null

        fun version(version: Int?) = apply { this.version = version }

        fun type(type: CommunicationPayloadEType?) = apply { this.type = type }

        fun transactionId(transactionId: String?) = apply { this.transactionId = transactionId }

        fun text(text: String?) = apply { this.text = text }

        fun hint(hint: String?) = apply { this.hint = hint }

        fun url(url: String?) = apply { this.url = url }

        fun readyForCollection(readyForCollection: CollectionReadyEType?) = apply {
            this.readyForCollection = readyForCollection
        }

        fun pickupCodeHR(pickupCodeHR: String?) = apply { this.pickupCodeHR = pickupCodeHR }

        fun pickupCodeDMC(pickupCodeDMC: String?) = apply { this.pickupCodeDMC = pickupCodeDMC }

        fun deliveryStatus(deliveryStatus: DeliverStatusEType?) = apply {
            this.deliveryStatus = deliveryStatus
        }

        fun inTransportPosition(inTransportPosition: TransportEPosition?) = apply {
            this.inTransportPosition = inTransportPosition
        }

        fun inTransportETA(inTransportETA: TransportETime?) = apply {
            this.inTransportETA = inTransportETA
        }

        fun totalAmount(totalAmount: Int?) = apply { this.totalAmount = totalAmount }

        fun paymentMethods(paymentMethods: List<PaymentEMethod>?) = apply {
            this.paymentMethods = paymentMethods
        }

        fun supplyOptionsType(supplyOptionsType: SupplyOptionsEType?) = apply {
            this.supplyOptionsType = supplyOptionsType
        }

        fun phone(phone: String?) = apply { this.phone = phone }

        fun firstname(firstname: String?) = apply { this.firstname = firstname }

        fun lastname(lastname: String?) = apply { this.lastname = lastname }

        fun address(address: String?) = apply {
            if (address != null) {
                this.address.add(address)
            }
        }

        fun postcode(postcode: String?) = apply { this.postcode = postcode }

        fun city(city: String?) = apply { this.city = city }

        fun country(country: String?) = apply { this.country = country }

        fun build() = CommunicationEPayload(
            version = version ?: 3,
            communicationType = type,
            transactionID = transactionId,
            text = text,
            hint = hint,
            url = url,
            readyForCollection = readyForCollection,
            pickupCodeHR = pickupCodeHR,
            pickupCodeDMC = pickupCodeDMC,
            deliveryStatus = deliveryStatus,
            inTransportPosition = inTransportPosition,
            inTransportETA = inTransportETA,
            totalAmount = totalAmount,
            paymentMethods = paymentMethods,
            supplyOptionsType = supplyOptionsType,
            phone = phone,
            firstname = firstname,
            lastname = lastname,
            address = address,
            postcode = postcode,
            city = city,
            country = country,
        )
    }
}

data class TransportEPosition(
    val lat: Double,
    @param:JsonProperty("long")
    @param:JsonAlias("lon") // lon is wrong, but we need to support it as long the FD is not fixed
    @get:JsonProperty("long")
    val lon: Double,
)
data class TransportETime(val from: Long, val to: Long)
data class PaymentEMethod(val method: PaymentMethodEType, val url: String? = null)
