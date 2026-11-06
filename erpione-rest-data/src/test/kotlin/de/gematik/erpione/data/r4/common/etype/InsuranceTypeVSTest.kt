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

package de.gematik.erpione.data.r4.common.etype

import de.gematik.bbriccs.fhir.de.valueset.InsuranceTypeDe
import de.gematik.erpione.data.exception.InvalidCodeValueException
import de.gematik.erpione.data.r4.etype.InsuranceEType
import de.gematik.erpione.data.r4.etype.InsuranceEType.Companion.fromString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class InsuranceTypeVSTest {
    @Test
    fun shouldBuildFrom() {
        val value1 = fromString("GKV")
        val value2 = fromString("gkv")
        Assertions.assertEquals(InsuranceEType.GKV, value1)
        Assertions.assertEquals(value1, value2)
    }

    @Test
    fun shouldThrowOnInvalidCode() {
        Assertions.assertThrows(
            InvalidCodeValueException::class.java,
        ) { fromString("abc") }
    }

    @Test
    fun shouldHaveAllInsuranceTypes() {
        Arrays.stream(InsuranceTypeDe.entries.toTypedArray())
            .forEach { vs: InsuranceTypeDe? ->
                val dto = Assertions.assertDoesNotThrow<InsuranceEType> { fromString(vs!!.code) }
                Assertions.assertEquals(vs!!.code, dto.code)
                Assertions.assertEquals(vs.display, dto.display)
            }
    }

    @Test
    fun shouldNotHaveAnyExtraInsuranceTypes() {
        Arrays.stream(InsuranceEType.entries.toTypedArray())
            .forEach { dto: InsuranceEType? ->
                val vs = Assertions.assertDoesNotThrow<InsuranceTypeDe> {
                    InsuranceTypeDe.fromCode(
                        dto!!.code,
                    )
                }
                Assertions.assertEquals(vs.code, dto!!.code)
                Assertions.assertEquals(vs.display, dto.display)
            }
    }
}
