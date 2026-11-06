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
import de.gematik.erpione.server.jpa.error.EntityNotFoundError;
import de.gematik.erpione.server.jpa.mapper.gem.ErpEReceiptMapper;
import de.gematik.erpione.server.jpa.mapper.gem.ErpETaskMapper;
import de.gematik.erpione.server.jpa.repo.ErpReceiptRepository;
import de.gematik.erpione.server.jpa.repo.ErpTaskRepository;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ErpionePersistenceImpl implements ErpionePersistence {

  private final ErpTaskRepository taskRepository;
  private final ErpETaskMapper taskMapper;

  private final ErpReceiptRepository receiptRepository;
  private final ErpEReceiptMapper receiptMapper;

  public ErpionePersistenceImpl(
      ErpTaskRepository taskRepository, ErpReceiptRepository receiptRepository) {
    this.taskRepository = taskRepository;
    this.taskMapper = Mappers.getMapper(ErpETaskMapper.class);

    this.receiptRepository = receiptRepository;
    this.receiptMapper = Mappers.getMapper(ErpEReceiptMapper.class);
  }

  @Scheduled(cron = "0 0 2 * * *") // every night at 2:00 AM
  @Override
  public void purgeExpiredPrescriptions() {
    val expiredTasks = taskRepository.findByExpiryDateBefore(new Date());

    expiredTasks.forEach(
        task -> {
          log.info("Purging expired prescription with taskId: {}", task.getTaskId());
          taskRepository.delete(task);

          // remove the corresponding receipts as well
          receiptRepository
              .findErpReceiptEntitiesByPrescriptionId(task.getTaskId())
              .ifPresent(receiptRepository::delete);
        });
  }

  @Transactional
  @Override
  public ErpETask save(ErpETask task) {
    val entity =
        taskRepository
            .findErpTaskEntityByTaskId(task.getTaskId())
            .map(it -> taskMapper.updateEntity(it, task))
            .orElseGet(() -> taskMapper.fromData(task));

    taskRepository.save(entity);
    return task;
  }

  /**
   * @param receipt
   * @return
   */
  @Transactional
  @Override
  public ErpEReceiptOut save(ErpEReceiptOut receipt) {
    val entity = receiptMapper.fromData(receipt);
    receiptRepository.save(entity);
    return receipt;
  }

  /**
   * @param prescriptionId
   * @return
   */
  @Override
  public EmptyData deletePrescription(String prescriptionId) {
    taskRepository.findErpTaskEntityByTaskId(prescriptionId).ifPresent(taskRepository::delete);
    return new EmptyData();
  }

  @Override
  public Optional<ErpETask> getPrescriptionById(String prescriptionId) {
    return taskRepository.findErpTaskEntityByTaskId(prescriptionId).map(taskMapper::fromEntity);
  }

  /**
   * @param prescriptionId
   * @return
   */
  @Override
  public Optional<ErpEReceiptOut> getReceiptById(String prescriptionId) {
    return this.receiptRepository
        .findErpReceiptEntitiesByPrescriptionId(prescriptionId)
        .map(receiptMapper::fromEntity);
  }

  /**
   * @return
   */
  @Override
  public Page<ErpEReceiptEntity> getReceipts(Pageable pageable) {
    return this.receiptRepository.findAll(pageable);
  }

  @Override
  public ErpETask getPrescriptionByIdOrThrow(String prescriptionId) {
    return this.getPrescriptionById(prescriptionId)
        .orElseThrow(() -> new EntityNotFoundError(ErpETask.class, prescriptionId));
  }

  @Override
  public Page<ErpETask> searchPrescriptions(Specification<ErpETaskEntity> spec, Pageable pageable) {

    val entityPage = taskRepository.findAll(spec, pageable);
    return new PageImpl<>(
        entityPage.getContent().stream().map(taskMapper::fromEntity).toList(),
        pageable,
        entityPage.getTotalElements());
  }
}
