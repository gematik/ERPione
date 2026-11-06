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

package de.gematik.erpione.client.rest

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.reflect.TypeInfo

interface ErpioneRequest<D> {
    val typeInfo: TypeInfo // abstract property to hold type information

    fun finalizeRequest(rb: HttpRequestBuilder, actorId: String? = null)
}

/**
 * Generic request for Erpione API which does not require any actor interaction.
 */
abstract class ErpioneGenericRequest<D>(override val typeInfo: TypeInfo) : ErpioneRequest<D>

/**
 * Specific request for Erpione API that requires interaction with an actor.
 */
abstract class ErpioneActorRequest<D>(override val typeInfo: TypeInfo) : ErpioneRequest<D> {

    /**
     * Provides a safe mnemonic for the actor, defaulting to "Hades" if none is provided.
     */
    protected fun safeMnemonic(actorId: String?): String = actorId ?: "Hades"
}
