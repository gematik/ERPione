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

import de.gematik.bbriccs.fhir.de.value.IKNR
import de.gematik.erpione.data.r4.etype.InsuranceEType
import de.gematik.erpione.data.r4.etype.InsurantEStatus
import de.gematik.erpione.data.r4.etype.PayorEType
import de.gematik.erpione.data.r4.etype.PersonEGroup
import de.gematik.erpione.data.r4.etype.WopEType
import de.gematik.test.erezept.fhir.builder.GemFaker
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import kotlin.test.assertEquals

class ECoverageTest {

    @ParameterizedTest
    @EnumSource(value = InsuranceEType::class)
    fun shouldSerializeExample(type: InsuranceEType) {
        val om = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        val coverage = KbvECoverageIn().apply {
            this.type = type
            this.payorType = GemFaker.fakerValueSet(PayorEType::class.java)
            this.iknr = IKNR.randomStringValue()
            this.wop = GemFaker.fakerValueSet(WopEType::class.java)
            this.name = GemFaker.insuranceName()
            this.insurantState = GemFaker.fakerValueSet(InsurantEStatus::class.java)
            this.personGroup = GemFaker.fakerValueSet(PersonEGroup::class.java)
        }

        val json = assertDoesNotThrow { om.writeValueAsString(coverage) }
        val coverage2 = assertDoesNotThrow { om.readValue(json, KbvECoverageIn::class.java) }

        assertEquals(coverage, coverage2)
    }
}
