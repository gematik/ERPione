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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.util.TaskCreator;
import de.gematik.test.erezept.fhir.values.Secret;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Slf4j
@SpringBootTest(
    properties = {"spring.profiles.active=rudev,offline,noauth"},
    classes = {
      ErpioneConfiguration.class,
      ErpEnvironmentConfig.class,
      ErpionePersistenceImpl.class,
      ErpioneJpaConfig.class,
      HermesServiceImpl.class,
      TaskCreator.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    })
@EnableConfigurationProperties(DataSourceProperties.class)
class ErpionePersistenceImplTest {

  @Autowired private ErpionePersistence repository;
  @Autowired private TaskCreator taskCreator;

  @Test
  void shouldFindTaskByPrescriptionId() {
    val task = repository.save(taskCreator.createNewTask());

    val found = repository.getPrescriptionById(task.getTaskId());
    assertTrue(found.isPresent(), "Task should be found by ID");

    val found2 = assertDoesNotThrow(() -> repository.getPrescriptionByIdOrThrow(task.getTaskId()));
    assertEquals(found.get().getTaskId(), found2.getTaskId());
  }

  @Test
  void shouldUpdateTask() {
    val task = repository.save(taskCreator.createNewTask());

    val secret = Secret.random().getValueAsString();
    task.setSecret(secret);
    task.setStatus(PrescriptionEStatus.INPROGRESS);
    repository.save(task);

    val found = repository.getPrescriptionById(task.getTaskId());
    assertTrue(found.isPresent(), "Task should be found by ID");
    val updated = found.get();
    assertEquals(secret, updated.getSecret());
    assertEquals(PrescriptionEStatus.INPROGRESS, updated.getStatus());
  }

  @Test
  void shouldPurgeExpiredPrescriptions() {
    val task = repository.save(taskCreator.createNewTask());
    task.setExpiryDate(new Date(System.currentTimeMillis() - 1000)); // Set expiry date in the past
    repository.save(task);

    // Simulate the scheduled purge method
    repository.purgeExpiredPrescriptions();

    val found = repository.getPrescriptionById(task.getTaskId());
    assertTrue(found.isEmpty(), "Task should NOT be found by ID");
  }

  @Test
  @Disabled("Create x random prescriptions for testing purposes")
  void createRandomPrescriptions() {
    for (var i = 0; i < 100; i++) {
      val task = repository.save(taskCreator.createNewTask());
      log.info("Created task with ID: {}", task.getTaskId());
    }
  }
}
