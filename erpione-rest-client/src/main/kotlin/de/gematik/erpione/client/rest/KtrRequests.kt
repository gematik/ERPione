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

import de.gematik.erpione.data.r4.gem.ErpEReceiptOut
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.appendEncodedPathSegments
import io.ktor.util.reflect.typeInfo

object KtrRequests {

    @JvmStatic
    fun accept(prescriptionId: String, accessCode: String) = ClaimPrescriptionRequest(prescriptionId, accessCode)

    @JvmStatic
    fun reject(prescriptionId: String) = ReturnPrescriptionClaimRequest(prescriptionId)

    @JvmStatic
    fun approve(prescriptionId: String) = CloseDigaClaimRequest(prescriptionId, true)

    @JvmStatic
    fun decline(prescriptionId: String) = CloseDigaClaimRequest(prescriptionId, false)

    @JvmStatic
    fun delete(prescriptionId: String, accessCode: String, secret: String? = null) = DeletePrescriptionRequest(prescriptionId, accessCode, secret)
}

class CloseDigaClaimRequest(
    private val prescriptionId: String,
    private val approval: Boolean,
) : ErpioneActorRequest<ErpEReceiptOut>(typeInfo<ErpEReceiptOut>()) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Post
        val mnemonicId = safeMnemonic(actorId)
        val approvalOp = if (approval) "approve" else "decline"
        rb.url.appendEncodedPathSegments("erp", mnemonicId, approvalOp, prescriptionId)

        rb.header(HttpHeaders.ContentType, ContentType.Application.Json)
    }
}
