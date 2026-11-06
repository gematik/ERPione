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

import de.gematik.erpione.data.actor.ErpioneInstitutionData
import de.gematik.erpione.data.meta.ErpioneServiceInfo
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.appendEncodedPathSegments
import io.ktor.util.reflect.typeInfo

object BasicRequests {

    @JvmStatic
    fun getInfo() = InfoRequest()

    @JvmStatic
    fun getActors() = ActorsRequest()

    @JvmStatic
    fun getActorByMnemonic(mnemonic: String) = ActorByMnemonicRequest(mnemonic)

    @JvmStatic
    fun getActorInformation() = GetActorInformationRequest()
}

class InfoRequest : ErpioneGenericRequest<ErpioneServiceInfo>(typeInfo<ErpioneServiceInfo>()) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Get
        rb.accept(ContentType.Application.Json)
        rb.url.appendEncodedPathSegments("info")
    }
}

class ActorsRequest :
    ErpioneGenericRequest<List<ErpioneInstitutionData>>(
        typeInfo<List<ErpioneInstitutionData>>(),
    ) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Get
        rb.url.appendEncodedPathSegments("actors")
    }
}

class ActorByMnemonicRequest(val mnemonic: String) :
    ErpioneGenericRequest<ErpioneInstitutionData>(
        typeInfo<ErpioneInstitutionData>(),
    ) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Get
        rb.url.appendEncodedPathSegments("actors")
        rb.url.appendEncodedPathSegments(mnemonic)
    }
}

class GetActorInformationRequest :
    ErpioneActorRequest<ErpioneInstitutionData>(
        typeInfo<ErpioneInstitutionData>(),
    ) {

    override fun finalizeRequest(rb: HttpRequestBuilder, actorId: String?) {
        rb.method = HttpMethod.Get
        val mnemonic = actorId ?: "Hades"
        rb.url.appendEncodedPathSegments("actors")
        rb.url.appendEncodedPathSegments(mnemonic)
    }
}
