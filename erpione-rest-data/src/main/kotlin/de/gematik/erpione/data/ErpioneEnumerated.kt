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

package de.gematik.erpione.data

import de.gematik.erpione.data.exception.InvalidCodeValueException
import java.util.*

/**
 * Base interface for all ERPIONE base types which are enumerated types in the system.
 *
 *
 * This interface defines a method to retrieve the name of the type. It is intended to be
 * implemented by all base types in the ERPIONE system.
 */
interface ErpioneEnumerated<E : Enum<E>> {

    companion object {
        @JvmStatic
        fun <E : ErpioneEnumerated<E>> fromName(type: Class<E>, input: String?): E = type.enumConstants
            .find { (it as Enum<*>).name.equals(input, true) }
            ?: throw InvalidCodeValueException(type, input)
    }
}
