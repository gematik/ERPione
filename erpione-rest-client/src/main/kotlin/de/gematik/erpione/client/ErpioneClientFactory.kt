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

package de.gematik.erpione.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.gematik.erpione.client.rest.BasicRequests
import de.gematik.erpione.client.rest.ErpioneGenericRequest
import de.gematik.erpione.data.actor.ActorERole
import de.gematik.erpione.data.actor.ErpioneInstitutionData
import de.gematik.erpione.data.meta.ErpioneServiceInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.serialization.jackson.jackson
import kotlin.random.Random

class ErpioneClientFactory(
    httpClient: HttpClient,
    config: ErpioneClientConfiguration,
) : ErpioneBaseClient<ErpioneGenericRequest<*>>(httpClient, config) {

    var info: ErpioneServiceInfo = performBlockingOrThrow(BasicRequests.getInfo())
    val actorsInfo: List<ErpioneInstitutionData> = performBlockingOrThrow(BasicRequests.getActors())
    private val doctors: List<ErpioneInstitutionData> = actorsInfo.filter { it.practitioner?.role == ActorERole.DOCTOR }
    private val pharmacies: List<ErpioneInstitutionData> = actorsInfo.filter { it.practitioner?.role == ActorERole.PHARMACIST }

    fun getDoctor(identifier: String): ErpioneActorClient {
        val doctorDto = findActorData(ActorERole.DOCTOR, identifier)
        return ErpioneActorClient(doctorDto.mnemonicId, httpClient, config)
    }

    fun getDoctor(idx: Int): ErpioneActorClient = ErpioneActorClient(
        doctors[idx].mnemonicId,
        httpClient,
        this.config,
    )

    fun getRandomDoctor(): ErpioneActorClient = getDoctor(Random.nextInt(doctors.size))

    fun getPharmacist(idx: Int): ErpioneActorClient = ErpioneActorClient(
        pharmacies[idx].mnemonicId,
        httpClient,
        this.config,
    )

    fun getPharmacist(identifier: String): ErpioneActorClient {
        val pharmacyDto = findActorData(ActorERole.PHARMACIST, identifier)
        return ErpioneActorClient(pharmacyDto.mnemonicId, httpClient, this.config)
    }

    fun getRandomPharmacist(): ErpioneActorClient = getPharmacist(Random.nextInt(pharmacies.size))

    private fun findActorData(role: ActorERole, mnemonicId: String): ErpioneInstitutionData = actorsInfo.find {
        it.practitioner?.role == role &&
            (
                it.mnemonicId == mnemonicId ||
                    it.mnemonic.equals(
                        mnemonicId,
                        ignoreCase = true,
                    )
                )
    } ?: throw UnknownActorException(role, mnemonicId)

    companion object {
        @JvmStatic
        fun forRemote(baseUrl: String): ClientBuilder {
            val url = URLBuilder(baseUrl)
            val clientBuilder = ClientBuilder(url.protocol, url.host)

            if (url.port > 0) {
                clientBuilder.port(url.port)
            }

            return clientBuilder
        }
    }

    class ClientBuilder(protocol: URLProtocol, host: String) {

        val config: ErpioneClientConfiguration = ErpioneClientConfiguration(protocol, host)

        private var timeoutMillis: Long = 15000

        fun port(port: Int) = apply { config.port = port }
        fun apiKey(apiKey: String) = apply { config.apiKey = apiKey }

        fun timeoutMillis(timeout: Long) = apply {
            this.timeoutMillis = timeout
        }

        fun build(): ErpioneClientFactory {
            val httpClient = HttpClient {
                install(HttpTimeout) {
                    connectTimeoutMillis = timeoutMillis
                    requestTimeoutMillis = timeoutMillis
                }
                install(ContentNegotiation) {
                    jackson {
                        // TODO: see de.gematik.erpione.server.actor.factory.JacksonConfiguration
                        ObjectMapper().registerKotlinModule {
                            setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                        }
                    }
                }
            }
            return build(httpClient)
        }

        fun build(httpClient: HttpClient): ErpioneClientFactory = ErpioneClientFactory(httpClient, config)
    }
}
