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

package de.gematik.erpione.server.jpa.entity.gem;

import de.gematik.erpione.data.r4.etype.PrescriptionEFlow;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.server.jpa.entity.kbv.KbvEPrescriptionEntity;
import de.gematik.erpione.server.jpa.mapper.PrescriptionEFlowConverter;
import de.gematik.erpione.server.jpa.mapper.gem.PrescriptionEStatusConverter;
import jakarta.persistence.*;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class ErpETaskEntity extends AbstractPersistable<Long> {

  @Column(name = "task_id", unique = true, nullable = false)
  private String taskId;

  @Convert(converter = PrescriptionEFlowConverter.class)
  private PrescriptionEFlow flowType;

  @Convert(converter = PrescriptionEStatusConverter.class)
  private PrescriptionEStatus status;

  private String accessCode;
  private String secret;

  private Date authoredOn;
  private Date lastMedicationDispense;
  private Date expiryDate;
  private Date acceptDate;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
  @JoinColumn(name = "prescription_id")
  private KbvEPrescriptionEntity prescription;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "signature")
  private ErpESignatureEntity signature;

  private String prescriptionReference;
  private String receiptReference;
}
