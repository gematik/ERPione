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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MedicationECategoryTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "00, C_00",
            "Arzneimittel, C_00",
            "arzneimittel, C_00",
            "01, C_01",
            "BtM, C_01",
            "02, C_02",
            "AMVV § 3a Abs. 1 (Thalidomid o. ä.), C_02",
            "Thalidomid, C_02",
            "AMVV § 3a Abs. 1, C_02",
            "03, C_03",
            "sonstiges, C_03",
        ],
    )
    fun `should match all enum values`(input: String, category: MedicationECategory) {
        val fromString = MedicationECategory.fromString(input)
        assertEquals(fromString, category)
    }
}
