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

package de.gematik.erpione.server.actor;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.parser.DataFormatException;
import de.gematik.bbriccs.fhir.codec.EmptyResource;
import de.gematik.bbriccs.fhir.codec.utils.FhirTestResourceUtil;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.server.actor.impl.ErpioneActorBuilder;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.error.ErpResponseExpectationError;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.idp.client.IdpClientRuntimeException;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.usecases.ErpBaseCommand;
import de.gematik.test.erezept.client.usecases.TaskGetByIdCommand;
import de.gematik.test.erezept.client.usecases.TaskRejectCommand;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.Secret;
import de.gematik.test.erezept.fhir.values.TaskId;
import java.util.Map;
import lombok.val;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

class ErpioneActorTest {

  @Test
  void shouldHandleResponseExpectationEdgeCases01() {
    // error case with operation outcome response
    val response =
        FhirBResponse.forPayload(EmptyResource.class, new OperationOutcome())
            .withStatusCode(403)
            .withHeaders(Map.of())
            .andValidationResult(FhirTestResourceUtil.createEmptyValidationResult());
    val actor = new ErpioneTestActor01(response);

    val request = new TaskRejectCommand(TaskId.random(), AccessCode.random(), Secret.random());
    val error =
        assertThrows(ErpResponseExpectationError.class, () -> actor.requestAndUnpack(request));
    assertInstanceOf(ErpResponseExpectationError.class, error);
  }

  @Test
  void shouldHandleResponseExpectationEdgeCases02() {
    val client = mock(ErpClient.class);
    when(client.request(any())).thenThrow(new DataFormatException("not FHIR"));

    val actorPair = ErpioneActorBuilder.doctor("Alice").erpClient(client).build();
    val actor = actorPair.getFirst();
    val command = new TaskGetByIdCommand(TaskId.random());

    assertThrows(ErpResponseExpectationError.class, () -> actor.request(command));
  }

  @Test
  void shouldHandleResponseExpectationEdgeCases03() {
    val client = mock(ErpClient.class);
    when(client.request(any())).thenThrow(new IdpClientRuntimeException("some IDP error"));

    val actorPair = ErpioneActorBuilder.doctor("Alice").erpClient(client).build();
    val actor = actorPair.getFirst();
    val command = new TaskGetByIdCommand(TaskId.random());

    assertThrows(ErpResponseExpectationError.class, () -> actor.request(command));
  }

  private record ErpioneTestActor01(FhirBResponse<?> mockResponse) implements ErpioneActor {
    @Override
    public void initialize() {
      // not needed for this test
    }

    @Override
    public ErpioneInstitutionData getInfo() {
      return null;
    }

    @Override
    public String getMnemonic() {
      return "Machaon";
    }

    @Override
    public HermesService getHermes() {
      return null;
    }

    @Override
    public ErpionePersistence getRepository() {
      return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Resource> FhirBResponse<R> request(ErpBaseCommand<R> request) {
      return (FhirBResponse<R>) mockResponse;
    }
  }
}
