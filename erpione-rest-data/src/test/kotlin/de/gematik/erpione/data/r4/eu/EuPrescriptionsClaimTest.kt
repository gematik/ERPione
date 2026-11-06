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

package de.gematik.erpione.data.r4.eu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import kotlin.test.Test

class EuPrescriptionsClaimTest {

    @Test
    fun `should serialize EU prescription claims`() {
        val om = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        val claims = EuPrescriptionsClaim.with("200.000.003.587.370.14", "160.000.114.583.349.65")

        val json = assertDoesNotThrow { om.writeValueAsString(claims) }
        val claims2 = assertDoesNotThrow { om.readValue(json, EuPrescriptionsClaim::class.java) }

        assertEquals(claims.claims.size, claims2.claims.size)
    }
}
