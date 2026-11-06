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

import de.gematik.erpione.data.ErpioneData
import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData
import de.gematik.erpione.data.r4.EMedication
import de.gematik.erpione.data.r4.HumanEName
import de.gematik.erpione.data.r4.HumanENameIn
import de.gematik.erpione.data.r4.HumanENameOut
import de.gematik.erpione.data.r4.PhysicianENumber
import de.gematik.erpione.data.r4.PhysicianENumberIn
import de.gematik.erpione.data.r4.PhysicianENumberOut

interface KbvEPrescription<
    NAME : HumanEName,
    PAT : KbvEPatient<NAME>,
    COV : KbvECoverage,
    MR : KbvEMedicationRequest,
    HAR : KbvEHealthAppRequest,
    PEN : PhysicianENumber,
    PRA : KbvEPractitioner<NAME, PEN>,
    PROLE : KbvEPractitionerRole,
    ORG : KbvEOrganization,
    > {
    val patient: PAT?
    val medication: EMedication?
    val coverage: COV?
    val medicationRequest: MR?
    val healthAppRequest: HAR?
    val practitioner: PRA?
    val practitionerRole: PROLE?
    val organization: ORG?
}

data class KbvEPrescriptionIn(
    override var patient: KbvEPatientIn? = null,
    override var medication: EMedication? = null,
    override var coverage: KbvECoverageIn? = null,
    override var medicationRequest: KbvEMedicationRequestIn? = null,
    override var healthAppRequest: KbvEHealthAppRequestIn? = null,
    override var practitioner: KbvEPractitionerIn? = null,
    override var practitionerRole: KbvEPractitionerRoleIn? = null,
    override var organization: KbvEOrganizationIn? = null,
) : KbvEPrescription<HumanENameIn, KbvEPatientIn, KbvECoverageIn, KbvEMedicationRequestIn, KbvEHealthAppRequestIn, PhysicianENumberIn, KbvEPractitionerIn, KbvEPractitionerRoleIn, KbvEOrganizationIn>,
    ErpioneInputData

open class KbvEPrescriptionOut(
    val prescriptionId: String,
    override val patient: KbvEPatientOut,
    override var medication: EMedication?,
    override var coverage: KbvECoverageOut,
    override var medicationRequest: KbvEMedicationRequestOut? = null,
    override var healthAppRequest: KbvEHealthAppRequestOut? = null,
    override var practitioner: KbvEPractitionerOut,
    override var practitionerRole: KbvEPractitionerRoleOut? = null,
    override var organization: KbvEOrganizationOut,
) : KbvEPrescription<HumanENameOut, KbvEPatientOut, KbvECoverageOut, KbvEMedicationRequestOut, KbvEHealthAppRequestOut, PhysicianENumberOut, KbvEPractitionerOut, KbvEPractitionerRoleOut, KbvEOrganizationOut>,
    ErpioneOutputData

data class KbvEPrescriptionList(
    var prescriptions: List<KbvEPrescriptionOut> = emptyList(),
) : ErpioneData {

    val total: Int = prescriptions.size

    companion object {
        @JvmStatic
        fun empty() = KbvEPrescriptionList()

        @JvmStatic
        fun of(prescriptions: List<KbvEPrescriptionOut>) = KbvEPrescriptionList(
            prescriptions = prescriptions,
        )
    }
}
