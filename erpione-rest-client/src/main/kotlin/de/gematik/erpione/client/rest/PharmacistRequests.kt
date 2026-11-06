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

import de.gematik.erpione.data.error.EmptyData
import de.gematik.erpione.data.r4.gem.ErpEReceiptOut
import de.gematik.erpione.data.r4.gem.ErpETask
import de.gematik.erpione.data.r4.gem.MedicationDispenseEBundleIn
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.appendEncodedPathSegments
import io.ktor.util.reflect.typeInfo

object PharmacistRequests {
    @JvmStatic
    fun accept(prescriptionId: String, accessCode: String) = ClaimPrescriptionRequest(prescriptionId, accessCode)

    @JvmStatic
    fun reject(prescriptionId: String) = ReturnPrescriptionClaimRequest(prescriptionId)

    @JvmStatic
    fun dispense(prescriptionId: String, body: MedicationDispenseEBundleIn? = null) = DispensePrescriptionClaimRequest(prescriptionId, body)

    @JvmStatic
    fun close(prescriptionId: String, body: MedicationDispenseEBundleIn? = null) = ClosePrescriptionClaimRequest(prescriptionId, body)

    @JvmStatic
    fun delete(prescriptionId: String, accessCode: String, secret: String? = null) = DeletePrescriptionRequest(prescriptionId, accessCode, secret)
}

class ClaimPrescriptionRequest(
    private val prescriptionId: String,
    private val accessCode: String,
) : ErpioneActorRequest<ErpETask>(typeInfo<ErpETask>()) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Post
        val mnemonicId = safeMnemonic(actorId)
        rb.url.appendEncodedPathSegments("erp", mnemonicId, "claim", prescriptionId)
        rb.url.parameters.append("ac", accessCode)
        rb.header(HttpHeaders.ContentType, ContentType.Application.Json)
    }
}

class ReturnPrescriptionClaimRequest(private val prescriptionId: String) : ErpioneActorRequest<EmptyData>(typeInfo<EmptyData>()) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Post
        val mnemonicId = safeMnemonic(actorId)
        rb.url.appendEncodedPathSegments("erp", mnemonicId, "return", prescriptionId)
        rb.header(HttpHeaders.ContentType, ContentType.Application.Json)
    }
}

class DispensePrescriptionClaimRequest(private val prescriptionId: String, private val body: MedicationDispenseEBundleIn?) : ErpioneActorRequest<MedicationDispenseEBundleIn>(typeInfo<MedicationDispenseEBundleIn>()) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Post
        val mnemonicId = safeMnemonic(actorId)
        rb.url.appendEncodedPathSegments("erp", mnemonicId, "dispense", prescriptionId)
        rb.header(HttpHeaders.ContentType, ContentType.Application.Json)
        rb.setBody(body)
    }
}

class ClosePrescriptionClaimRequest(private val prescriptionId: String, private val body: MedicationDispenseEBundleIn?) : ErpioneActorRequest<ErpEReceiptOut>(typeInfo<ErpEReceiptOut>()) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Post
        val mnemonicId = safeMnemonic(actorId)
        rb.url.appendEncodedPathSegments("erp", mnemonicId, "close", prescriptionId)
        rb.header(HttpHeaders.ContentType, ContentType.Application.Json)
        rb.setBody(body)
    }
}
