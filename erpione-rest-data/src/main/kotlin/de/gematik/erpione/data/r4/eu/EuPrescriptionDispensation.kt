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

package de.gematik.erpione.data.r4.eu

import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData
import de.gematik.erpione.data.r4.EMedication
import de.gematik.erpione.data.r4.MedicationEBatch
import de.gematik.erpione.data.r4.MedicationEBatchIn
import io.swagger.v3.oas.annotations.media.Schema

interface EuPrescriptionDispensation {
    @get:Schema(
        title = "PrescriptionId",
        type = "string",
        example = "160.000.114.564.495.76",
    )
    val prescriptionId: String
    val medication: EMedication?
    val batch: MedicationEBatch?

    @get:Schema(
        title = "Wurde das Rezept substituiert?",
        type = "boolean",
        example = "false",
    )
    val wasSubstituted: Boolean?
}

data class EuPrescriptionDispensationIn(
    override val prescriptionId: String,
    override var medication: EMedication? = null,
    override val batch: MedicationEBatchIn? = null,
    override val wasSubstituted: Boolean? = null,
) : EuPrescriptionDispensation,
    ErpioneInputData

data class EuPrescriptionDispensationListIn(
    var prescriptions: List<EuPrescriptionDispensationIn> = emptyList(),
) : ErpioneInputData {
    companion object {
        @JvmStatic
        fun empty() = EuPrescriptionDispensationListIn()

        @JvmStatic
        fun of(prescriptions: List<EuPrescriptionDispensationIn>) = EuPrescriptionDispensationListIn(
            prescriptions = prescriptions,
        )
    }
}

/**
 * Output data for the EU-dispensation of multiple prescriptions by an EU-Pharmacy.
 * The output data contains a list of dispensation results for each prescription.
 */
data class EuPrescriptionDispensationBatchOut(
    val dispensations: List<EuPrescriptionDispensationOut>,
) : ErpioneOutputData

/**
 * Output data for a single EU-dispensation of a prescription by an EU-Pharmacy.
 * The output data contains the prescription ID and the status code the Erpione has received for this operation from the Fachdienst.
 */
data class EuPrescriptionDispensationOut(
    @get:Schema(
        title = "PrescriptionId",
        type = "string",
        example = "160.000.114.564.495.76",
    )
    val prescriptionId: String,

    @get:Schema(
        title = "HTTP Status Code",
        description = "HTTP Status Code received from the Fachdienst for this operation",
        type = "integer",
        example = "200",
    )
    val statusCode: Int,
) : ErpioneOutputData
