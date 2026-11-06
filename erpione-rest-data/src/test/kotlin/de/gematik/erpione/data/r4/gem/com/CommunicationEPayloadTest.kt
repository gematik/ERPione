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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID

class CommunicationEPayloadTest {

    @Test
    fun `test CommunicationEPayload builder`() {
        val payloadBuilder = CommunicationEPayload.builder()
            .version(3)
            .type(CommunicationPayloadEType.DELIVERY_STATUS)
            .transactionId(UUID.randomUUID().toString())
            .text("Test message")
            .hint("This is a hint")
            .url("https://example.com")
            .readyForCollection(CollectionReadyEType.NA)
            .pickupCodeHR("HR123456")
            .pickupCodeDMC("DMC123456")
            .deliveryStatus(DeliverStatusEType.DELIVERED)
            .inTransportPosition(TransportEPosition(0.0, 0.0))
            .inTransportETA(TransportETime(0, 1000))
            .totalAmount(100)
            .paymentMethods(listOf(PaymentEMethod(PaymentMethodEType.CREDIT_CARD, "1234-5678-9012-3456")))
            .supplyOptionsType(SupplyOptionsEType.DELIVERY)
            .phone("+491234567890")
            .firstname("John")
            .lastname("Doe")
            .address("123 Main St")
            .postcode("12345")
            .city("Berlin")
            .country("Germany")

        assertDoesNotThrow { payloadBuilder.build() }
    }

    @Test
    fun `address accepts single string value in JSON`() {
        val payload = jacksonObjectMapper()
            .readValue("""{"address":"123 Main St"}""", CommunicationEPayload::class.java)
        assertEquals(listOf("123 Main St"), payload.address)
    }

    @Test
    fun `should serialize and deserialize in transport position properly`() {
        val om = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        val pos = TransportEPosition(52.5200, 13.4050)

        val json = assertDoesNotThrow { om.writeValueAsString(pos) }
        val pos2 = assertDoesNotThrow { om.readValue(json, TransportEPosition::class.java) }
        assertEquals(pos, pos2)
    }
}
