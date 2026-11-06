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

interface KbvEOrganization {
    @get:Schema(
        title = "Organisationsname",
        type = "string",
        example = "Praxis Friedrichstraße",
    )
    val officeName: String?

    @get:Schema(
        title = "Betriebsstättennummer",
        type = "string",
        example = "687777700",
    )
    val bsnr: String?

    @get:Schema(
        title = "Telefonnummer",
        type = "string",
        example = "12365499",
    )
    val phone: String?

    @get:Schema(
        title = "Email-Adresse",
        type = "string",
        example = "praxis-friedrich@doctormail.de",
    )
    val email: String?

    @get:Schema(
        title = "Stadt",
        type = "string",
        example = "Altkölln",
    )
    val city: String?

    @get:Schema(
        title = "Postleitzahl",
        type = "string",
        example = "12345",
    )
    val postal: String?

    @get:Schema(
        title = "Straße",
        type = "string",
        example = "Herbert-Lewin-Platz 2",
    )
    val street: String?
}

data class KbvEOrganizationIn(
    override var officeName: String? = null,
    override var bsnr: String? = null,
    override var phone: String? = null,
    override var email: String? = null,
    override var city: String? = null,
    override var postal: String? = null,
    override var street: String? = null,
) : KbvEOrganization,
    ErpioneInputData

data class KbvEOrganizationOut(
    override var officeName: String,
    override var bsnr: String,
    override var phone: String,
    override var email: String,
    override var city: String,
    override var postal: String,
    override var street: String,
) : KbvEOrganization,
    ErpioneOutputData
