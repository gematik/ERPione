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

import de.gematik.erpione.cli.cmd.ErpioneCommand

interface ErpionePrinter {

    fun info(data: Any)

    fun info(data: List<Any>) = data.forEachIndexed { idx, it ->
        info("[${idx + 1}]" + "-".repeat(80))
        info(it)
    }

    fun error(e: Throwable, text: String? = null)

    fun error(text: String)
}

class ConsolePrinter(
    val cmd: ErpioneCommand,
    val formatter: ErpioneFormatter = DefaultErpioneFormatter(),
) : ErpionePrinter {

    override fun info(data: Any) {
        val text = formatter.encodeToString(data)
        cmd.echo(text, trailingNewline = true, err = false)
    }

    override fun error(e: Throwable, text: String?) {
        val errorMessage = e.message ?: e.toString()
        val errorText = text.let {
            if (it.isNullOrBlank()) errorMessage else "$it: $errorMessage"
        }
        error(errorText)
    }

    override fun error(text: String) {
        cmd.echo(text, trailingNewline = true, err = true)
    }
}
