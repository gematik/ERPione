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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import de.gematik.erpione.data.r4.PhysicianENumberIn
import de.gematik.erpione.data.r4.kbv.KbvEPractitionerIn
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals

internal class PractitionerDataTest {
    @Test
    @Throws(JsonProcessingException::class)
    fun shouldCreateDoctorData() {
        val om = ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

        val dto = KbvEPractitionerIn().apply {
            anr = PhysicianENumberIn(type = "LANR", value = "123123123")
            addQualifications("ASV-Fachgruppennummer: 555555009")
        }

        val json = assertDoesNotThrow<String?> { om.writeValueAsString(dto) }
        val dto2 = om.readValue(json, KbvEPractitionerIn::class.java)
        assertEquals(dto, dto2)
    }
}
