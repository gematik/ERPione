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

package de.gematik.erpione.data.r4.etype

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SupplyEFormTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "TRO, TRO",
            "tropfen, TRO",
            "Gel, GEL",
            "Suppositorien, SUP",
        ],
    )
    fun `should match from string exactly on exact matches`(input: String, expected: SupplyEForm) {
        val supplyForm = SupplyEForm.fromString(input)
        assertEquals(expected, supplyForm)
    }

    @Test
    fun `should match from string fuzzy on no exact matches`() {
        val supplyForm = assertDoesNotThrow { SupplyEForm.fromString("Einnehmen") }
        assertTrue(supplyForm.display.contains("Einnehmen", ignoreCase = true))
    }
}
