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

package de.gematik.erpione.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.erpione.data.error.ErrorData;
import de.gematik.erpione.data.error.ErrorType;
import de.gematik.erpione.server.actor.error.ActorNotFoundError;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class ControllerErrorHandlerTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper om;

  @ParameterizedTest
  @CsvSource({"/api/actors/unknown,No such actor with id 'unknown'"})
  void shouldProperlyHandleActorNotFound(String uriTemplate, String expectedMessage)
      throws Exception {

    mvc.perform(get(uriTemplate).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            result -> assertInstanceOf(ActorNotFoundError.class, result.getResolvedException()))
        .andExpect(
            result -> assertEquals(expectedMessage, result.getResolvedException().getMessage()))
        .andExpect(
            result -> {
              val content = result.getResponse().getContentAsString();
              val error = assertDoesNotThrow(() -> om.readValue(content, ErrorData.class));
              assertEquals(ErrorType.ERPIONE, error.getType());
            });
  }
}
