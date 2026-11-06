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

package de.gematik.erpione.cli.print

import de.gematik.erpione.data.ErpioneData
import de.gematik.erpione.data.actor.ActorERole
import de.gematik.erpione.data.actor.ErpioneActorData
import de.gematik.erpione.data.actor.ErpioneInstitutionData
import de.gematik.erpione.data.actor.InstitutionERole
import de.gematik.erpione.data.meta.ErpioneBuildInfo
import de.gematik.erpione.data.meta.ErpioneFhirInfo
import de.gematik.erpione.data.meta.ErpioneServiceInfo
import de.gematik.erpione.data.meta.TIConnectionData
import de.gematik.erpione.data.meta.TIServiceData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

class DefaultErpioneFormatterTest {

    val formatter = DefaultErpioneFormatter()

    @Test
    fun `should encodeToString with String input`() {
        val input = "Test String"
        val result = assertDoesNotThrow { formatter.encodeToString(input) }
        assertFalse(result.isEmpty())
    }

    @Test
    fun `should encodeToString unknown types without throwing`() {
        val input = UUID.randomUUID()
        val result = assertDoesNotThrow { formatter.encodeToString(input) }
        assertFalse(result.isEmpty())
    }

    @Test
    fun `should encodeToString unknown ErpioneData types without throwing`() {
        val input = ErpioneTestData("hello world")
        val result = assertDoesNotThrow { formatter.encodeToString(input) }
        assertFalse(result.isEmpty())
    }

    @Test
    fun `should encodeToString with InfoData input`() {
        val certDummy = "MIIDAzCCAqmgAwIBAgICG4IwCgYIKoZIzj0EAwIwgYQxCzAJBgNVBAYTAkRFMR8wHQYDVQQKDBZnZW1hdGlrIEdtYkggTk9ULVZBTElEMTIwMAYDVQQLDClLb21wb25lbnRl"
        val tiData = TIConnectionData()
            .apply {
                addService("erp", TIServiceData("https://erp-ref.zentral.erp.splitdns.ti-dienste.de", certDummy))
                addService("idp", TIServiceData("https://idp-ref.zentral.idp.splitdns.ti-dienste.de/.well-known/openid-configuration"))
            }
        val build = ErpioneBuildInfo.fromEnvironment()
        val fhir = ErpioneFhirInfo("1.2.3")
        fhir.profiles.put("kbv.basis", "1.7.0")
        fhir.profiles.put("de.gematik.epa.medication", "1.5.0")
        fhir.profiles.put("de.gematik.erezept-patientenrechnung.r4", "1.1.0")

        val input = ErpioneServiceInfo(build = build, ti = tiData, fhir = fhir)

        val result = assertDoesNotThrow { formatter.encodeToString(input) }
        assertFalse(result.isEmpty())
    }

    @Test
    fun `should encodeToString with ActorInstitutionData input`() {
        val input = ErpioneInstitutionData(
            mnemonicId = "12345",
            mnemonic = "TestActor",
            name = "Test Institution",
            role = InstitutionERole.PHARMACY,
        )
        input.smcb.profession = "Arztpraxis"
        input.practitioner = ErpioneActorData(
            role = ActorERole.DOCTOR,
            name = "Test Doctor",
        )
        val result = assertDoesNotThrow { formatter.encodeToString(input) }
        assertFalse(result.isEmpty())
    }

    data class ErpioneTestData(val value: String) : ErpioneData
}
