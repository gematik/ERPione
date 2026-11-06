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

package de.gematik.erpione.data.r4

import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData
import io.swagger.v3.oas.annotations.media.Schema

interface HumanEName {
    @get:Schema(
        title = "Namenspräfix",
        type = "string",
        example = "Dr. med.",
    )
    val prefix: String?

    @get:Schema(
        title = "Vorname",
        type = "string",
        example = "Ludger",
    )
    val given: String?

    @get:Schema(
        title = "Nachname",
        type = "string",
        example = "Königsstein",
    )
    val family: String?

    fun asFullName(): String = listOfNotNull(prefix, given, family).joinToString(" ")
}

data class HumanENameIn(
    override var prefix: String? = null,
    override var given: String? = null,
    override var family: String? = null,
) : HumanEName,
    ErpioneInputData

data class HumanENameOut(
    override var prefix: String? = null,
    override var given: String,
    override var family: String,
) : HumanEName,
    ErpioneOutputData
