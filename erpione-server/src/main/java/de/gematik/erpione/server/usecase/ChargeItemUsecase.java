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

package de.gematik.erpione.server.usecase;

import static java.text.MessageFormat.format;
import static java.util.Optional.ofNullable;

import com.ibm.icu.impl.Pair;
import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.erpione.data.r4.chargeitem.ErpEChargeItemOut;
import de.gematik.erpione.data.r4.chargeitem.InvoiceEDataOut;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.error.ImpossibleOperationError;
import de.gematik.test.erezept.client.usecases.ChargeItemGetByIdCommand;
import de.gematik.test.erezept.client.usecases.ChargeItemPostCommand;
import de.gematik.test.erezept.client.usecases.ChargeItemPutCommand;
import de.gematik.test.erezept.fhir.builder.dav.DavPkvAbgabedatenBuilder;
import de.gematik.test.erezept.fhir.builder.dav.DavPkvAbgabedatenFaker;
import de.gematik.test.erezept.fhir.builder.dav.DavPkvDispensedMedicationBuilder;
import de.gematik.test.erezept.fhir.builder.dav.PharmacyOrganizationFaker;
import de.gematik.test.erezept.fhir.builder.erp.ErxChargeItemBuilder;
import de.gematik.test.erezept.fhir.r4.dav.DavInvoice;
import de.gematik.test.erezept.fhir.r4.dav.DavPkvAbgabedatenBundle;
import de.gematik.test.erezept.fhir.r4.dav.DavPkvDispensedMedication;
import de.gematik.test.erezept.fhir.r4.dav.PharmacyOrganization;
import de.gematik.test.erezept.fhir.r4.erp.ErxChargeItem;
import de.gematik.test.erezept.fhir.r4.erp.ErxReceipt;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.Secret;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ChargeItemUsecase
    implements ErpioneActorUsecase<HealthcareInstitution, ErpEChargeItemOut> {
  private final PrescriptionId prescriptionId;
  private final DavInvoice invoice;
  private final String mode;
  private final AccessCode chargeItemAc;

  public static ChargeItemUsecase createForPrescription(String prescriptionId, DavInvoice invoice) {
    return new ChargeItemUsecase(PrescriptionId.from(prescriptionId), invoice, "post", null);
  }

  public static ChargeItemUsecase updateForPrescription(
      String prescriptionId, DavInvoice invoice, String chargeItemAc) {
    return new ChargeItemUsecase(
        PrescriptionId.from(prescriptionId), invoice, "put", AccessCode.from(chargeItemAc));
  }

  public static ChargeItemUsecase getForPrescription(String prescriptionId, String chargeItemAc) {
    return new ChargeItemUsecase(
        PrescriptionId.from(prescriptionId), null, "get", AccessCode.from(chargeItemAc));
  }

  /**
   * Executes the use case with the given actor and data.
   *
   * @param actor the actor performing the charge item use case, usually a pharmacy
   * @return a result of type D, which is the output of the use case
   */
  @Override
  public ErpEChargeItemOut performAs(HealthcareInstitution actor) {
    return switch (mode) {
      case "get" -> getChargeItem(actor);
      case "post" -> postChargeItem(actor);
      case "put" -> putChargeItem(actor);
      default -> throw new ImpossibleOperationError(
          format("Mode {0} is not supported for ChargeItemUsecase", mode));
    };
  }

  private ErpEChargeItemOut getChargeItem(HealthcareInstitution actor) {
    val cmd = new ChargeItemGetByIdCommand(prescriptionId, chargeItemAc);

    val chargeItemBundle = actor.requestAndUnpack(prescriptionId, cmd);
    val chargeItem = chargeItemBundle.getChargeItem();
    val davAbgabeBundle = chargeItemBundle.getAbgabedatenBundle();

    return mapToErpioneChargeItem(chargeItem, davAbgabeBundle.getInvoice(), actor);
  }

  private ErpEChargeItemOut postChargeItem(HealthcareInstitution actor) {
    val task = actor.getRepository().getPrescriptionByIdOrThrow(prescriptionId.getValue());
    val chargeItemPair = this.createChargeItem(actor, task);
    val chargeItemRequested = chargeItemPair.first;
    val davAbgabeBundle = chargeItemPair.second;
    val secret =
        ofNullable(task.getSecret())
            .map(Secret::from)
            .orElseThrow(
                () ->
                    new ImpossibleOperationError(
                        format(
                            "Prescription with id {0} has no secret", prescriptionId.getValue())));

    val cmd = new ChargeItemPostCommand(chargeItemRequested, secret);
    val chargeItem = actor.requestAndUnpack(prescriptionId, cmd);

    return mapToErpioneChargeItem(chargeItem, davAbgabeBundle.getInvoice(), actor);
  }

  private ErpEChargeItemOut putChargeItem(HealthcareInstitution actor) {
    val getCmd = new ChargeItemGetByIdCommand(prescriptionId, chargeItemAc);

    val chargeItemBundle = actor.requestAndUnpack(prescriptionId, getCmd);
    val originalChargeItem = chargeItemBundle.getChargeItem();

    val pharmacyOrg =
        PharmacyOrganizationFaker.builder().withName(actor.getInfo().getName()).fake();
    val newDavBundle = createAbgabedatenBundle(prescriptionId.getValue(), invoice, pharmacyOrg);
    val davXml = actor.getHermes().encode(newDavBundle, EncodingType.XML);

    val newChargeItem =
        originalChargeItem.withChangedContainedBinaryData(
            newDavBundle.getReference(), actor.signDocument(davXml));

    val putCmd = new ChargeItemPutCommand(prescriptionId, chargeItemAc, newChargeItem);
    val chargeItem = actor.requestAndUnpack(prescriptionId, putCmd);

    return mapToErpioneChargeItem(chargeItem, newDavBundle.getInvoice(), actor);
  }

  private ErpEChargeItemOut mapToErpioneChargeItem(
      ErxChargeItem chargeItem, DavInvoice davInvoice, HealthcareInstitution actor) {
    // aggregate the outputs mapped by hermes
    val pid = prescriptionId.getValue();
    val tidEnterer = chargeItem.getEntererTelematikId().getValue();
    val enteredDate = chargeItem.getEnteredDate();
    val kvnr = chargeItem.getSubjectKvnr().getValue();

    // extract the invoice data from the request because the FD doesn't mirror this one
    val jsonInvoice = actor.getHermes().toJson(InvoiceEDataOut.class, davInvoice);

    return new ErpEChargeItemOut(enteredDate, pid, kvnr, tidEnterer, jsonInvoice);
  }

  private Pair<ErxChargeItem, DavPkvAbgabedatenBundle> createChargeItem(
      HealthcareInstitution actor, ErpETask task) {
    val prescribed =
        ofNullable(task.getPrescription())
            .orElseThrow(
                () ->
                    new ImpossibleOperationError(
                        format(
                            "Prescription with id {0} has no prescription information",
                            prescriptionId.getValue())));

    val prescriptionReference =
        ofNullable(task.getPrescriptionReference())
            .map(KbvErpBundle::asReferenceFromId)
            .orElseThrow(
                () ->
                    new ImpossibleOperationError(
                        format(
                            "Prescription with id {0} has no prescription reference",
                            prescriptionId.getValue())));
    val receiptReference =
        ofNullable(task.getReceiptReference())
            .map(ErxReceipt::asReferenceFromId)
            .orElseThrow(
                () ->
                    new ImpossibleOperationError(
                        format(
                            "Prescription with id {0} has no receipt reference",
                            prescriptionId.getValue())));

    val kvnr = prescribed.getPatient().getKvnr();
    val coverageName = prescribed.getCoverage().getName();

    val pharmacyName = actor.getInfo().getName();
    val org = PharmacyOrganizationFaker.builder().withName(pharmacyName).fake();
    val medication =
        DavPkvDispensedMedicationBuilder.builder()
            .prescription(prescriptionId)
            .invoice(invoice)
            .build();
    val davBundle =
        DavPkvAbgabedatenFaker.builder(prescriptionId)
            .withDispensedMedication(medication, org, invoice)
            .fake();

    val signed = actor.signDocument(davBundle);

    val chargeItem =
        ErxChargeItemBuilder.forPrescription(prescriptionId)
            .status("billable")
            .enterer(actor.getTelematikId())
            .subject(KVNR.from(kvnr), coverageName)
            .receipt(receiptReference)
            .verordnung(prescriptionReference)
            .abgabedatensatz(davBundle.getReference(), signed)
            .build();

    return Pair.of(chargeItem, davBundle);
  }

  private DavPkvAbgabedatenBundle createAbgabedatenBundle(
      String prescriptionId, DavInvoice davInvoice, PharmacyOrganization pharmacyOrg) {
    val dispMed = createDispensedMedicationFor(prescriptionId, davInvoice, pharmacyOrg);
    return DavPkvAbgabedatenBuilder.builder(PrescriptionId.from(prescriptionId))
        .invoice(davInvoice)
        .medication(dispMed)
        .pharmacy(pharmacyOrg)
        .build();
  }

  private DavPkvDispensedMedication createDispensedMedicationFor(
      String prescriptionId, DavInvoice davInvoice, PharmacyOrganization pharmacyOrg) {
    return DavPkvDispensedMedicationBuilder.builder()
        .prescription(PrescriptionId.from(prescriptionId))
        .pharmacy(pharmacyOrg)
        .invoice(davInvoice)
        .build();
  }
}
