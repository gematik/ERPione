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
import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData
import de.gematik.erpione.data.r4.HumanEName
import de.gematik.erpione.data.r4.HumanENameIn
import de.gematik.erpione.data.r4.HumanENameOut
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date

interface KbvEPatient<NAME : HumanEName> {
    @get:Schema(
        title = "KVNR",
        type = "string",
        example = "K220635158",
    )
    val kvnr: String?

    @get:Schema(
        title = "Name",
        description = "Name der Versicherten Person (Präfix, Vor- und Nachname)",
    )
    val name: NAME?

    @get:Schema(title = "Geburtsdatum", type = "date", example = "23.06.1935")
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    val birthDate: Date?

    @get:Schema(
        title = "Stadt",
        type = "string",
        example = "Esens",
    )
    val city: String?

    @get:Schema(
        title = "Postleitzahl",
        type = "string",
        example = "26427",
    )
    val postal: String?

    @get:Schema(
        title = "Straße",
        type = "string",
        example = "Blumenweg 7a",
    )
    val street: String?
}

data class KbvEPatientIn(
    override var kvnr: String? = null,
    override var name: HumanENameIn? = null,
    override var birthDate: Date? = null,
    override var city: String? = null,
    override var postal: String? = null,
    override var street: String? = null,
) : KbvEPatient<HumanENameIn>,
    ErpioneInputData {

    companion object Companion {
        @JvmStatic
        fun withKvnr(kvnr: String): KbvEPatientIn = KbvEPatientIn(kvnr = kvnr)
    }
}

data class KbvEPatientOut(
    override var kvnr: String,
    override var name: HumanENameOut,
    override var birthDate: Date,
    override var city: String,
    override var postal: String,
    override var street: String,
) : KbvEPatient<HumanENameOut>,
    ErpioneOutputData
