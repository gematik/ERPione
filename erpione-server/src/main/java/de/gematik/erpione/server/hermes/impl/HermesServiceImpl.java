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

package de.gematik.erpione.server.hermes.impl;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.erpione.data.ErpioneInputData;
import de.gematik.erpione.data.ErpioneOutputData;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.error.HermesTransformerNotFoundError;
import de.gematik.erpione.server.hermes.impl.dav.DavInvoiceDataInputTransformer;
import de.gematik.erpione.server.hermes.impl.dav.DavInvoiceDataOutputTransformer;
import de.gematik.erpione.server.hermes.impl.erp.ErpReceiptTransformer;
import de.gematik.erpione.server.hermes.impl.erp.ErpTaskTransformer;
import de.gematik.erpione.server.hermes.impl.eu.EuMedicationInputTransformer;
import de.gematik.erpione.server.hermes.impl.gem.*;
import de.gematik.erpione.server.hermes.impl.kbv.*;
import de.gematik.test.erezept.fhir.parser.FhirParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.Delegate;
import lombok.val;
import org.hl7.fhir.r4.model.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class HermesServiceImpl implements HermesService {

  private static final EncodingType ENCODING_TYPE = EncodingType.XML;
  private static final boolean PRETTY_PRINT = true;

  @Delegate private final FhirParser fhirCodec;

  private final Map<HermesTransformer.HKey<?, ?>, JsonToFhirTransformer<?, ?, ?>> jsonTransformer =
      new HashMap<>();
  private final Map<HermesTransformer.HKey<?, ?>, FhirToJsonTransformer<?, ?>> fhirTransformer =
      new HashMap<>();

  @Autowired
  public HermesServiceImpl(FhirParser fhirCodec, ObjectMapper objectMapper) {
    this.fhirCodec = fhirCodec;

    // Register transformers here
    // TODO: find a better way to register transformers
    register(new KbvErpBundleInputTransformer(this));
    register(new KbvErpBundleOutputTransformer(this));
    register(new KbvEvdgaBundleInputTransformer(this));
    register(new KbvEvdgaBundleOutputTransformer(this));
    register(new KbvCoverageInputTransformer());
    register(new KbvCoverageOutputTransformer());
    register(new KbvMedicationRequestInputTransformer());
    register(new KbvMedicationRequestOutputTransformer());
    register(new KbvMedicationInputTransformer());
    register(new KbvMedicationOutputTransformer());
    register(new KbvPatientInputTransformer());
    register(new KbvPatientOutputTransformer());
    register(new KbvPractitionerInputTransformer());
    register(new KbvPractitionerOutputTransformer());
    register(new KbvMedicalOrganizationInputTransformer());
    register(new KbvMedicalOrganizationOutputTransformer());
    register(new ErpTaskTransformer(this));
    register(new ErpReceiptTransformer());
    register(new GemErpMedicationInputTransformer());
    register(new GemErpMedicationOutputTransformer());
    register(new DispenseParametersTransformer(this));
    register(new CloseParametersTransformer(this));
    register(new MedicationDispenseInputTransformer());
    register(new MedicationDispenseOutputTransformer());
    register(new MedicationDispenseBundleTransformer(this));
    register(new CommunicationOutputTransformer(objectMapper));
    register(new CommunicationReplyInputTransformer());
    register(new EuMedicationInputTransformer());

    // PKV charge item
    register(new DavInvoiceDataOutputTransformer());
    register(new DavInvoiceDataInputTransformer());
  }

  private void register(FhirToJsonTransformer<?, ?> transformer) {
    fhirTransformer.put(transformer.key(), transformer);
  }

  private void register(JsonToFhirTransformer<?, ?, ?> transformer) {
    jsonTransformer.put(transformer.key(), transformer);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <J extends ErpioneOutputData, R extends Resource> J toJson(
      Class<J> jsonType, R fhirResource) {
    val t = (FhirToJsonTransformer<J, R>) findFhirTransformer(jsonType, fhirResource.getClass());
    return Optional.ofNullable(t)
        .map(it -> it.toJson(fhirResource))
        .orElseThrow(
            () -> HermesTransformerNotFoundError.fhir2Hermes(fhirResource.getClass(), jsonType));
  }

  @Override
  public <J extends ErpioneInputData, R extends Resource> String toFhirString(
      Class<R> fhirType, @NotNull J data) {
    val fhir = toFhirResource(fhirType, data);
    return fhirCodec.encode(fhir, ENCODING_TYPE, PRETTY_PRINT);
  }

  @Override
  public <J extends ErpioneInputData, R extends Resource> R toFhirResource(
      Class<R> fhirType, @NotNull J data) {
    val t = findJsonTransformer(data, fhirType);
    return Optional.ofNullable(t)
        .map(it -> it.toFhir(data).build())
        .orElseThrow(() -> HermesTransformerNotFoundError.hermes2Fhir(data.getClass(), fhirType));
  }

  @SuppressWarnings({"unchecked", "java:S1172"})
  @Override
  public <J extends ErpioneInputData, R extends Resource, B extends ResourceBuilder<R, B>>
      B toFhirBuilder(J data, Class<R> fhirType, Class<B> builderType) {
    val t = findJsonTransformer(data, fhirType);
    return (B)
        Optional.ofNullable(t)
            .map(it -> it.toFhir(data))
            .orElseThrow(
                () -> HermesTransformerNotFoundError.hermes2Fhir(data.getClass(), fhirType));
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <J extends ErpioneInputData, R extends Resource>
      JsonToFhirTransformer<J, R, ?> findJsonTransformer(J erpioneData, Class<R> fhirType) {
    return (JsonToFhirTransformer<J, R, ?>) findJsonTransformer(erpioneData.getClass(), fhirType);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <J extends ErpioneInputData, R extends Resource>
      JsonToFhirTransformer<J, R, ?> findJsonTransformer(Class<J> jsonType, Class<R> fhirType) {
    val key = new HermesTransformer.HKey<>(jsonType, fhirType);
    return (JsonToFhirTransformer<J, R, ?>) jsonTransformer.get(key);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <J extends ErpioneOutputData, R extends Resource>
      FhirToJsonTransformer<J, R> findFhirTransformer(Class<J> jsonType, Class<R> fhirType) {
    val key = new HermesTransformer.HKey<>(jsonType, fhirType);
    return (FhirToJsonTransformer<J, R>) fhirTransformer.get(key);
  }
}
