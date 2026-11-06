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
 * corresponding to WOP FHIR-ValueSet [de.gematik.test.erezept.fhir.valuesets.MedicationType]
 */
enum class MedicationEType(@JsonValue override val display: String) : ErpioneValueSet<MedicationEType> {
    PZN("PZN"),
    INGREDIENT("Wirkstoff"),
    FREETEXT("Freitext"),
    COMPOUNDING("Rezeptur"),
    ;

    override val code: String = this.name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): MedicationEType = ErpioneValueSet.fromString(MedicationEType::class.java, input)
            ?: PZN

        @JvmStatic
        fun matches(input: String?): Boolean = ErpioneValueSet.fromString(MedicationEType::class.java, input) != null
    }
}
