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

package de.gematik.erpione.cli.print

import com.github.ajalt.clikt.core.parse
import de.gematik.erpione.cli.cmd.ErpioneCommand
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ConsolePrinterTest {

    @Test
    fun `should print single data`() {
        val cmd = TestCommand()
        cmd.parse(listOf())

        val printer = ConsolePrinter(cmd)
        val input = "Test String"
        assertDoesNotThrow { printer.info(input) }
    }

    @Test
    fun `should print error without throwing`() {
        val cmd = TestCommand()
        cmd.parse(listOf())

        val printer = ConsolePrinter(cmd)
        val error = Exception("Test Error")
        val text = "Test String"
        assertDoesNotThrow { printer.error(error, text) }
    }

    class TestCommand : ErpioneCommand("Test Command") {
        override fun run() {
            // This is a placeholder for the abstract class implementation
            // In a real scenario, this would contain logic to run the command
        }
    }
}
