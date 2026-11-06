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

import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData
import io.swagger.v3.oas.annotations.media.Schema

interface KbvEHealthAppRequest {
    @get:Schema(
        title = "Pharmazentralnummer EVDGA",
        type = "string",
        example = "17169789",
    )
    val pzn: String?

    @get:Schema(
        title = "EVDGA-Name",
        type = "string",
        example = "Migräne-Adee Entspannungsübungen",
    )
    val name: String?

    @get:Schema(
        title = "Recht auf Soziale Entschädigung (SER)",
        type = "boolean",
        example = "false",
    )
    val ser: Boolean?
}

data class KbvEHealthAppRequestIn(
    override var pzn: String? = null,
    override var name: String? = null,
    override var ser: Boolean = false,
) : KbvEHealthAppRequest,
    ErpioneInputData

data class KbvEHealthAppRequestOut(
    override var pzn: String,
    override var name: String,
    override var ser: Boolean,
) : KbvEHealthAppRequest,
    ErpioneOutputData
