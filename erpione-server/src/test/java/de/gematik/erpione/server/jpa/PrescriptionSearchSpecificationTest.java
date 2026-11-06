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

import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.actor.factory.ErpioneConfiguration;
import de.gematik.erpione.server.actor.factory.JacksonConfiguration;
import de.gematik.erpione.server.controller.search.PrescriptionSearchSpecification;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.entity.gem.ErpETaskEntity;
import de.gematik.erpione.server.jpa.factory.ErpioneJpaConfig;
import de.gematik.erpione.server.jpa.mapper.PrescriptionEFlowConverter;
import de.gematik.erpione.server.util.TaskCreator;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Slf4j
@SpringBootTest(
    classes = {
      ErpioneConfiguration.class,
      ErpEnvironmentConfig.class,
      HermesServiceImpl.class,
      ErpionePersistenceImpl.class,
      ErpioneJpaConfig.class,
      TaskCreator.class,
      PrescriptionEFlowConverter.class,
      JacksonConfiguration.class,
      JacksonAutoConfiguration.class
    },
    properties = {"spring.profiles.active=rudev,offline,noauth"})
@EnableConfigurationProperties(DataSourceProperties.class)
class PrescriptionSearchSpecificationTest {

  @Autowired private ErpionePersistence repository;
  @Autowired private TaskCreator taskCreator;

  private static Specification<ErpETaskEntity> convert(String requestParam) {
    val params =
        Arrays.stream(requestParam.split("&"))
            .map(
                arg -> {
                  val kv = arg.split("=");
                  val key = kv[0];
                  val value = kv[1];
                  return Map.entry(key, value);
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return PrescriptionSearchSpecification.fromQueryParams(params);
  }

  @ParameterizedTest
  @ValueSource(strings = {"flowType=160", "medication-type=PZN", "status=ready"})
  void shouldPerformChainOfSearchRequests(String initialQuery) {
    // Create some tasks as precondition
    val amountOfTasks = new Random().nextInt(20, 50);
    for (var i = 0; i < amountOfTasks; i++) {
      repository.save(taskCreator.createNewTask(PrescriptionFlowType.FLOW_TYPE_160));
    }
    for (var i = 0; i < amountOfTasks; i++) {
      repository.save(taskCreator.createNewTask(PrescriptionFlowType.FLOW_TYPE_200));
    }

    val spec = convert(initialQuery);
    val pageable = PageRequest.of(0, 10);
    val result = repository.searchPrescriptions(spec, pageable);
    log.info(
        "###> Search result {} entities for query '{}'",
        result.getNumberOfElements(),
        initialQuery);
    assertFalse(
        result.isEmpty(), "Expected to find at least one task to fit the query: " + initialQuery);

    val pivotIdx = new Random().nextInt(result.getNumberOfElements());
    val pivotTask = result.getContent().get(pivotIdx);
    val pivotKvnr = pivotTask.getPrescription().getPatient().getKvnr();

    val resultByKvnr = findByKvnr(pivotKvnr);
    assertFalse(resultByKvnr.isEmpty(), "Should find tasks by KVNR");
  }

  private Page<ErpETask> findByKvnr(String kvnr) {
    val spec = convert("kvnr=" + kvnr);
    val pageable = PageRequest.of(0, 10);
    return repository.searchPrescriptions(spec, pageable);
  }
}
