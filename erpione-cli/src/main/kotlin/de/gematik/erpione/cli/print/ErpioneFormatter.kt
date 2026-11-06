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
import de.gematik.erpione.data.actor.ErpioneInstitutionData
import de.gematik.erpione.data.meta.ErpioneBuildInfo
import de.gematik.erpione.data.meta.ErpioneServiceInfo
import de.gematik.erpione.data.meta.TIServiceData

fun interface ErpioneFormatter {
    fun encodeToString(data: Any): String
}

class DefaultErpioneFormatter : ErpioneFormatter {
    override fun encodeToString(data: Any): String = when (data) {
        is String -> data
        is ErpioneData -> stringifyErpioneData(data)
        else -> data.toString()
    }

    private fun stringifyErpioneData(data: ErpioneData): String = when (data) {
        is ErpioneServiceInfo -> data.toCliString()
        is ErpioneInstitutionData -> data.toCliString()
        else -> data.toString()
    }
}

private fun ErpioneInstitutionData.toCliString(): String {
    val name = this.name
    val profession = this.smcb.profession ?: "n/a"
    val smcb = this.smcb
    val staff = this.practitioner

    val r = buildString {
        append("# $name ($profession)")
        append("\n  SMC-B: ${smcb.telematikId} (${smcb.validUntil})")
        staff?.let {
            append("\n##  ${it.name} (${it.hba?.profession})")
            append("\n    HBA: ${it.hba?.telematikId} (${it.hba?.validUntil})")
        }
    }

    return r.ifEmpty { "No information available" }
}

private fun alignVersions(versions: Map<String, String>): String {
    val maxKeyLength = versions.keys.maxOf { it.length }
    return "  " + versions.entries.joinToString("\n  ") { (key, value) ->
        "${key.padEnd(maxKeyLength, ' ')} : $value"
    }
}

private fun ErpioneServiceInfo.toCliString(): String {
    val tiInfo: Map<String, TIServiceData> = this.ti.services
    val fhirInfo = this.fhir
    val buildInfoServer = this.build
    val buildInfoClient = ErpioneBuildInfo.fromEnvironment()

    val r = buildString {
        append("### Erpione Information")

        append("\n# TI Connections:")
        tiInfo.forEach { service ->
            append("\n  ${service.key}: ${service.value.url}")

            service.value.certificate?.let { append("\n    cert: $it") }
        }

        append("\n")
        append("-".repeat(30))
        append("\n# ERP FHIR View ${fhirInfo.view}\n")
        append(alignVersions(fhirInfo.profiles))

        append("\n")
        append("-".repeat(30))
        append("\n# Erpione Build Information:")
        append("\n== Version:")
        append("\n   - Server: ${buildInfoServer.version}")
        append("\n   - Client: ${buildInfoClient.version}")
        append("\n== Commit:")
        append("\n   - Server: ${buildInfoServer.hash}")
        append("\n   - Client: ${buildInfoClient.hash}")
        append("\n== Build Date:")
        append("\n   - Server: ${buildInfoServer.buildDate}")
        append("\n   - Client: ${buildInfoClient.buildDate}")
    }

    return r.ifEmpty { "No information available" }
}
