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

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.erpione.client.ErpioneClientFactory
import de.gematik.erpione.client.rest.DoctorRequests
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.io.path.walk

class PrescribeCommand : ActorRemoteCommand(name = "prescribe") {
    private val directAssignment by
        option(
            "-d",
            "--direct",
            help = "Prescribe as 'Direktzuweisung' resulting in 169/209 Workflows",
        )
            .flag(default = false)
    private val xlsx by
        option("-x", "--xlsx", help = "Write prescription information into xlsx-summary file")
            .flag(default = false)
    private val source: Path by
        argument().path(canBeFile = true, mustExist = true, mustBeReadable = true)

    @OptIn(ExperimentalPathApi::class)
    override fun runCommand(client: ErpioneClientFactory) {
        val files =
            when (source.isDirectory()) {
                true -> source.walk(PathWalkOption.BREADTH_FIRST).map { it.toFile() }.toList()
                false -> listOf(source.toFile())
            }

        if (files.isEmpty()) error("No files were found in the specified source path: ${source.pathString}")

        val totalAmount = files.size
        val summary =
            files
                .mapIndexed { index, file ->
                    if (verbose > 0) echo("(${index + 1}|$totalAmount) Prescribing ${file.path}")
                    val request = DoctorRequests.prescribeKbvBundle(file, directAssignment)
                    this.perform(request, exitOnError = false)
                }
                .filterNotNull()
        printer.info(summary)
    }
}
