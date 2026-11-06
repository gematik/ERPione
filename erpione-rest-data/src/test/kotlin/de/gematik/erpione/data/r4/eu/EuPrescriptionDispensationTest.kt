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

import de.gematik.test.erezept.fhir.values.PrescriptionId
import org.junit.jupiter.api.assertDoesNotThrow
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals

class EuPrescriptionDispensationTest {

    @Test
    fun `should serialize minimal EU dispensation input list`() {
        val om = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        val dispensations =
            Stream.of(PrescriptionId.random().valueAsString, PrescriptionId.random().valueAsString, PrescriptionId.random().valueAsString)
                .map { it: String -> EuPrescriptionDispensationIn(it) }
                .toList()
        val body = EuPrescriptionDispensationListIn.of(dispensations)
        val json = assertDoesNotThrow { om.writeValueAsString(body) }

        val dispensation2 = assertDoesNotThrow { om.readValue(json, EuPrescriptionDispensationListIn::class.java) }
        assertEquals(dispensations.size, dispensation2.prescriptions.size)
    }
}
