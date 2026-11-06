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

function submitCommunicationsSearch() {
    const actorId = document.getElementById('selected-actor').value;
    console.log("fetch communications as " + actorId)

    const container = document.getElementById('response-container');
    container.innerHTML = '';

    showSpinner()
    fetch(`/api/erp/communications`, {
        headers: {
            'x-actor': actorId
        }
    })
        .then(response => checkForLogin(response))
        .then(response => response.json())
        .then(data => {
            hideSpinner()
            if (!data) {
                container.innerHTML = '<p>Keine E-Rezepte gefunden.</p>';
                return;
            }
            createAccordion(container, data.communications, createCommunicationAccordionItem);
        })
        .catch(handleUnexpectedFetchError);
}

function createCommunicationAccordionItem(erpCom) {
    if (!erpCom) return '';

    const type = erpCom.type || 'N/A';
    const pid = erpCom.reference.prescriptionId || 'N/A';

    const pcolor = getColorForComType(type);

    const sender = erpCom.sender
    const recipient = erpCom.recipient
    const sent = erpCom.sent
    const receivedText = erpCom.received ? `gelesen am ${erpCom.received}` : 'noch nicht gelesen';

    const rawMessageJson = erpCom.message
    const comVersion = rawMessageJson.version || '1'

    const title = `${sender} -> ${recipient}`
    const rid = erpCom.rid

    return `
<div class="accordion-item">
    <div class="accordion-header">
        <button class="accordion-button collapsed" style="background-color: ${pcolor}" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-prescription-${rid}"
                aria-expanded="false" aria-controls="collapse-prescription-${rid}">
            <div class="d-flex flex-column pt-0 pb-0">
                <div><span class="fw-bold h6 text-bg-secondary">(v${comVersion})</span> <span class="fw-bold h6">${type}</span> zum E-Rezept ${pid}</div>
                <small class="text-muted" style="font-size: 0.85em;">${title}</small>
                <small class="text-muted" style="font-size: 0.85em;">gesendet am ${sent} / ${receivedText}</small>
                <small class="text-muted" style="font-size: 0.85em;">FHIR-Resource: ${rid}</small>
            </div>
        </button>
    </div>
    <div id="collapse-prescription-${rid}" class="accordion-collapse collapse is-open" data-bs-parent="#accordionExample">
        <div class="container-fluid accordion-body">
            <pre>${JSON.stringify(rawMessageJson, null, 3)}</pre>
        </div>
    </div>
</div>`
}

function getColorForComType(comType) {
    let color;
    switch (comType) {
        case 'DISP_REQ':
            color = 'rgb(0, 123, 255, 0.1)';
            break;
        case 'INFO_REQ':
            color = 'rgb(111, 66, 193, 0.1)';
            break;
        case 'REPLY':
        case 'CHANGE_REPLY':
            color = 'rgb(32, 201, 151, 0.1)';
            break;
        case 'CHANGE_REQ':
            color = 'rgb(255, 140, 80, 0.2)';
            break;
        default:
            color = 'rgb(255, 90, 90, 0.2)';
    }
    return color;
}
