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

import de.gematik.erpione.data.r4.kbv.KbvEPatientIn
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionIn
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.encodedPath
import java.util.*
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertNotNull

class PrescribeRequestTest {

    @Test
    fun shouldSerializeWithDate() {
        val rb = HttpRequestBuilder()
        val prescription = KbvEPrescriptionIn(
            patient = KbvEPatientIn(
                birthDate = Date(),
            ),
        )
        val request = DoctorRequests.prescribe(prescription)
        request.finalizeRequest(rb, "machaon")
        assert(rb.url.encodedPath == "erp/machaon/pharmaceutical")
        assert(rb.method.value == "POST")
        assertNotNull(rb.body)
    }

    @Test
    fun shouldSerializeRandomPrescription() {
        val rb = HttpRequestBuilder()
        val request = DoctorRequests.prescribe(kvnr = "X123123123", true)
        request.finalizeRequest(rb)
        // Hades is the default mnemonic if don't specify one
        // hades will fail on backend but recognizable as this specific issue
        assert(rb.url.encodedPath == "erp/Hades/pharmaceutical")
        assert(rb.method.value == "POST")
        assertNotNull(rb.body)
    }

    @Test
    fun shouldSerializeFromXmlFile() {
        val rb = HttpRequestBuilder()
        val templateFile = Path(PrescribeRequestTest::class.java.getResource("/logback-test.xml")!!.toURI().path)
        val request = DoctorRequests.prescribeKbvBundle(templateFile, true)

        request.finalizeRequest(rb, "Machaon")

        assert(rb.url.encodedPath == "erp/Machaon/pharmaceutical")
        assert(rb.method.value == "POST")
        assertNotNull(rb.body)
    }
}
