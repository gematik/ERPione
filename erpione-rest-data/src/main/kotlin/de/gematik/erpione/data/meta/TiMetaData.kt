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

package de.gematik.erpione.data.meta

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import de.gematik.erpione.data.ErpioneData
import de.gematik.erpione.data.ErpioneOutputData
import de.gematik.erpione.data.ErpioneValueSet
import io.swagger.v3.oas.annotations.media.Schema

data class TIServiceData(
    val url: String,
    val certificate: String?,
) : ErpioneData {

    // no argument defaults in Java...
    constructor(url: String) : this(url, certificate = null)
}

data class TIConnectionData(
    val services: MutableMap<String, TIServiceData> = mutableMapOf(),
) : ErpioneData {

    fun addService(name: String, service: TIServiceData) {
        services[name] = service
    }
}

data class TIHealthCheckResult(
    @field:Schema(
        title = "Health-Check-Status",
        implementation = TIHealthCheckStatus::class,
        example = "OK",
    )
    val status: TIHealthCheckStatus,

    @field:Schema(
        title = "HTTP Status Code",
        type = "integer",
        example = "200",
    )
    val statusCode: Int,

    @field:Schema(
        title = "Latenz in Millisekunden",
        type = "long",
        example = "75",
    )
    val latency: Long,
) : ErpioneOutputData

enum class TIHealthCheckStatus : ErpioneValueSet<TIHealthCheckStatus> {
    OK,
    ERROR,
    ;

    @JsonValue
    override val code = name
    override val display = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(input: String?): TIHealthCheckStatus = ErpioneValueSet.fromStringOrThrow(
            TIHealthCheckStatus::class.java,
            input,
        )

        @JvmStatic
        fun fromHttpStatusCode(statusCode: Int): TIHealthCheckStatus = when (statusCode) {
            in 200..299 -> OK
            else -> ERROR
        }
    }
}
