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

package de.gematik.erpione.client.rest

import arrow.core.Either
import de.gematik.erpione.client.ErpioneRestException
import de.gematik.erpione.data.error.ErrorData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull

class ErpioneResponseTest {

    @Test
    fun shouldGetExpectedPayload() {
        val body = Either.Right("Hello")
        val response = ErpioneResponse(200, body = body)

        val payload = assertDoesNotThrow { response.asExpectedPayload() }
        assertTrue(response.successful())
        assert(payload == "Hello")
    }

    @Test
    fun shouldThrowOnErrorData() {
        val body = Either.Left(ErrorData())
        val response = ErpioneResponse(400, body = body)
        assertFalse(response.successful())

        val error = assertThrows<ErpioneRestException> { response.asExpectedPayload() }
        assertNotNull(error)
    }
}
