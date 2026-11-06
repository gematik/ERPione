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

package de.gematik.erpione.server.jpa.entity.kbv;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Data
@Entity
@Table(name = "PRESCRIPTIONS")
@EqualsAndHashCode(callSuper = true)
public class KbvEPrescriptionEntity extends AbstractPersistable<Long> {

  @Column(name = "prescription_id")
  private String prescriptionId;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
  private KbvEPatientEntity patient;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
  private KbvEPractitionerEntity practitioner;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private KbvEPractitionerRoleEntity practitionerRole;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
  private KbvEOrganizationEntity organization;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
  private KbvECoverageEntity coverage;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private KbvEMedicationEntity medication;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private KbvEMedicationRequestEntity medicationRequest;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private KbvEHealthAppRequestEntity healthAppRequest;
}
