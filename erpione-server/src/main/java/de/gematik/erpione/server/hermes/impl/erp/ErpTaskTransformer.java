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

package de.gematik.erpione.server.hermes.impl.erp;

import de.gematik.bbriccs.fhir.coding.SemanticValue;
import de.gematik.erpione.data.r4.etype.PrescriptionEFlow;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.erp.ErxTask;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ErpTaskTransformer implements FhirToJsonTransformer<ErpETask, ErxTask> {

  private final HermesService hermes;

  @Override
  public ErpETask toJson(ErxTask task) {
    val taskId = task.getPrescriptionId().getValue();
    val ft = PrescriptionEFlow.fromString(task.getFlowType().getCode());
    val status = PrescriptionEStatus.fromString(task.getStatus().name());
    val ac = task.getAccessCode().getValue();
    val secret = task.getSecret().map(SemanticValue::getValue).orElse(null);
    val authoredOn = task.getAuthoredOn();
    val lmd = task.getLastMedicationDispenseDate().map(Date::from).orElse(null);
    val expiry = task.getExpiryDate();
    val accept = task.getAcceptDate();

    // Note: set an empty prescription, will be filled later along the usecase execution
    return new ErpETask(
        taskId, ft, status, ac, secret, authoredOn, lmd, expiry, accept, null, null, null, null);
  }

  @Override
  public Class<ErpETask> jsonType() {
    return ErpETask.class;
  }

  @Override
  public Class<ErxTask> fhirType() {
    return ErxTask.class;
  }
}
