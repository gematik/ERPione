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
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.appendEncodedPathSegments
import io.ktor.util.reflect.typeInfo

class DeletePrescriptionRequest(
    private val prescriptionId: String,
    private val accessCode: String,
    private val secret: String? = null,
) : ErpioneActorRequest<EmptyData>(typeInfo<EmptyData>()) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Delete
        val mnemonicId = safeMnemonic(actorId)
        rb.url.appendEncodedPathSegments("erp", mnemonicId, "abort", prescriptionId)
        rb.url.parameters.append("ac", accessCode)
        secret?.let { rb.url.parameters.append("secret", it) }
        rb.header(HttpHeaders.ContentType, ContentType.Application.Json)
    }
}
