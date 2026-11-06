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

package de.gematik.erpione.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.platform.MultiplatformSystem.exitProcess
import de.gematik.erpione.cli.Logging
import de.gematik.erpione.cli.actorOption
import de.gematik.erpione.cli.environmentOption
import de.gematik.erpione.cli.factory.ErpioneCliConfiguration
import de.gematik.erpione.cli.factory.ErpioneCliConfigurationFactory
import de.gematik.erpione.cli.print.ConsolePrinter
import de.gematik.erpione.cli.print.ErpionePrinter
import de.gematik.erpione.client.ErpioneClientFactory
import de.gematik.erpione.client.ErpioneRestException
import de.gematik.erpione.client.rest.ErpioneActorRequest

abstract class ErpioneCommand(name: String) :
    CliktCommand(name = name),
    Logging {
    val verbose by
        option(
            "-v",
            "--verbose",
            hidden = true,
            help = "Enable verbose output with additional information",
        ).counted()
    val env by OptionGroup().environmentOption()
    val configuration: ErpioneCliConfiguration by
        lazy { ErpioneCliConfigurationFactory.readConfiguration() }
    val printer: ErpionePrinter = ConsolePrinter(this)
}

abstract class OfflineCommand(name: String) : ErpioneCommand(name)

abstract class RemoteCommand(name: String) : ErpioneCommand(name) {

    lateinit var factory: ErpioneClientFactory

    override fun run() {
        factory = ErpioneCliConfigurationFactory.connectClientFactory(env)
        runCommand(factory)
    }

    abstract fun runCommand(client: ErpioneClientFactory)
}

abstract class ActorRemoteCommand(name: String) : RemoteCommand(name) {
    val mnemonicId by actorOption()

    fun <T : Any> perform(request: ErpioneActorRequest<T>, exitOnError: Boolean = true): T? {
        try {
            val doc = mnemonicId?.let { factory.getDoctor(it) }
                ?: run {
                    factory.getRandomDoctor()
                }
            return doc.performBlockingOrThrow(request)
        } catch (e: ErpioneRestException) {
            printer.error(e)
            if (exitOnError) exitProcess(1)

            // TODO: custom exit status code
            // https://ajalt.github.io/clikt/advanced/#custom-exit-status-codes
            return null
        }
    }
}
