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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import de.gematik.erpione.data.ErpioneValueSet

/**
 * corresponding to MedicationCategory FHIR-ValueSet [ ]
 */
enum class MedicationECategory(
    override val code: String,
    @JsonValue
    override val display: String,
) : ErpioneValueSet<MedicationECategory> {
    C_00("00", "Arzneimittel"),
    C_01("01", "BtM"),
    C_02("02", "AMVV § 3a Abs. 1 (Thalidomid o. ä.)"),
    C_03("03", "Sonstiges"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): MedicationECategory = ErpioneValueSet.fromStringOrThrow(MedicationECategory::class.java, input)
    }
}
