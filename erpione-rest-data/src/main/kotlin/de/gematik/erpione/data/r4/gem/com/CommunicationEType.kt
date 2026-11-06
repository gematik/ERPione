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

package de.gematik.erpione.data.r4.gem.com

import com.fasterxml.jackson.annotation.JsonValue
import de.gematik.erpione.data.ErpioneValueSet

enum class CommunicationEType(
    @JsonValue override val code: String,
    override val display: String,
) : ErpioneValueSet<CommunicationEType> {
    INFO_REQ("INFO_REQ", "Informationsanfrage"),
    DISP_REQ("DISP_REQ", "Abgabeanfrage"),
    REPLY("REPLY", "Antwort"),
    DIGA("DIGA", "Digitale Gesundheitsanwendung"),
    CHANGE_REQ("CHANGE_REQ", "Änderungsanfrage"),
    CHANGE_REPLY("CHANGE_REPLY", "Änderungsantwort"),
    REPRESENTATIVE("REPRESENTATIVE", "Vertreter"),

    UNKNOWN("UNKNOWN", "Unbekannt"),
    ;

    companion object {
        @JvmStatic
        fun fromString(input: String?): CommunicationEType = ErpioneValueSet.fromString(CommunicationEType::class.java, input)
            ?: UNKNOWN
    }
}
