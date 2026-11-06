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

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class CommunicationPayloadETypeTest {

    @ParameterizedTest
    @EnumSource(CommunicationPayloadEType::class)
    fun `test CommunicationPayloadEType enum values`(type: CommunicationPayloadEType) {
        val t1 = assertDoesNotThrow { CommunicationPayloadEType.fromString(type.code) }
        val t2 = assertDoesNotThrow { CommunicationPayloadEType.fromString(type.display) }
        val t3 = assertDoesNotThrow { CommunicationPayloadEType.fromString(type.name) }

        assertEquals(type, t1)
        assertEquals(type, t2)
        assertEquals(type, t3)
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid", "information", "helloworld"])
    @NullSource
    @EmptySource
    fun `test CommunicationPayloadEType fromString with invalid input`(input: String?) {
        assertThrows(IllegalArgumentException::class.java) {
            CommunicationPayloadEType.fromString(input)
        }
    }

    @ParameterizedTest
    @EnumSource(CollectionReadyEType::class)
    fun `test CollectionReadyEType enum values`(type: CollectionReadyEType) {
        val t1 = assertDoesNotThrow { CollectionReadyEType.fromString(type.code) }
        val t2 = assertDoesNotThrow { CollectionReadyEType.fromString(type.display) }
        val t3 = assertDoesNotThrow { CollectionReadyEType.fromString(type.name) }

        assertEquals(type, t1)
        assertEquals(type, t2)
        assertEquals(type, t3)
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid", "info", "helloworld"])
    @NullSource
    @EmptySource
    fun `test CollectionReadyEType fromString with invalid input`(input: String?) {
        assertThrows(IllegalArgumentException::class.java) {
            CollectionReadyEType.fromString(input)
        }
    }

    @ParameterizedTest
    @EnumSource(DeliverStatusEType::class)
    fun `test DeliverStatusEType enum values`(type: DeliverStatusEType) {
        val t1 = assertDoesNotThrow { DeliverStatusEType.fromString(type.code) }
        val t2 = assertDoesNotThrow { DeliverStatusEType.fromString(type.display) }
        val t3 = assertDoesNotThrow { DeliverStatusEType.fromString(type.name) }

        assertEquals(type, t1)
        assertEquals(type, t2)
        assertEquals(type, t3)
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid", "info", "helloworld"])
    @NullSource
    @EmptySource
    fun `test DeliverStatusEType fromString with invalid input`(input: String?) {
        assertThrows(IllegalArgumentException::class.java) {
            DeliverStatusEType.fromString(input)
        }
    }

    @ParameterizedTest
    @EnumSource(PaymentMethodEType::class)
    fun `test PaymentMethodEType enum values`(type: PaymentMethodEType) {
        val t1 = assertDoesNotThrow { PaymentMethodEType.fromString(type.code) }
        val t2 = assertDoesNotThrow { PaymentMethodEType.fromString(type.display) }
        val t3 = assertDoesNotThrow { PaymentMethodEType.fromString(type.name) }

        assertEquals(type, t1)
        assertEquals(type, t2)
        assertEquals(type, t3)
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid", "info", "helloworld"])
    @NullSource
    @EmptySource
    fun `test PaymentMethodEType fromString with invalid input`(input: String?) {
        assertThrows(IllegalArgumentException::class.java) {
            PaymentMethodEType.fromString(input)
        }
    }

    @ParameterizedTest
    @EnumSource(SupplyOptionsEType::class)
    fun `test SupplyOptionsEType enum values`(type: SupplyOptionsEType) {
        val t1 = assertDoesNotThrow { SupplyOptionsEType.fromString(type.code) }
        val t2 = assertDoesNotThrow { SupplyOptionsEType.fromString(type.display) }
        val t3 = assertDoesNotThrow { SupplyOptionsEType.fromString(type.name) }

        assertEquals(type, t1)
        assertEquals(type, t2)
        assertEquals(type, t3)
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid", "info", "helloworld"])
    @NullSource
    @EmptySource
    fun `test SupplyOptionsEType fromString with invalid input`(input: String?) {
        assertThrows(IllegalArgumentException::class.java) {
            SupplyOptionsEType.fromString(input)
        }
    }
}
