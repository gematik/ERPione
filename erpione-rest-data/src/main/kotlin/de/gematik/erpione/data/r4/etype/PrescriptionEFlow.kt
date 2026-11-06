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

enum class PrescriptionEFlow(
    @JsonValue override val code: String,
    override val display: String,
) : ErpioneValueSet<PrescriptionEFlow> {
    FLOW_TYPE_160("160", "Apothekenpflichtige Arzneimittel"),
    FLOW_TYPE_162("162", "Digitale Gesundheitsanwendungen"),

//    FLOW_TYPE_165("165", "Betäubungsmittel"),
    FLOW_TYPE_166("166", "Arzneimittel nach § 3a AMVV"),
    FLOW_TYPE_169("169", "Workflow-Steuerung durch Leistungserbringer"),
    FLOW_TYPE_200("200", "Apothekenpflichtige Arzneimittel (PKV)"),
    FLOW_TYPE_209("209", "Workflow-Steuerung durch Leistungserbringer (PKV)"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): PrescriptionEFlow = ErpioneValueSet.fromStringOrThrow(PrescriptionEFlow::class.java, input)

        @JvmStatic
        fun matches(input: String?): Boolean = ErpioneValueSet.fromString(PrescriptionEFlow::class.java, input) != null
    }
}
