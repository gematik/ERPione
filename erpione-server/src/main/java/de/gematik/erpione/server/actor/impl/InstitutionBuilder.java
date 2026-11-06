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

package de.gematik.erpione.server.actor.impl;

import de.gematik.bbriccs.konnektor.Konnektor;
import de.gematik.bbriccs.rest.HttpBClient;
import de.gematik.bbriccs.smartcards.SmcB;
import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.test.erezept.client.ErpClient;

public interface InstitutionBuilder {
  ErpioneInstitutionData info();

  SmcB smcB();

  ErpClient erpClient();

  HttpBClient erpProbingClient();

  Konnektor konnektor();

  HermesService hermes();

  ErpionePersistence repository();
}
