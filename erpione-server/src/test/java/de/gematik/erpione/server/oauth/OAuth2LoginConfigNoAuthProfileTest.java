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

package de.gematik.erpione.server.oauth;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.actor.ErpioneActorService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
@AutoConfigureMockMvc
class OAuth2LoginConfigNoAuthProfileTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private ErpioneActorService actorService;

  @BeforeEach
  void stubProtectedEndpoint() {
    given(actorService.getActorsInformation())
        .willReturn(
            List.of(
                new ErpioneInstitutionData(
                    "123456789", "demo", "Demo Pharmacy", InstitutionERole.PHARMACY)));
  }

  @Test
  void shouldAllowPermittedEndpointWithoutAuthentication() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void shouldAllowProtectedEndpointWithoutAuthenticationInNoauthMode() throws Exception {
    mvc.perform(get("/api/actors"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].mnemonicId").value("123456789"));
  }
}
