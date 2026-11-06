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

package de.gematik.erpione.data.meta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue

class TIHealthCheckResultTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "200, OK",
            "201, OK",
            "202, OK",
            "204, OK",
            "400, ERROR",
            "401, ERROR",
            "403, ERROR",
            "404, ERROR",
            "500, ERROR",
            "502, ERROR",
            "503, ERROR",
        ],
    )
    fun `should return correct TIHealthCheckStatus from HTTP Status Code`(statusCode: Int, expectedStatus: TIHealthCheckStatus) {
        val actualStatus = TIHealthCheckStatus.fromHttpStatusCode(statusCode)
        assertEquals(expectedStatus, actualStatus)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "OK, OK",
            "Ok, OK",
            "ok, OK",
            "ERROR, ERROR",
            "Error, ERROR",
            "error, ERROR",
        ],
    )
    fun `should return the correct TIHealthCheckStatus from a string`(status: String, expectedStatus: TIHealthCheckStatus) {
        val actualStatus = TIHealthCheckStatus.fromString(status)
        assertEquals(expectedStatus, actualStatus)
    }

    @Test
    fun `should serialize and deserialize TIHealthCheckResult correctly`() {
        val om: ObjectMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

        val originalResult = TIHealthCheckResult(
            status = TIHealthCheckStatus.OK,
            statusCode = 200,
            latency = 75L,
        )

        // Serialize to JSON
        val json = om.writeValueAsString(originalResult)

        // Deserialize from JSON
        val deserializedResult = om.readValue<TIHealthCheckResult>(json)

        assertEquals(originalResult, deserializedResult)
    }
}
