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

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BuildInfoDataTest {

    @Test
    fun shouldNotThrowOnMissingBuildInfo() {
        assertDoesNotThrow { ErpioneBuildInfo.fromEnvironment() }
    }

    @Test
    fun shouldSerializeWithJackson() {
        val mapper = ObjectMapper()
        val info = ErpioneBuildInfo.fromEnvironment()
        val infoJson = mapper.writeValueAsString(info)

        println(infoJson)

        val info2 = mapper.readValue(infoJson, ErpioneBuildInfo::class.java)
        assertEquals(info, info2)
    }
}
