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

package de.gematik.erpione.server.usecase;

import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionList;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionOut;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.test.erezept.client.usecases.eu.EuGetPrescriptionPostCommand;
import de.gematik.test.erezept.fhir.builder.eu.EuGetPrescriptionInputBuilder;
import de.gematik.test.erezept.fhir.profiles.definitions.KbvItaErpStructDef;
import de.gematik.test.erezept.fhir.r4.eu.EuHealthcareFacilityType;
import de.gematik.test.erezept.fhir.r4.eu.EuOrganizationProfession;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.values.EuAccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.eu.EuRequestType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ReadEuPrescriptions
    implements ErpioneActorUsecase<HealthcareInstitution, KbvEPrescriptionList> {

  private final EuRequestType requestType;
  private final KVNR kvnr;
  private final EuAccessCode accessCode;
  private final List<PrescriptionId> prescriptionIds;

  public ReadEuPrescriptions(EuRequestType requestType, KVNR kvnr, EuAccessCode accessCode) {
    this(requestType, kvnr, accessCode, List.of());
  }

  /**
   * Executes the use case with the given actor and data.
   *
   * @param actor
   * @return a result of type D, which is the output of the use case
   */
  @Override
  public KbvEPrescriptionList performAs(HealthcareInstitution actor) {
    val input =
        EuGetPrescriptionInputBuilder.forRequestType(requestType)
            .kvnr(kvnr)
            .accessCode(accessCode)
            .countryCode(actor.getCountryCode())
            .practitionerRole(EuOrganizationProfession.getDefaultPharmacist())
            .pointOfCare(actor.getName())
            .healthcareFacilityType(EuHealthcareFacilityType.getDefault())
            .prescriptionIds(prescriptionIds)
            .practitionerName("Hannes Vogt")
            .build();

    val command =
        requestType.equals(EuRequestType.DEMOGRAPHICS)
            ? EuGetPrescriptionPostCommand.forDemographics(input)
            : new EuGetPrescriptionPostCommand(input);

    val bundle = actor.requestAndUnpack(command);
    val prescriptions =
        bundle.getEntry().stream()
            .filter(KbvItaErpStructDef.BUNDLE::matches)
            .map(b -> (KbvErpBundle) b.getResource())
            .map(b -> actor.getHermes().toJson(KbvEPrescriptionOut.class, b))
            .toList();

    // update status of the prescriptions
    val repo = actor.getRepository();
    prescriptions.forEach(
        it ->
            // TODO: only task created with this erpione instance will be updated
            // in future, we could try to extract external prescriptions and store
            // in local DB as well
            repo.getPrescriptionById(it.getPrescriptionId())
                .ifPresent(
                    task -> {
                      // assumption: if PRESCRIPTION_RETRIEVAL successful, FD will move status to
                      // in-progress
                      val status =
                          EuRequestType.PRESCRIPTION_RETRIEVAL == requestType
                              ? PrescriptionEStatus.INPROGRESS
                              : task.getStatus();
                      task.setStatus(status);
                      repo.save(task);
                    }));

    return KbvEPrescriptionList.of(prescriptions);
  }

  public static ReadEuPrescriptions demographicsFor(String kvnr, String accessCode) {
    return demographicsFor(KVNR.from(kvnr), EuAccessCode.from(accessCode));
  }

  public static ReadEuPrescriptions demographicsFor(KVNR kvnr, EuAccessCode accessCode) {
    return new ReadEuPrescriptions(EuRequestType.DEMOGRAPHICS, kvnr, accessCode);
  }

  public static ReadEuPrescriptions prescriptionsListFor(String kvnr, String accessCode) {
    return prescriptionsListFor(KVNR.from(kvnr), EuAccessCode.from(accessCode));
  }

  public static ReadEuPrescriptions prescriptionsListFor(KVNR kvnr, EuAccessCode accessCode) {
    return new ReadEuPrescriptions(EuRequestType.PRESCRIPTION_LIST, kvnr, accessCode);
  }

  public static ReadEuPrescriptions claimPrescriptions(
      String kvnr, String accessCode, List<String> prescriptionIds) {
    val pids = prescriptionIds.stream().map(PrescriptionId::from).toList();
    return claimPrescriptions(KVNR.from(kvnr), EuAccessCode.from(accessCode), pids);
  }

  public static ReadEuPrescriptions claimPrescriptions(
      KVNR kvnr, EuAccessCode accessCode, List<PrescriptionId> prescriptionIds) {
    return new ReadEuPrescriptions(
        EuRequestType.PRESCRIPTION_RETRIEVAL, kvnr, accessCode, prescriptionIds);
  }
}
