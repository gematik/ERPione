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

package de.gematik.erpione.data.r4.kbv

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue

class EPatientTest {

    @Test
    fun `should serialize with Dates`() {
        val om = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        val patient = KbvEPatientIn(
            birthDate = Date.from(
                LocalDate.of(1986, 6, 10)
                    .atTime(12, 0)
                    .atZone(ZoneId.systemDefault()).toInstant(),
            ),
        )

        val json = om.writeValueAsString(patient)
        val patient2 = om.readValue(json, KbvEPatientIn::class.java)

        val duration = Duration.between(patient2.birthDate?.toInstant(), patient.birthDate?.toInstant()).abs()
        val maxDiff = Duration.of(24, ChronoUnit.HOURS).compareTo(duration)
        assertTrue(maxDiff > 0)
    }
}
