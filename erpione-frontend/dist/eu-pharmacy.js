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

function updatePharmacyButtonLabel() {
    const actor = document.getElementById('selected-actor');
    const selectedActor = actor.options[actor.selectedIndex].innerHTML

    const select = document.getElementById('selected-flow-step');
    const selectedOption = select.options[select.selectedIndex].innerHTML;
    document.getElementById('eu-pharmacy-btn').innerText = selectedOption + " aus " + selectedActor;

    // change the country flag
    const flag = document.getElementById("country-flag")
    flag.innerHTML = actor.options[actor.selectedIndex].dataset.flag
}

function queryNcpeh() {
    const actorId = document.getElementById('selected-actor').value;
    const op = document.getElementById('selected-flow-step').value;

    const kvnr = document.getElementById('kvnr-input').value;
    const ac = document.getElementById('eu-code-input').value;

    if (!kvnr || !ac) {
        appendAlert("Bitte geben Sie sowohl die KVNR als auch den EU Access Code ein.", "warning")
        return
    }

    const responseDiv = document.getElementById('response-container')
    responseDiv.innerHTML = ``

    const prescriptionSelection = document.getElementById('prescription-selection-container')
    prescriptionSelection.innerHTML = ``;

    showSpinner()
    fetch(`/api/eu/${op}/${encodeURIComponent(kvnr)}?ac=${encodeURIComponent(ac)}`, {
        method: 'GET',
        headers: {
            'X-XSRF-Token': getXsrfToken(),
            'x-actor': actorId
        }
    })
        .then(response => checkForLogin(response))
        .then(async response => {
            const json = await response.json();
            return [response.ok, json];
        })
        .then(result => {
            hideSpinner()
            const [ok, data] = result;
            if (ok) {
                // wrap in task for reusability but in EU we simply don't have task information
                const tasks = data.prescriptions.map(p => {
                    const flowType = (typeof p.prescriptionId === 'string' && p.prescriptionId.length >= 3)
                        ? p.prescriptionId.substring(0, 3)
                        : "160";
                    return {taskId: p.prescriptionId, prescription: p, status: 'ready', flowType: flowType}
                })
                if (op === 'prescriptions')
                    createPrescriptionActionButtons(tasks, prescriptionSelection);
                createAccordion(responseDiv, tasks, createPrescriptionAccordionItem);
                responseDiv.append(createRawDataButton(data))
            } else {
                responseDiv.append(getHighlightedCodeBox(data))
            }
        }).catch(handleUnexpectedFetchError);
}

function createPrescriptionActionButtons(tasks, container) {
    tasks.forEach(it => {
        const prescription = it.prescription
        const pid = prescription.prescriptionId
        const name = prescription.medication.name

        const formDiv = document.createElement('div');
        const label = document.createElement('label');
        const claimBtn = document.createElement('button');
        const dispenseBtn = document.createElement('button');

        formDiv.append(label, claimBtn, dispenseBtn)
        container.prepend(formDiv);

        formDiv.className = 'd-flex align-items-center justify-content-between mb-2 mt-2'
        label.className = 'form-check-label flex-grow-1 me-2'
        label.for = pid
        label.innerText = `${name} (${pid})`;

        claimBtn.className = 'btn btn-outline-success'
        dispenseBtn.className = 'btn btn-outline-success'

        claimBtn.id = `eu-claim-button-${pid}`
        dispenseBtn.id = `eu-close-button-${pid}`
        claimBtn.innerText = 'Annehmen'
        dispenseBtn.innerText = 'Dispensieren'

        claimBtn.onclick = (e) => euPrescriptionAction('claim', e, pid);
        dispenseBtn.disabled = true
    })
}

function euPrescriptionAction(action, event, pid, medication) {
    event.preventDefault();
    const actorId = document.getElementById('selected-actor').value;
    const kvnr = document.getElementById('kvnr-input').value;
    const ac = document.getElementById('eu-code-input').value;

    let body;
    if(action === 'claim') {
        body = JSON.stringify({claims: [pid]});
    } else if(action === 'close') {
        body = JSON.stringify({prescriptions: [{prescriptionId: pid, medication: medication, wasSubstituted: false}]});
    }
    showSpinner();
    fetch(`/api/eu/${action}/${encodeURIComponent(kvnr)}?ac=${encodeURIComponent(ac)}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-Token': getXsrfToken(),
            'x-actor': actorId
        },
        credentials: "same-origin",
        body: body
    })
        .then(response => checkForLogin(response))
        .then(async response => [response.ok, await response.json()])
        .then(response => {
            const [ok, data] = response;
            if (ok) {
                const dispenseBtn = document.getElementById(`eu-close-button-${pid}`);
                const claimBtn = document.getElementById(`eu-claim-button-${pid}`);

                let actionElementDiv;
                Array.from(document.getElementsByClassName('form-check-label')).forEach(label => {
                    if(label.innerHTML.includes(pid)) {
                        actionElementDiv = label.parentElement;
                    }
                });

                if(action === 'claim') {
                    const medication = data.prescriptions[0].medication
                    claimBtn.disabled = true;
                    dispenseBtn.disabled = false;
                    dispenseBtn.onclick = (e) => euPrescriptionAction('close', e, pid, medication);
                    appendAlert(`E-Rezept ${medication.name} erfolgreich akzeptiert`, "success");
                } else if(action === 'close') {
                    dispenseBtn.disabled = true;
                    if(actionElementDiv) {
                        const statusElement = document.createElement('span');
                        statusElement.className = 'text-success ms-2 p-2 ps-0';
                        statusElement.innerText = '[Dispensiert]';
                        actionElementDiv.prepend(statusElement);
                    }
                    appendAlert(`E-Rezept ${medication.name} erfolgreich dispensiert`, "success");
                }
            } else {
                document.getElementById('response-container').prepend(getHighlightedCodeBox(data));
                appendAlert(`Fehler: ${data.message}`, "danger")
            }
            hideSpinner()
        }).catch(handleUnexpectedFetchError);
}
