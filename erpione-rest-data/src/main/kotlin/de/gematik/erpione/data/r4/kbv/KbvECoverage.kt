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
import de.gematik.erpione.data.r4.etype.InsuranceEType
import de.gematik.erpione.data.r4.etype.InsurantEStatus
import de.gematik.erpione.data.r4.etype.PayorEType
import de.gematik.erpione.data.r4.etype.PersonEGroup
import de.gematik.erpione.data.r4.etype.WopEType
import io.swagger.v3.oas.annotations.media.Schema

interface KbvECoverage {
    val type: InsuranceEType?
    val payorType: PayorEType?

    @get:Schema(
        title = "Institutskennzeichen-Nummer",
        type = "string",
        example = "108416214",
    )
    val iknr: String?

    @get:Schema(
        title = "Name der Versicherung",
        type = "string",
        example = "BIKK Bielefeld",
    )
    val name: String?
    val wop: WopEType?
    val insurantState: InsurantEStatus?
    val personGroup: PersonEGroup?
}

data class KbvECoverageIn(
    override var type: InsuranceEType? = null,
    override var payorType: PayorEType? = null,
    override var iknr: String? = null,
    override var name: String? = null,
    override var wop: WopEType? = null,
    override var insurantState: InsurantEStatus = InsurantEStatus.MEMBERS,
    override var personGroup: PersonEGroup = PersonEGroup.NOT_SET,
) : KbvECoverage,
    ErpioneInputData

data class KbvECoverageOut(
    override var type: InsuranceEType? = null,
    override var payorType: PayorEType? = null,
    override var iknr: String?,
    override var name: String,
    override var wop: WopEType?,
    override var insurantState: InsurantEStatus = InsurantEStatus.MEMBERS,
    override var personGroup: PersonEGroup = PersonEGroup.NOT_SET,
) : KbvECoverage,
    ErpioneOutputData
