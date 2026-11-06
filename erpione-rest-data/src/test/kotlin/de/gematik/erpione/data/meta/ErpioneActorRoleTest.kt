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

import de.gematik.erpione.data.actor.ActorERole
import de.gematik.erpione.data.actor.ActorERole.Companion.optionalFromString
import de.gematik.erpione.data.actor.InstitutionERole
import de.gematik.erpione.data.exception.InvalidActorRoleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class ErpioneActorRoleTest {
    @ParameterizedTest
    @ValueSource(strings = ["Arzt", "arzt", "Doctor", "doctor"])
    fun shouldMapValidDoctorRoles(input: String?) {
        assertEquals(ActorERole.DOCTOR, ActorERole.fromString(input))
    }

    @ParameterizedTest
    @ValueSource(strings = ["Apotheker", "apotheker", "Pharmacist", "pharmacist"])
    fun shouldMapValidPharmacistRoles(input: String?) {
        assertEquals(ActorERole.PHARMACIST, ActorERole.fromString(input))
    }

    @ParameterizedTest
    @ValueSource(strings = ["KTR", "ktr", "kostenträger"])
    fun shouldMapValidKtrRoles(input: String?) {
        assertEquals(InstitutionERole.HEALTH_INSURANCE, InstitutionERole.fromString(input))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Artz", "Apoteke", "TEST"])
    fun shouldThrowOnInvalidActorRoles(input: String?) {
        assertThrows(
            InvalidActorRoleException::class.java,
        ) { ActorERole.fromStringOrThrow(input) }
        assertFalse(optionalFromString(input).isPresent)
    }

    @Test
    fun shouldReturnReadable() {
        assertNotNull(ActorERole.DOCTOR.toString())
    }

    @Test
    fun createValidOptionalDoctor() {
        assertTrue(optionalFromString("Doctor").isPresent)
    }
}
