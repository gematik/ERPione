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

import de.gematik.erpione.data.ErpioneData
import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData
import de.gematik.erpione.data.r4.etype.MedicationECategory
import de.gematik.erpione.data.r4.etype.MedicationEType
import de.gematik.erpione.data.r4.etype.StandardESize
import de.gematik.erpione.data.r4.etype.SupplyEForm
import io.swagger.v3.oas.annotations.media.Schema

open class EMedication :
    ErpioneData,
    ErpioneInputData,
    ErpioneOutputData {

    var type = MedicationEType.PZN
    var category: MedicationECategory? = null

    @field:Schema(
        title = "Impfstoff-Status",
        type = "boolean",
        example = "false",
    )
    var vaccine = false

    @field:Schema(
        title = "Normgrösse",
        implementation = StandardESize::class,
        example = "N3",
    )
    var standardSize: StandardESize? = null

    @field:Schema(
        title = "Darreichungsform",
        implementation = SupplyEForm::class,
        example = "TAB",
    )
    var supplyForm: SupplyEForm? = null

    @field:Schema(
        title = "Packungsgröße",
        type = "string",
        example = "25",
    )
    var packagingSize: String? = null

    @field:Schema(
        title = "Packungseinheit",
        type = "string",
        example = "Stück",
    )
    var packagingUnit: String? = null

    @field:Schema(
        title = "Verpackung einer Rezeptur",
        type = "string",
        example = "Deo-Roller",
    )
    var packaging: String? = null

    @field:Schema(
        title = "Pharmazentralnummer",
        type = "string",
        example = "03386388",
    )
    var pzn: String? = null

    @field:Schema(
        title = "Medikamentenname",
        type = "string",
        example = "Prednison",
    )
    var name: String? = null
    var freeText: String? = null
}
