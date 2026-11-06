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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.gematik.erpione.cli.cmd.ActorRemoteCommand
import de.gematik.erpione.client.ErpioneClientFactory

fun CliktCommand.prettyPrintOption() = option("--pretty", help = "Pretty print the output").flag(default = false)

fun OptionGroup.environmentOption() = option("-e", "--env", envvar = "ERPIONE_ENV", help = "Set the environment")
    .default("RU")

fun OptionGroup.serverOption() = option("--url", help = "The URL of the backend service")
    .default("http://localhost:8080/")

fun OptionGroup.apiKeyOption() = option(envvar = "ERPIONE_API_KEY", help = "API-Key for accessing the backend service")
    .default("")

class ServerOptionGroup : OptionGroup(name = "Server Options", "Server configuration options") {
    val url by serverOption()
    val apiKey by apiKeyOption()
    val dev by option("--dev", hidden = true).flag(default = false)

    fun connectFactory(): ErpioneClientFactory {
        val factoryBuilder = ErpioneClientFactory.forRemote(url)
        if (!dev) {
            factoryBuilder.apiKey(apiKey)
        }
        return factoryBuilder.timeoutMillis(30_000).build()
    }
}

fun ActorRemoteCommand.actorOption() = option(
    "-a",
    "--actor",
    help = "The mnemonic id of the actor to be used for this operation (random by default)",
)

fun CliktCommand.kvnrOption() = option("--kvnr", help = "KVNR to be filtered by")

fun ParameterHolder.taskIdOption() = option("-t", "--taskid", help = "TaskId or PrescriptionId for the prescription")

fun CliktCommand.secretOption() = option("-s", "--secret", help = "Secret for an accepted prescription")

fun CliktCommand.accessCodeOption() = option("-a", "--accesscode", help = "AccessCode for a prescription")
