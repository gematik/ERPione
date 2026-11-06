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

package de.gematik.erpione.cli.factory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.gematik.erpione.client.ErpioneClientFactory
import kotlinx.serialization.Serializable
import java.io.File

object ErpioneCliConfigurationFactory {

    val configurationFile = listOf(
        System.getProperty("user.home"),
        System.getProperty("user.dir"),
    )
        .map { File("$it/.erpione/erpione-cli.json") }
        .lastOrNull { it.exists() && it.isFile }

    val configurationAsString = configurationFile
        ?.readText()
        ?: ErpioneCliConfigurationFactory.javaClass.getResourceAsStream("/erpione-cli.json")
            ?.bufferedReader()
            ?.readLines()
            ?.joinToString(separator = "\n")
        ?: "{}"

    fun readConfiguration(): ErpioneCliConfiguration = ObjectMapper().registerKotlinModule().readValue(configurationAsString, ErpioneCliConfiguration::class.java)

    fun getEnvironmentConfig(env: String): CliEnvironmentConfig = readConfiguration().envs
        .firstOrNull {
            val name = it.name.replace(Regex("[^a-z]"), "")
            val given = env.replace(Regex("[^a-z]"), "")
            given.startsWith(name, true)
        }
        ?: throw IllegalArgumentException("Environment '$env' not found in configuration")

    fun connectClientFactory(env: String): ErpioneClientFactory {
        val config = getEnvironmentConfig(env)

        val apikey: String = System.getenv("ERPIONE_API_KEY") ?: ""
        return ErpioneClientFactory.forRemote(config.url).apiKey(apikey).build()
    }
}

@Serializable
data class ErpioneCliConfiguration(
    val envs: List<CliEnvironmentConfig>,
    val out: OutputConfiguration = OutputConfiguration(),
)

@Serializable
data class CliEnvironmentConfig(
    val name: String,
    val url: String,
)

@Serializable
data class OutputConfiguration(val pretty: Boolean = false)
