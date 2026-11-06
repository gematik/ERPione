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

package de.gematik.erpione.data.r4.gem.com

import com.fasterxml.jackson.annotation.JsonFormat
import de.gematik.erpione.data.ErpioneData
import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date

interface CommunicationEData {
    val type: CommunicationEType

    @get:Schema(
        title = "Communication Payload message",
    )
    val message: CommunicationEPayload?

    @get:Schema(
        title = "Recipient",
        description = "ID (Telematik-ID or KVNR) of the recipient",
        type = "string",
        example = "L982864321",
    )
    val recipient: String?

    @get:Schema(
        title = "based on prescription reference",
        type = "string",
        example = "160.000.248.738.536.17",
    )
    val reference: CommunicationEReference?

    @get:Schema(
        title = "OrderID",
        description = "Identifier with system https://gematik.de/fhir/NamingSystem/OrderID, if present",
        type = "string",
    )
    val orderId: String?
}

data class CommunicationEDataIn(
    override val type: CommunicationEType = CommunicationEType.REPLY,
    val sender: String? = null,
    override val recipient: String? = null,
    override val message: CommunicationEPayload? = null,
    override var reference: CommunicationEReferenceIn? = null,
    override val orderId: String? = null,
) : CommunicationEData,
    ErpioneInputData

class CommunicationEDataOut(
    override val type: CommunicationEType,

    @get:Schema(
        title = "FHIR Resource ID",
        type = "string",
        example = "2be1c6ac-5d10-47f6-84ee-8318b2c22c76",
    )
    val rid: String,

    @get:Schema(
        title = "Sender",
        description = "ID (Telematik-ID or KVNR) of the sender",
        type = "string",
        example = "1-SMC-B-123456789",
    )
    val sender: String,
    override val recipient: String,
    override val message: CommunicationEPayload,
    override val reference: CommunicationEReferenceOut,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    val sent: Date,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    val received: Date? = null,
    override val orderId: String? = null,
) : CommunicationEData,
    ErpioneOutputData

data class CommunicationEDataBundleOut(
    val communications: List<CommunicationEDataOut> = emptyList(),
) : ErpioneData,
    ErpioneOutputData
