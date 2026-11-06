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

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.lessThanOrExactly
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import de.gematik.erpione.client.rest.BasicRequests
import de.gematik.erpione.client.rest.RestTest
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@WireMockTest
class ErpioneClientFactoryTest : RestTest() {

    @Test
    fun shouldGetBaseInformationOnStartup() {
        setupMetaStubs()
        val clientFactory = ErpioneClientFactory
            .forRemote("http://127.0.0.1").port(port).build()

        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/info")))
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/actors")))

        assertEquals(4, clientFactory.actorsInfo.size)
    }

    @Test
    fun shouldGetActorsByIndex() {
        setupMetaStubs()
        val clientFactory = ErpioneClientFactory
            .forRemote("http://127.0.0.1").port(port).build()

        assertDoesNotThrow {
            clientFactory.getDoctor(0)
            clientFactory.getPharmacist(0)
        }
    }

    @Test
    fun shouldGetActorsByIdentifier() {
        setupMetaStubs()
        val clientFactory = ErpioneClientFactory
            .forRemote("http://127.0.0.1").port(port).build()

        assertDoesNotThrow {
            clientFactory.getDoctor("Doctor 0")
            clientFactory.getDoctor("DOCTOR 0")
            clientFactory.getPharmacist("Pharmacist 0")

            // fetch the actual actor information
            val pharm = clientFactory.getPharmacist("pharmacist 0")
            val r1 = pharm.performBlocking(BasicRequests.getActorInformation()).asExpectedPayload()

            val r2 = clientFactory.performBlocking(BasicRequests.getActorByMnemonic(r1.mnemonicId)).asExpectedPayload()

            // both calls should return the same actor information
            assertEquals(r1, r2)
        }
    }

    @TestFactory
    fun shouldThrowOnUnknownDoctorClient() = actorIdentifierData().map { actorId ->
        DynamicTest.dynamicTest("Should throw on unknown Doctor with ID $actorId") {
            setupMetaStubs()
            val clientFactory = ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()

            assertThrows<UnknownActorException> {
                clientFactory.getDoctor(actorId)
            }
        }
    }

    @TestFactory
    fun shouldThrowOnUnknownPharmacyClient() = actorIdentifierData().map { actorId ->
        DynamicTest.dynamicTest("Should throw on unknown Pharmacy with ID $actorId") {
            setupMetaStubs()
            val clientFactory = ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()

            assertThrows<UnknownActorException> {
                clientFactory.getPharmacist(actorId)
            }
        }
    }

    fun actorIdentifierData() = arrayOf(
        "Doctor 00",
        "doc00",
        "Pharmacy 00",
        "pharm00",
    )

    @Test
    fun shouldGetRandomActors() {
        setupMetaStubs()
        val clientFactory = ErpioneClientFactory
            .forRemote("http://127.0.0.1").port(port).build()

        assertDoesNotThrow {
            clientFactory.getRandomDoctor()
            clientFactory.getRandomPharmacist()
        }
    }

    @Test
    fun shouldFailOnErrorAtInformation() {
        setupErrorInfo()
        assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()
        }
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/info")))
    }

    @Test
    fun shouldFailOnErrorAtActors() {
        setupPositiveInfo()
        setupErrorActors()
        assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()
        }
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/info")))
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/actors")))
    }

    @Test
    fun shouldHandleHtmlErrorResponseAtActors() {
        setupPositiveInfo()
        setupErrorActorsHtml()
        assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()
        }
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/info")))
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/actors")))
    }

    @Test
    fun shouldHandleEmptyErrorResponseAtActors() {
        setupPositiveInfo()
        setupErrorActorsEmpty()
        assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()
        }
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/info")))
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/actors")))
    }

    @Test
    fun shouldHandleInvalidPrimSysErrorResponse() {
        setupPositiveInfo()
        setupActorsInvalidErrorResponse(400)
        assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()
        }
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/info")))
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/actors")))
    }

    @Test
    fun shouldHandleInvalidErpioneResponse() {
        setupPositiveInfo()
        setupActorsInvalidErrorResponse(200)
        assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()
        }
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/info")))
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/actors")))
    }

    @Test
    fun shouldHandleValidPrimSysErrorResponse() {
        setupPositiveInfo()
        setupActorsValidErrorResponse()
        assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).build()
        }
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/info")))
        wiremockExtension.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/actors")))
    }

    @Test
    fun shouldThrowOnConnectionTimeout() {
        val timeout = 25
        setupConnectionTimeout(timeout * 2)
        val ce = assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").port(port).timeoutMillis(timeout.toLong()).build()
        }
        assertTrue(ce.message!!.contains("timeout"))
        assertTrue(ce.message!!.contains("http://127.0.0.1:$port/api/info"))

        // in CI the info request is not detected reliably, so we allow 0 or 1 requests to be detected
        wiremockExtension.verify(lessThanOrExactly(1), getRequestedFor(urlPathEqualTo("/info")))
        wiremockExtension.verify(exactly(0), getRequestedFor(urlPathEqualTo("/actors")))
    }

    @Test
    fun shouldThrowOnUnreachable() {
        assertThrows<ErpioneRestException> {
            ErpioneClientFactory
                .forRemote("http://127.0.0.1").build() // missing the port!!
        }
    }
}
