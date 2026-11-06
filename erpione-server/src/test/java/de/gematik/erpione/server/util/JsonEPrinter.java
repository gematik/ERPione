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

package de.gematik.erpione.server.util;

import de.gematik.erpione.data.ErpioneData;
import lombok.SneakyThrows;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

public class JsonEPrinter {
  private static final boolean SHOULD_PRINT = false;
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

  @SneakyThrows
  public static void printAsJson(ErpioneData data) {
    if (SHOULD_PRINT) System.out.println(OBJECT_WRITER.writeValueAsString(data));
  }
}
