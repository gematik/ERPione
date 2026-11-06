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

enum class QualificationEType(
    override val code: String,
    @JsonValue
    override val display: String,
) : ErpioneValueSet<QualificationEType> {
    DOCTOR("00", "Arzt"),
    DENTIST("01", "Zahnarzt"),
    MIDWIFE("02", "Hebamme"),
    DOCTOR_IN_TRAINING("03", "Arzt in Weiterbildung"),
    DOCTOR_AS_REPLACEMENT("04", "Arzt als Vertreter"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): QualificationEType = ErpioneValueSet.fromStringOrThrow(QualificationEType::class.java, input)
    }
}
