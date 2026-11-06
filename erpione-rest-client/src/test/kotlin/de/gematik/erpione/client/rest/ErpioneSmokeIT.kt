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

import de.gematik.erpione.client.ErpioneClientFactory
import de.gematik.erpione.client.rest.BasicRequests.getActorByMnemonic
import de.gematik.erpione.client.rest.BasicRequests.getActorInformation
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Disabled("Integration test, requires API key and network access")
class ErpioneSmokeIT {

    val clientFactory = ErpioneClientFactory
        .forRemote("https://erpps-test.dev.gematik.solutions/api/")
        .apiKey(System.getenv("ERPIONE_API_KEY"))
        .timeoutMillis(30_000)
        .build()

    val doctor = clientFactory.getRandomDoctor()
    val pharmacist = clientFactory.getRandomPharmacist()

    @Test
    fun shouldGetBaseInformationOnStartup() {
        assertEquals(7, clientFactory.actorsInfo.size)

        // call meta-endpoint to get service information
        val infoResponse = clientFactory.performBlocking(InfoRequest()).asExpectedPayload()
        println(infoResponse)

        // call one actor's information
        val rndDoc = clientFactory.getRandomDoctor().performBlocking(getActorInformation()).asExpectedPayload()
        println("Random Doctor: $rndDoc")

        // call each actor by their mnemonic
        clientFactory.actorsInfo.forEach {
            val ad = clientFactory.performBlocking(getActorByMnemonic(it.mnemonic)).asExpectedPayload()
            println("Actor: $ad")
        }
    }

    @Test
    fun shouldDeleteClaimedPrescription() {
        var erpTask = doctor.performBlocking(DoctorRequests.prescribe("X110407071"))
            .asExpectedPayload()
        println("Created prescription: $erpTask")
        assertNotNull(erpTask.prescription?.prescriptionId)
        assertNotNull(erpTask.accessCode)
        assertNull(erpTask.secret)

        val prescription = assertDoesNotThrow { erpTask.prescription!! }
        val pid = prescription.prescriptionId
        val ac = erpTask.accessCode

        erpTask = pharmacist.performBlocking(PharmacistRequests.accept(pid, ac))
            .asExpectedPayload()
        println("Accepted prescription: $erpTask")
        assertNotNull(erpTask.secret)

        erpTask = pharmacist.performBlocking(PrescriptionRequests.byId(erpTask.taskId))
            .asExpectedPayload()
        println("Accepted(II) prescription: $erpTask")
        assertEquals(PrescriptionEStatus.INPROGRESS, erpTask.status)

        val secret = assertDoesNotThrow { erpTask.secret!! }
        pharmacist.performBlocking(PharmacistRequests.delete(pid, ac, secret))
            .asExpectedPayload()
        println("Deleted prescription: $pid")
    }
}
