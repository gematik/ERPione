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

import com.fasterxml.jackson.annotation.JsonFormat
import de.gematik.erpione.data.ErpioneData
import de.gematik.erpione.data.r4.etype.AccidentEType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

data class KbvAccidentEData(
    var type: AccidentEType? = AccidentEType.fromString("1"),

    @field:Schema(
        title = "Unfallbetrieb",
        type = "String",
        example = "gematik GmbH",
    )
    var organization: String? = null,

    @field:Schema(title = "Unfalltag", type = "date", example = "01.01.2000")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    var date: Date? = null,

) : ErpioneData
