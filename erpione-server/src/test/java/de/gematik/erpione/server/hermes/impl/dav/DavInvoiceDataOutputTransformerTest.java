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

package de.gematik.erpione.server.hermes.impl.dav;

import static de.gematik.erpione.server.util.JsonEPrinter.printAsJson;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.data.r4.chargeitem.InvoiceEDataOut;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.util.ExamplesReader;
import de.gematik.test.erezept.fhir.r4.dav.DavPkvAbgabedatenBundle;
import de.gematik.test.erezept.fhir.testutil.ErpFhirBuildingTest;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class DavInvoiceDataOutputTransformerTest extends ErpFhirBuildingTest {

  @Autowired private HermesService hermes;

  static Stream<Arguments> shouldTransformFromFhirToJson() {
    return ExamplesReader.readExampleFiles("classpath:/fhir/valid/dav/**/*.xml").stream()
        .flatMap(
            pair ->
                Stream.of(
                    Arguments.of(pair.getFirst(), pair.getSecond(), true),
                    Arguments.of(pair.getFirst(), pair.getSecond(), false)));
  }

  @ParameterizedTest
  @MethodSource
  @SneakyThrows
  void shouldTransformFromFhirToJson(String fileName, String content) {
    val resource = hermes.decode(DavPkvAbgabedatenBundle.class, content);
    val invoice = resource.getInvoice();

    val data = assertDoesNotThrow(() -> hermes.toJson(InvoiceEDataOut.class, invoice));
    assertNotNull(data);
    printAsJson(data);
  }
}
