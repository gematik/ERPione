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

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import de.gematik.erpione.data.actor.ActorERole
import de.gematik.erpione.data.actor.ErpioneActorData
import de.gematik.erpione.data.actor.ErpioneInstitutionData
import de.gematik.erpione.data.actor.InstitutionERole
import de.gematik.erpione.data.error.ErrorData
import de.gematik.erpione.data.error.ErrorType
import de.gematik.erpione.data.meta.ErpioneBuildInfo
import de.gematik.erpione.data.meta.ErpioneFhirInfo
import de.gematik.erpione.data.meta.ErpioneServiceInfo
import de.gematik.erpione.data.meta.TIConnectionData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import tools.jackson.databind.ObjectMapper

open class RestTest {

    @RegisterExtension
    val wiremockExtension: WireMockExtension = WireMockExtension.newInstance()
        .options(options().dynamicPort())
        .build()

    val om = ObjectMapper()
    var port: Int = 0

    @BeforeEach
    fun setup() {
        port = wiremockExtension.port
    }

    fun setupPositiveInfo(): StubMapping {
        val infoDto = ErpioneServiceInfo(
            build = ErpioneBuildInfo.fromEnvironment(),
            fhir = ErpioneFhirInfo(view = "Test Profile View"),
            ti = TIConnectionData(),
        )

        val info = om.writeValueAsString(infoDto)

        val testUrl = "/api/info"
        return wiremockExtension.stubFor(
            get(urlPathEqualTo(testUrl)).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(info),
            ),
        )
    }

    fun setupErrorInfo(errorType: ErrorType = ErrorType.ERPIONE): StubMapping {
        val error = om.writeValueAsString(ErrorData("internal error", errorType))
        return wiremockExtension.stubFor(
            get(urlPathEqualTo("/api/meta/erpione")).willReturn(
                aResponse().withStatus(400).withHeader("Content-Type", "application/json").withBody(error),
            ),
        )
    }

    fun setupPositiveActors(): StubMapping {
        val data = mutableListOf<ErpioneInstitutionData>()
        for (i in 0..1) {
            data.add(
                ErpioneInstitutionData(
                    mnemonic = "Doctor $i",
                    mnemonicId = "doc$i",
                    role = InstitutionERole.HEALTH_FACILITY,
                ).apply {
                    practitioner = ErpioneActorData("doc$i", ActorERole.DOCTOR)
                },
            )
        }
        for (i in 0..1) {
            val actorInfo = ErpioneInstitutionData(
                mnemonic = "Pharmacist $i",
                mnemonicId = "pharm$i",
                role = InstitutionERole.HEALTH_FACILITY,
            ).apply {
                practitioner = ErpioneActorData("pharm$i", ActorERole.PHARMACIST)
            }
            data.add(actorInfo)

            wiremockExtension.stubFor(
                get(urlPathEqualTo("/api/actors/pharm$i")).willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(om.writeValueAsString(actorInfo)),
                ),
            )
        }

        val actors = om.writeValueAsString(data)
        val testUrl = "/api/actors"
        return wiremockExtension.stubFor(
            get(urlPathEqualTo(testUrl)).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(actors),
            ),
        )
    }

    fun setupErrorActors(errorType: ErrorType = ErrorType.ERPIONE): StubMapping {
        val error = om.writeValueAsString(ErrorData("internal error", errorType))
        return wiremockExtension.stubFor(
            get(urlPathEqualTo("/api/actors")).willReturn(
                aResponse().withStatus(400).withHeader("Content-Type", "application/json").withBody(error),
            ),
        )
    }

    fun setupErrorActorsHtml(): StubMapping {
        val error = "<html><body>internal error</body></html>"
        return wiremockExtension.stubFor(
            get(urlPathEqualTo("/api/actors")).willReturn(
                aResponse().withStatus(400).withHeader("Content-Type", "text/html").withBody(error),
            ),
        )
    }

    fun setupErrorActorsEmpty(): StubMapping = wiremockExtension.stubFor(
        get(urlPathEqualTo("/api/actors")).willReturn(
            aResponse().withStatus(400).withHeader("Content-Type", "application/json"),
        ),
    )

    fun setupActorsInvalidErrorResponse(statusCode: Int): StubMapping = wiremockExtension.stubFor(
        get(urlPathEqualTo("/api/actors")).willReturn(
            aResponse().withStatus(statusCode).withHeader("Content-Type", "application/json")
                .withHeader("content-length", "20").withBody("{'a': 'b'}"),
        ),
    )

    fun setupActorsValidErrorResponse(): StubMapping {
        val errorJson = "{\"type\": \"ERPIONE\", \"message\": \"Test Message\"}"
        return wiremockExtension.stubFor(
            get(urlPathEqualTo("/api/actors")).willReturn(
                aResponse().withStatus(400).withHeader("Content-Type", "application/json")
                    .withHeader("content-length", errorJson.length.toString()).withBody(errorJson),
            ),
        )
    }

    fun setupConnectionTimeout(delayMilliSeconds: Int): StubMapping = wiremockExtension.stubFor(
        get(urlPathEqualTo("/api/info")).willReturn(
            aResponse().withStatus(200).withFixedDelay(delayMilliSeconds),
        ),
    )

    fun setupMetaStubs() {
        setupPositiveInfo()
        setupPositiveActors()
    }
}
