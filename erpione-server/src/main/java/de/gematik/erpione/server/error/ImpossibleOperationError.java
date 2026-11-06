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

package de.gematik.erpione.server.error;

import de.gematik.erpione.data.error.IssueData;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class ImpossibleOperationError extends RuntimeException {

  @Getter private final List<IssueData> issues = new ArrayList<>();

  public ImpossibleOperationError(String message) {
    super(message);
  }

  public ImpossibleOperationError(String message, IssueData... issues) {
    this(message, List.of(issues));
  }

  public ImpossibleOperationError(String message, List<IssueData> issues) {
    this(message);
    this.issues.addAll(issues);
  }
}
