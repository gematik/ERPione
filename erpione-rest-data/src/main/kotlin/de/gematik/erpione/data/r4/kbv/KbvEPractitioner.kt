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
import de.gematik.erpione.data.r4.HumanEName
import de.gematik.erpione.data.r4.HumanENameIn
import de.gematik.erpione.data.r4.HumanENameOut
import de.gematik.erpione.data.r4.PhysicianENumber
import de.gematik.erpione.data.r4.PhysicianENumberIn
import de.gematik.erpione.data.r4.PhysicianENumberOut
import de.gematik.erpione.data.r4.etype.QualificationEType
import io.swagger.v3.oas.annotations.media.Schema

interface KbvEPractitioner<NAME : HumanEName, PEN : PhysicianENumber> {
    @get:Schema(
        title = "Name",
        description = "Name der Leistungserbringerin bzw. des Leistungserbringers (Präfix, Vor- und Nachname)",
    )
    val name: NAME?
    val type: QualificationEType?
    val anr: PEN?

    @get:Schema(
        title = "Liste von Qualifikationen",
        examples = ["FA Innere Medizin und Kardiologie"],
    )
    val qualifications: MutableList<String>
}

interface KbvEPractitionerRole {
    @get:Schema(
        title = "ASV-Nummer",
        type = "string",
        example = "12345678",
    )
    val asvnr: String?
}

data class KbvEPractitionerIn(
    override var name: HumanENameIn? = null,
    override var type: QualificationEType? = QualificationEType.DOCTOR,
    override var anr: PhysicianENumberIn? = null,
    override var qualifications: MutableList<String> = mutableListOf(),
) : KbvEPractitioner<HumanENameIn, PhysicianENumberIn>,
    ErpioneInputData {

    fun addQualifications(vararg input: String) {
        qualifications.addAll(input)
    }
}

data class KbvEPractitionerOut(
    override var name: HumanENameOut,
    override var type: QualificationEType,
    override var anr: PhysicianENumberOut?,
    override var qualifications: MutableList<String>,
) : KbvEPractitioner<HumanENameOut, PhysicianENumberOut>,
    ErpioneOutputData

data class KbvEPractitionerRoleIn(
    override val asvnr: String? = null,
) : KbvEPractitionerRole,
    ErpioneInputData

data class KbvEPractitionerRoleOut(
    override val asvnr: String,
) : KbvEPractitionerRole,
    ErpioneOutputData
