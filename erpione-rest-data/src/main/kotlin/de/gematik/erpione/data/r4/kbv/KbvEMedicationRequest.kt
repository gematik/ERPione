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

interface KbvEMedicationRequest {
    @get:Schema(
        title = "Dosieranweisung",
        type = "string",
        example = "bis zu 4mal täglich je 20-40 Tropfen",
    )
    val dosage: String?

    @get:Schema(
        title = "Menge",
        type = "int",
        example = "2",
    )
    val packageQuantity: Int?

    @get:Schema(
        title = "Notiz",
        type = "string",
        example = "Patient erneut auf Anwendung der Schmelztabletten hinweisen",
    )
    val note: String?

    @get:Schema(
        title = "EMP-Identifier",
        type = "string",
        example = "fe8f1a1b-0e7e-4a3e-90d7-5fc0222cb9af",
    )
    val empIdentifier: String?

    @get:Schema(
        title = "Substitution erlaubt",
        type = "boolean",
        example = "true",
    )
    val substitutionAllowed: Boolean?

    @get:Schema(
        title = "Bundesversorgungsgesetz",
        type = "boolean",
        example = "true",
        description = (
            "Patient ist nach dem Bundesversorgungsgesetz/Bundesentschädigungsgesetz anspruchsberechtigt"
            ),
    )
    val bvg: Boolean?

    @get:Schema(
        title = "Notfallgebühren",
        type = "boolean",
        example = "false",
    )
    val emergencyFee: Boolean?
    val mvo: KbvMvoEData?
    val accident: KbvAccidentEData?
}

data class KbvEMedicationRequestIn(
    override var dosage: String? = null,
    override var packageQuantity: Int? = null,
    override var note: String? = null,
    override var empIdentifier: String? = null,
    override var substitutionAllowed: Boolean? = true,
    override var bvg: Boolean? = false,
    override var emergencyFee: Boolean? = false,
    override var mvo: KbvMvoEData? = null,
    override var accident: KbvAccidentEData? = null,
) : KbvEMedicationRequest,
    ErpioneInputData

data class KbvEMedicationRequestOut(
    override var dosage: String?,
    override var packageQuantity: Int,
    override var note: String? = null,
    override var empIdentifier: String? = null,
    override var substitutionAllowed: Boolean = true,
    override var bvg: Boolean = false,
    override var emergencyFee: Boolean = false,
    override var mvo: KbvMvoEData? = null,
    override var accident: KbvAccidentEData? = null,
) : KbvEMedicationRequest,
    ErpioneOutputData
