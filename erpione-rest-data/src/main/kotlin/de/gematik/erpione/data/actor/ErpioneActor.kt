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

package de.gematik.erpione.data.actor

import de.gematik.erpione.data.ErpioneData

/**
 * the Leistungserbringer (service provider) in the ERPIONE system
 */
data class ErpioneActorData(
    var name: String = "n/a",
    val role: ActorERole,
    var hba: SmartcardEData? = null,
) : ErpioneData

data class ErpioneInstitutionData(
    val mnemonicId: String,
    val mnemonic: String,
    var name: String = "n/a",
    val role: InstitutionERole,
) : ErpioneData {
    var countryCode: String = "n/a"
    var smcb: SmartcardEData = SmartcardEData()
    var practitioner: ErpioneActorData? = null
}
