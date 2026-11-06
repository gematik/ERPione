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
import io.swagger.v3.oas.annotations.media.Schema

interface PhysicianENumber {
    @get:Schema(
        title = "Art der ANR",
        allowableValues = ["LANR", "ZANR"],
        type = "string",
        example = "LANR",
    )
    val type: String?

    @get:Schema(
        title = "Arzt-/Zahnärztekammernummer",
        description = "Muss neunstellig numerisch sein",
        type = "string",
        minimum = "9",
        maximum = "9",
        example = "456456534",
    )
    val value: String?
}

data class PhysicianENumberIn(
    override var type: String? = null,
    override var value: String? = null,
) : PhysicianENumber,
    ErpioneInputData

data class PhysicianENumberOut(
    override var type: String,
    override var value: String,
) : PhysicianENumber,
    ErpioneInputData
