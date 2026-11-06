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

package de.gematik.erpione.server.actor.error;

import static java.text.MessageFormat.format;

import de.gematik.erpione.data.actor.ActorERole;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.error.ErpioneNotFoundError;

public class ActorNotFoundError extends ErpioneNotFoundError {
  public ActorNotFoundError(String id) {
    super(format("No such actor with id ''{0}''", id));
  }

  public ActorNotFoundError(ActorERole type, String id) {
    this(type.name(), id);
  }

  public ActorNotFoundError(InstitutionERole type, String id) {
    this(type.name(), id);
  }

  private ActorNotFoundError(String type, String id) {
    super(format("No such {0} with id ''{1}''", type, id));
  }
}
