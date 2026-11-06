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

import arrow.core.Either
import arrow.core.getOrElse
import de.gematik.erpione.client.ErpioneRestException
import de.gematik.erpione.data.error.ErrorData
import io.ktor.http.Headers

class ErpioneResponse<T>(val statusCode: Int, val headers: Headers = Headers.Empty, val body: Either<ErrorData, T>) {

    /**
     * Unwrap the Either-Body and get the expected response body
     *
     * **Attention: ** if the body contains an [ErrorData] this method will throw a [ErpioneRestException]
     */
    fun asExpectedPayload(): T = this.body.getOrElse {
        val message = "unexpected response: [${it.type}] ${it.message}"
        throw ErpioneRestException(statusCode, message)
    }

    fun successful(): Boolean = this.body.isRight()
}
