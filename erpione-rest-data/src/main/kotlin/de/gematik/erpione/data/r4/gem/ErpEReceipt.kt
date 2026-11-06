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

package de.gematik.erpione.data.r4.gem

import com.fasterxml.jackson.annotation.JsonFormat
import de.gematik.erpione.data.ErpioneOutputData
import de.gematik.erpione.data.r4.etype.DocumentEType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

interface ErpEReceipt {
    @get:Schema(
        title = "Prescription ID",
        type = "string",
        example = "160.100.000.000.006.24",
    )
    val prescriptionId: String?
    val type: DocumentEType?

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss")
    @get:Schema(
        title = "Date of the Signature",
        type = "date",
        example = "17.10.2025 11:22:00",
    )
    val signatureDate: Date?
    val digest: String?
    val qes: String?
}

data class ErpEReceiptOut(
    override var prescriptionId: String,
    override var type: DocumentEType,
    override var signatureDate: Date,
    override var digest: String,
    override var qes: String,
) : ErpEReceipt,
    ErpioneOutputData
