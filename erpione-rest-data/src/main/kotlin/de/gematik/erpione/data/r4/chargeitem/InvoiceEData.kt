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

package de.gematik.erpione.data.r4.chargeitem

import de.gematik.erpione.data.ErpioneInputData
import de.gematik.erpione.data.ErpioneOutputData

/**
 * represents a invoice data
 *
 * @see {link: http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PKV-PR-ERP-Abrechnungszeilen} for details
 */
interface InvoiceEData<
    L : InvoiceEDataLine<PC>,
    PC : PriceEComponent,
    TG : TotalEGross,
    > {
    val currency: String?
    val type: InvoiceEType?
    val total: TG?
    val items: List<L>
}

/**
 * represents a single data line in the invoice
 */
interface InvoiceEDataLine<PC : PriceEComponent> {
    val sequence: Int?
    val name: String?
    val code: String?
    val codeSystem: String?
    val priceComponent: PriceEComponent?
}

data class InvoiceEDataIn(
    override val currency: String? = null,
    override val type: InvoiceEType? = null,
    override val total: TotalEGrossIn? = null,
    override var items: List<InvoiceEDataLineIn> = mutableListOf(),
) : InvoiceEData<InvoiceEDataLineIn, PriceEComponentIn, TotalEGrossIn>,
    ErpioneInputData

data class InvoiceEDataOut(
    override val currency: String,
    override val type: InvoiceEType,
    override val total: TotalEGrossOut,
    override val items: List<InvoiceEDataLineOut>,
) : InvoiceEData<InvoiceEDataLineOut, PriceEComponentOut, TotalEGrossOut>,
    ErpioneOutputData

data class InvoiceEDataLineIn(
    override val sequence: Int? = null,
    override val name: String? = null,
    override val codeSystem: String? = null,
    override val code: String? = null,
    override val priceComponent: PriceEComponentIn? = null,
) : InvoiceEDataLine<PriceEComponentIn>,
    ErpioneInputData

data class InvoiceEDataLineOut(
    override val sequence: Int,
    override val name: String,
    override val codeSystem: String,
    override val code: String,
    override val priceComponent: PriceEComponentOut,
) : InvoiceEDataLine<PriceEComponentOut>,
    ErpioneOutputData
