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

package de.gematik.erpione.data.error

import de.gematik.erpione.data.error.ErrorData.Companion.fachdienstError
import de.gematik.erpione.data.error.ErrorData.Companion.internalError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ErrorDataTest {
    @Test
    fun shouldCreateInternalError() {
        val dto = internalError("error message").withoutIssues()
        assertEquals(ErrorType.ERPIONE, dto.type)
    }

    @Test
    fun shouldCreateFachdienstError() {
        val dto = fachdienstError("error message").withoutIssues()
        assertEquals(ErrorType.FACHDIENST, dto.type)
    }
}
