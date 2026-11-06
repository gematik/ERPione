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

package de.gematik.erpione.data.actor

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import de.gematik.erpione.data.ErpioneValueSet
import de.gematik.erpione.data.exception.InvalidActorRoleException
import java.util.*

enum class ActorERole(
    @JsonValue override val display: String,
) : ErpioneValueSet<ActorERole> {
    DOCTOR("Arzt"),
    PHARMACIST("Apotheker"),
    ;

    override val code: String = this.name

    companion object {
        @JvmStatic
        fun fromString(input: String?): ActorERole? = ErpioneValueSet.fromString(ActorERole::class.java, input)

        @JvmStatic
        fun optionalFromString(input: String?): Optional<ActorERole> = Optional.ofNullable(fromString(input))

        @JvmStatic
        @JsonCreator
        fun fromStringOrThrow(input: String?): ActorERole = ErpioneValueSet.fromString(ActorERole::class.java, input)
            ?: throw InvalidActorRoleException(input)
    }
}
