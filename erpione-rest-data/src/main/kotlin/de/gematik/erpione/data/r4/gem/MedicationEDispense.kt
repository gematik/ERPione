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

package de.gematik.erpione.data.r4.gem

import com.fasterxml.jackson.annotation.JsonFormat
import de.gematik.erpione.data.ErpioneData
import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData
import de.gematik.erpione.data.r4.EMedication
import de.gematik.erpione.data.r4.MedicationEBatch
import de.gematik.erpione.data.r4.MedicationEBatchIn
import de.gematik.erpione.data.r4.MedicationEBatchOut
import java.util.*

interface MedicationEDispense {
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    val whenHandedOver: Date?

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    val whenPrepared: Date?

    val dosageInstructions: MutableList<String>
    val notes: MutableList<String>
}

interface MedicationDispenseEPackage<MD : MedicationEDispense> {
    val medication: EMedication?
    val batch: MedicationEBatch?
    val dispense: MD?
}

class MedicationEDispenseIn(
    override var whenHandedOver: Date? = null,
    override var whenPrepared: Date? = null,
    override val dosageInstructions: MutableList<String> = LinkedList<String>(),
    override val notes: MutableList<String> = LinkedList<String>(),
) : MedicationEDispense,
    ErpioneInputData

class MedicationEDispenseOut(
    override var whenHandedOver: Date,
    override var whenPrepared: Date?,
    override val dosageInstructions: MutableList<String> = LinkedList<String>(),
    override val notes: MutableList<String> = LinkedList<String>(),
) : MedicationEDispense,
    ErpioneOutputData

class MedicationDispenseEPackageIn(
    override var medication: EMedication? = null,
    override var batch: MedicationEBatchIn? = null,
    override var dispense: MedicationEDispenseIn? = null,
) : MedicationDispenseEPackage<MedicationEDispenseIn>,
    ErpioneInputData

class MedicationDispenseEPackageOut(
    override val medication: EMedication,
    override val batch: MedicationEBatchOut?,
    override val dispense: MedicationEDispenseOut,
) : MedicationDispenseEPackage<MedicationEDispenseOut>,
    ErpioneInputData

data class MedicationDispenseEBundleIn(
    val dispensation: List<MedicationDispenseEPackageIn> = emptyList(),
) : ErpioneData,
    ErpioneInputData

data class MedicationDispenseEBundleOut(
    val dispensation: List<MedicationDispenseEPackageOut> = emptyList(),
) : ErpioneData,
    ErpioneOutputData
