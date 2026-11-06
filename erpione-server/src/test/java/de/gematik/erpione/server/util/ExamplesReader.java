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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.util.Pair;

public class ExamplesReader {

  private static final PathMatchingResourcePatternResolver RESOLVER =
      new PathMatchingResourcePatternResolver((ClassLoader) null);

  @SneakyThrows
  public static List<Resource> readResources(String locationPattern) {
    val resources = RESOLVER.getResources(locationPattern);
    return Arrays.stream(resources).toList();
  }

  public static List<String> readExamples(String locationPattern) {
    return readResources(locationPattern).stream().map(ExamplesReader::readAsString).toList();
  }

  public static List<Pair<String, String>> readExampleFiles(String locationPattern) {
    return readResources(locationPattern).stream()
        .map(
            it -> {
              val content = readAsString(it);
              val file = Optional.ofNullable(it.getFilename()).orElse("unknown-file");
              return Pair.of(file, content);
            })
        .toList();
  }

  @SneakyThrows
  private static String readAsString(Resource resource) {
    return resource.getContentAsString(Charset.defaultCharset());
  }
}
