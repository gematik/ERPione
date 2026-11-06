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

package de.gematik.erpione.data.r4.gem

import de.gematik.bbriccs.utils.ResourceLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedicationDispenseEBundleTest {

    val om: ObjectMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @ParameterizedTest
    @MethodSource("provideExampleData")
    fun `should deserialize from file`(content: String) {
        val bundle = om.readValue(content, MedicationDispenseEBundleIn::class.java)
        assertEquals(1, bundle.dispensation.size)
    }

    fun provideExampleData(): Stream<Arguments> = ResourceLoader.readFilesFromDirectory("examples/dispensation")
        .map { Arguments.of(it) }
        .stream()
}
