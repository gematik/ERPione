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
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

data class KbvMvoEData(
    @field:Schema(
        title = "MVO ID",
        type = "string",
        example = "urn:uuid:24e2e10d-e962-4d1c-be4f-8760e690a5f0",
    )
    var mvoId: String? = null,

    @field:Schema(
        title = "Numerator",
        type = "int",
        example = "1",
    )
    var numerator: Int? = null,

    @field:Schema(
        title = "Denominator",
        type = "int",
        example = "4",
    )
    var denominator: Int? = null,

    @field:Schema(title = "gültig ab", type = "date", example = "22.06.2000")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    var startDate: Date = Date(),

    @field:Schema(title = "gültig bis", type = "date", example = "22.06.2035")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    var endDate: Date = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.time,
) : ErpioneData
