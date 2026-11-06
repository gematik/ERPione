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

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.utils.EmptyContent
import io.ktor.http.encodedPath
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.function.Supplier

class CommonActorRequestsTests {

    @ParameterizedTest
    @MethodSource("claimRequestProvider")
    fun shouldClaimRandomPrescription(supplier: Supplier<ClaimPrescriptionRequest>) {
        val rb = HttpRequestBuilder()
        val request = supplier.get()
        request.finalizeRequest(rb, "Hygieia")

        assert(rb.url.encodedPath == "erp/Hygieia/claim/pid")
        assert(rb.url.parameters["ac"] == "ac")
        assert(rb.method.value == "POST")
        assertTrue(rb.body is EmptyContent)
    }

    @ParameterizedTest
    @MethodSource("returnRequestProvider")
    fun shouldReturnRandomPrescriptionClaim(supplier: Supplier<ReturnPrescriptionClaimRequest>) {
        val rb = HttpRequestBuilder()
        val request = supplier.get()
        request.finalizeRequest(rb, "Hygieia")
        // Hades is the default mnemonic if don't specify one
        // hades will fail on backend but recognizable as this specific issue
        assert(rb.url.encodedPath == "erp/Hygieia/return/pid")
        assert(rb.method.value == "POST")
        assertTrue(rb.body is EmptyContent)
    }

    @ParameterizedTest
    @MethodSource("deleteRequestProvider")
    fun shouldDeleteRandomPrescription(supplier: Supplier<DeletePrescriptionRequest>) {
        val rb = HttpRequestBuilder()
        val request = supplier.get()
        request.finalizeRequest(rb, "Hygieia")

        assert(rb.url.encodedPath == "erp/Hygieia/abort/pid")
        assert(rb.url.parameters["ac"] == "ac")
        assert(rb.method.value == "DELETE")
        assertTrue(rb.body is EmptyContent)
    }

    companion object Companion {

        @JvmStatic
        fun claimRequestProvider(): List<Supplier<ClaimPrescriptionRequest>> = listOf(
            Supplier { PharmacistRequests.accept("pid", "ac") },
            Supplier { KtrRequests.accept("pid", "ac") },
        )

        @JvmStatic
        fun returnRequestProvider(): List<Supplier<ReturnPrescriptionClaimRequest>> = listOf(
            Supplier { PharmacistRequests.reject("pid") },
            Supplier { KtrRequests.reject("pid") },
        )

        @JvmStatic
        fun deleteRequestProvider(): List<Supplier<DeletePrescriptionRequest>> = listOf(
            Supplier { PharmacistRequests.delete("pid", "ac") },
            Supplier { PharmacistRequests.delete("pid", "ac", "secret") },
            Supplier { DoctorRequests.delete("pid", "ac") },
            Supplier { KtrRequests.delete("pid", "ac") },
        )
    }
}
