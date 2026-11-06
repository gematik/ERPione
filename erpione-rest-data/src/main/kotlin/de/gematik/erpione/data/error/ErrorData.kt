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

package de.gematik.erpione.data.error

import de.gematik.erpione.data.ErpioneOutputData

data class ErrorData(
    var message: String = "unknown error",
    var type: ErrorType? = null,
    val issues: MutableList<IssueData> = mutableListOf(),
) : ErpioneOutputData {

    companion object {
        @JvmStatic
        fun internalError(message: String): ErrorDtoBuilder = ErrorDtoBuilder(ErrorType.ERPIONE, message)

        @JvmStatic
        fun fachdienstError(message: String): ErrorDtoBuilder = ErrorDtoBuilder(ErrorType.FACHDIENST, message)

        class ErrorDtoBuilder(
            val type: ErrorType,
            val message: String,
        ) {
            fun withoutIssues(): ErrorData = withIssues()

            fun withIssues(vararg issues: IssueData): ErrorData = withIssues(mutableListOf(*issues))

            fun withIssues(issues: MutableList<IssueData>): ErrorData = ErrorData(message, type, issues)
        }
    }
}
