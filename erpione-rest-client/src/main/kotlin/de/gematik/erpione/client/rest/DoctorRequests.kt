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

package de.gematik.erpione.client.rest

import de.gematik.erpione.data.r4.gem.ErpETask
import de.gematik.erpione.data.r4.kbv.KbvEPatientIn
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionIn
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.appendEncodedPathSegments
import io.ktor.util.reflect.typeInfo
import java.io.File
import java.nio.file.Path

object DoctorRequests {

    @JvmStatic
    fun prescribeKbvBundle(file: Path, asDirectAssignment: Boolean = false) = prescribeKbvBundle(file.toFile(), asDirectAssignment)

    @JvmStatic
    fun prescribeKbvBundle(file: File, asDirectAssignment: Boolean = false) = PrescribeKbvBundleRequest(file, asDirectAssignment)

    @JvmStatic
    fun prescribe(kvnr: String, asDirectAssignment: Boolean = false) = prescribe(
        KbvEPrescriptionIn(
            patient = KbvEPatientIn(
                kvnr = kvnr,
            ),
        ),
        asDirectAssignment,
    )

    @JvmStatic
    fun prescribe(body: KbvEPrescriptionIn, asDirectAssignment: Boolean = false) = PrescribeRequest(body, asDirectAssignment)

    @JvmStatic
    fun delete(prescriptionId: String, accessCode: String, secret: String? = null) = DeletePrescriptionRequest(prescriptionId, accessCode, secret)
}

class PrescribeRequest(private val body: KbvEPrescriptionIn, private val asDirectAssignment: Boolean = false) :
    ErpioneActorRequest<ErpETask>(
        typeInfo<ErpETask>(),
    ) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Post
        val mnemonicId = safeMnemonic(actorId)
        rb.url.appendEncodedPathSegments("erp", mnemonicId, "pharmaceutical")
        rb.url.parameters.append("direct", asDirectAssignment.toString())
        rb.setBody(body)
    }
}

class PrescribeKbvBundleRequest(private val file: File, private val asDirectAssignment: Boolean = false) : ErpioneActorRequest<ErpETask>(typeInfo<ErpETask>()) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Post
        val mnemonicId = safeMnemonic(actorId)
        rb.url.appendEncodedPathSegments("erp", mnemonicId, "pharmaceutical")
        rb.url.parameters.append("direct", asDirectAssignment.toString())

        if (file.extension == "xml") {
            rb.headers.remove(HttpHeaders.ContentType)
            rb.header(HttpHeaders.ContentType, ContentType.Application.Xml)
        }
        rb.setBody(file.readText(Charsets.UTF_8))
    }
}
