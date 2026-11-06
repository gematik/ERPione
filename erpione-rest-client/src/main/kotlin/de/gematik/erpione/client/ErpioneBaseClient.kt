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

import arrow.core.Either
import de.gematik.erpione.client.rest.ErpioneActorRequest
import de.gematik.erpione.client.rest.ErpioneGenericRequest
import de.gematik.erpione.client.rest.ErpioneRequest
import de.gematik.erpione.client.rest.ErpioneResponse
import de.gematik.erpione.data.error.ErrorData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.appendEncodedPathSegments
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Client for interacting with the Erpione Actor API.
 *
 * @param mnemonicId The mnemonic ID of the actor to interact with.
 * @param httpClient The HTTP client used for making requests.
 * @param clientData Configuration data for the Erpione client.
 */
class ErpioneActorClient(
    val mnemonicId: String,
    httpClient: HttpClient,
    clientData: ErpioneClientConfiguration,
) : ErpioneBaseClient<ErpioneActorRequest<*>>(httpClient, clientData) {

    suspend fun <T, R : ErpioneActorRequest<T>> perform(request: R): ErpioneResponse<T> {
        val rb = this.initRequestBuilder()
        request.finalizeRequest(rb, mnemonicId)
        return performHttpRequest(request, rb)
    }

    fun <T, R : ErpioneActorRequest<T>> performBlocking(request: R): ErpioneResponse<T> = runBlocking {
        perform(request)
    }

    fun <T, R : ErpioneActorRequest<T>> performBlockingOrThrow(request: R): T {
        val response = performBlocking(request)
        return when (response.body) {
            is Either.Left -> throw ErpioneRestException(response.statusCode, response.body.value)
            is Either.Right -> response.body.value
        }
    }
}

/**
 * Base class for all Erpione clients.
 *
 * @param R The type of request this client can perform.
 */
abstract class ErpioneBaseClient<R : ErpioneRequest<*>>(
    val httpClient: HttpClient,
    val config: ErpioneClientConfiguration,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val contentType: ContentType = ContentType.Application.Json

    protected fun initRequestBuilder(): HttpRequestBuilder {
        val rb = HttpRequestBuilder()
        rb.url.protocol = this@ErpioneBaseClient.config.protocol
        rb.url.host = this@ErpioneBaseClient.config.host

        this@ErpioneBaseClient.config.port?.let { rb.url.port = it }

        this@ErpioneBaseClient.config.apiKey?.let { rb.header("apiKey", it) }

        rb.header(HttpHeaders.ContentType, contentType)
        rb.header(HttpHeaders.Accept, contentType)

        rb.url.appendEncodedPathSegments("api")
        return rb
    }

    suspend fun <T, R : ErpioneGenericRequest<T>> perform(request: R): ErpioneResponse<T> {
        val rb = this.initRequestBuilder()
        request.finalizeRequest(rb)
        return performHttpRequest(request, rb)
    }

    fun <T, R : ErpioneGenericRequest<T>> performBlocking(request: R): ErpioneResponse<T> = runBlocking {
        perform(request)
    }

    fun <T, R : ErpioneGenericRequest<T>> performBlockingOrThrow(request: R): T {
        val response = performBlocking(request)
        return when (response.body) {
            is Either.Left -> throw ErpioneRestException(response.statusCode, response.body.value)
            is Either.Right -> response.body.value
        }
    }

    protected suspend fun <T, R : ErpioneRequest<T>> performHttpRequest(
        request: R,
        rb: HttpRequestBuilder,
    ): ErpioneResponse<T> {
        try {
            val httpStatement = httpClient.prepareRequest(rb)
            log.info("Performing request: ${rb.method.value} ${rb.url}")
            val response = httpStatement.execute()
            return unwrapResponse(request, response)
        } catch (e: Exception) {
            log.error("Error during request", e)
            return ErpioneResponse(
                500,
                Headers.Empty,
                Either.Left(ErrorData.internalError(e.localizedMessage).withoutIssues()),
            )
        }
    }

    private suspend fun <R : ErpioneRequest<T>, T> unwrapResponse(
        request: ErpioneRequest<T>,
        response: HttpResponse,
    ): ErpioneResponse<T> {
        try {
            return when (response.status.value < 300) {
                true -> {
                    val body = response.body<T>(request.typeInfo)
                    return ErpioneResponse(response.status.value, response.headers, Either.Right(body))
                }

                false -> handleErpioneErrorResponse(response)
            }
        } catch (e: Exception) {
            log.error("Error during deserialization of response", e)
            return handleErpioneErrorResponse(response)
        }
    }

    private suspend fun <T> handleErpioneErrorResponse(response: HttpResponse): ErpioneResponse<T> = try {
        val body = response.body<ErrorData>()
        ErpioneResponse(response.status.value, response.headers, Either.Left(body))
    } catch (e: Exception) {
        log.error("Unexpected error while handling error response", e)
        ErpioneResponse(
            response.status.value,
            response.headers,
            Either.Left(ErrorData.internalError(e.message ?: "Unknown error").withoutIssues()),
        )
    }
}
