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

package de.gematik.erpione.cli

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import de.gematik.erpione.cli.cmd.ActorsInformationCommand
import de.gematik.erpione.cli.cmd.CliConfigCommand
import de.gematik.erpione.cli.cmd.InfoCommand
import de.gematik.erpione.cli.cmd.PrescribeCommand
import de.gematik.erpione.client.ErpioneRestException
import java.net.ConnectException

class ERPione :
    NoOpCliktCommand(name = "erpione"),
    Logging {

    init {
        context {
            helpFormatter = {
                MordantHelpFormatter(it, showDefaultValues = true, requiredOptionMarker = "*")
            }
            terminal = Terminal(ansiLevel = AnsiLevel.TRUECOLOR, interactive = true)

            // Only suggest corrections that start with the entered value
            suggestTypoCorrection = { enteredValue, possibleValues ->
                possibleValues.filter { it.startsWith(enteredValue) }
            }

            completionOption(hidden = true)
                .subcommands(
                    InfoCommand(),
                    ActorsInformationCommand(),
                    PrescribeCommand(),
                    CliConfigCommand(),
                )
        }
    }
}

fun main(args: Array<String>) {
    val erpione = ERPione()
    val logger = erpione.logger()

    try {
        erpione.main(args)
    } catch (e: ErpioneRestException) {
        error("Request error: ${e.message ?: "Unknown error"}")
    } catch (e: IllegalArgumentException) {
        logger.error("Illegal Argument", e)
        error("Illegal argument: ${e.message ?: "Unknown error"}")
    } catch (e: ConnectException) {
        logger.error("Connection error: ${e.message}", e)
        error("Connection error: ${e.message}")
    } catch (e: Exception) {
        logger.error("An unexpected error occurred", e)
        error("An unexpected error occurred!")
    }
}
