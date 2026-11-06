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

package de.gematik.erpione.data.r4.kbv

import de.gematik.erpione.data.r4.HumanENameIn
import de.gematik.erpione.data.r4.etype.QualificationEType
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import kotlin.test.assertEquals

class EPractitionerTest {

    @ParameterizedTest
    @EnumSource(value = QualificationEType::class)
    fun shouldSerializeExample(type: QualificationEType) {
        val om = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        val practitioner = KbvEPractitionerIn().apply {
            this.name = HumanENameIn(prefix = "Dr. med.", given = "Ludger", family = "Königsstein")
            this.type = type
        }
        practitioner.addQualifications("Spezialarzt")

        val json = assertDoesNotThrow { om.writeValueAsString(practitioner) }
        val practitioner2 = assertDoesNotThrow { om.readValue(json, KbvEPractitionerIn::class.java) }

        assertEquals(practitioner, practitioner2)
    }
}
