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

package de.gematik.erpione.server.jpa;

import de.gematik.erpione.data.error.EmptyData;
import de.gematik.erpione.data.r4.gem.ErpEReceiptOut;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.server.jpa.entity.gem.ErpEReceiptEntity;
import de.gematik.erpione.server.jpa.entity.gem.ErpETaskEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

public interface ErpionePersistence {
  @Transactional
  ErpETask save(ErpETask task);

  @Transactional
  ErpEReceiptOut save(ErpEReceiptOut task);

  void purgeExpiredPrescriptions();

  EmptyData deletePrescription(String prescriptionId);

  Optional<ErpETask> getPrescriptionById(String prescriptionId);

  Optional<ErpEReceiptOut> getReceiptById(String prescriptionId);

  Page<ErpEReceiptEntity> getReceipts(Pageable pageable);

  ErpETask getPrescriptionByIdOrThrow(String prescriptionId);

  Page<ErpETask> searchPrescriptions(Specification<ErpETaskEntity> spec, Pageable pageable);
}
