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

package de.gematik.erpione.data.r4.chargeitem

import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData

interface PriceEComponent {
    val category: CostECategory?
    val type: String?
    val factor: Long?
    val insurantCost: Float?
    val totalCost: Float?
    val vatRate: Float?
}

interface TotalEGross {
    val insurantCost: Float?
    val totalCost: Float?
}

data class PriceEComponentIn(
    override val category: CostECategory? = null,
    override val type: String? = null,
    override val factor: Long? = null,
    override val insurantCost: Float? = null,
    override val totalCost: Float? = null,
    override val vatRate: Float? = null,
) : PriceEComponent,
    ErpioneInputData

data class PriceEComponentOut(
    override val category: CostECategory,
    override val type: String,
    override val factor: Long,
    override val insurantCost: Float,
    override val totalCost: Float,
    override val vatRate: Float,
) : PriceEComponent,
    ErpioneOutputData

data class TotalEGrossIn(
    override val insurantCost: Float? = null,
    override val totalCost: Float? = null,
) : TotalEGross,
    ErpioneInputData

data class TotalEGrossOut(
    override val insurantCost: Float,
    override val totalCost: Float,
) : TotalEGross,
    ErpioneOutputData
