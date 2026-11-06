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

package de.gematik.erpione.server.util;

import de.gematik.erpione.data.r4.etype.PrescriptionEFlow;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionOut;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.kbv.*;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import java.util.Date;
import java.util.UUID;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TaskCreator {

  @Autowired private HermesService hermes;

  public ErpETask createNewTask() {
    return createNewTask(GemFaker.fakerValueSet(PrescriptionFlowType.class));
  }

  public ErpETask createNewTask(PrescriptionFlowType flowType) {
    return createNewTask(PrescriptionId.random(flowType));
  }

  public ErpETask createNewTask(String prescriptionId) {
    return createNewTask(PrescriptionId.from(prescriptionId));
  }

  public ErpETask createNewTask(PrescriptionId prescriptionId) {
    val tid = prescriptionId.getValue();
    val flowTypeDto = PrescriptionEFlow.fromString(prescriptionId.getFlowType().getCode());
    val taskStatus = PrescriptionEStatus.READY;

    KbvEPrescriptionOut pd;
    if (prescriptionId.getFlowType() == PrescriptionFlowType.FLOW_TYPE_162) {
      val patient = KbvPatientFaker.builder().fake();
      val har = KbvHealthAppRequestFaker.forPatient(patient).fake();
      // TODO: KbvEvdgaBundleFaker is missing, use KbvEvdgaBundleBuilder instead
      val prescription =
          KbvEvdgaBundleBuilder.forPrescription(prescriptionId)
              .healthAppRequest(har)
              .patient(patient)
              .insurance(KbvCoverageFaker.builder().fake())
              .medicalOrganization(KbvMedicalOrganizationFaker.medicalPractice().fake())
              .practitioner(KbvPractitionerFaker.builder().fake())
              .build();
      pd = hermes.toJson(KbvEPrescriptionOut.class, prescription);
    } else {
      val prescription = KbvErpBundleFaker.builder().withPrescriptionId(prescriptionId).fake();
      pd = hermes.toJson(KbvEPrescriptionOut.class, prescription);
    }

    val accessCode = AccessCode.random().getValue();
    val authoredOn = new Date();
    val expiryDate = GemFaker.fakerFutureExpirationDate();
    val acceptDate = GemFaker.fakerFutureExpirationDate();

    return new ErpETask(
        tid,
        flowTypeDto,
        taskStatus,
        accessCode,
        null,
        authoredOn,
        null,
        expiryDate,
        acceptDate,
        pd,
        null,
        UUID.randomUUID().toString(),
        null);
  }
}
