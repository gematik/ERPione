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

function sendToFhir(csrfToken) {
    const actor = document.getElementById('selected-actor').value;
    const ptype = document.getElementById('selected-ptype').value;
    const data = document.getElementById('fhir-input').innerText || '';

    // Direktzuweisung
    const direct = document.getElementById('direct-assignment').checked;
    const customKvnr = document.getElementById("custom-kvnr").value
    const customKvnrParam = (customKvnr) ? `&kvnr=${customKvnr}` : ''

    if (!data) {
        appendAlert("Kein Verordnungsdatensatz angegeben", "danger")
        return
    }

    prescribeThroughFhir(ptype, direct, customKvnrParam, csrfToken, actor, data);
}

function formatDate(forErpione, date) {
    const mm_start = date.getMonth() + 1;
    const dd_start = date.getDate();
    if(forErpione){
        return (dd_start < 10 ? '0' + dd_start : dd_start) + '.' + (mm_start < 10 ? '0' + mm_start : mm_start) + '.' + date.getFullYear();
    } else {
        return date.getFullYear() + '-' + (mm_start < 10 ? '0' + mm_start : mm_start) + '-' + (dd_start < 10 ? '0' + dd_start : dd_start);
    }
}

function fillAccidentValues(data, accidentType) {
    data.medicationRequest.accident = {};
    data.medicationRequest.accident.type = accidentType;

    let accidentLocation = accidentType === "2" ? document.getElementById('accident-workplace').value : null;
    accidentLocation = accidentLocation === "" ? null : accidentLocation;
    if(accidentLocation !== null){
        data.medicationRequest.accident.organization = accidentLocation;
    }

    let accidentDate = document.getElementById('accident-date').value;
    if(accidentType !== "4" && accidentDate !== ""){
        data.medicationRequest.accident.date = formatDate(true, new Date(accidentDate));
    }
}

function sendToHermes(csrfToken) {
    const actor = document.getElementById('selected-actor').value;
    const ptype = document.getElementById('selected-ptype').value;
    const shouldPrescribe = document.getElementById('hermes-mode').checked;

    const direct = document.getElementById('direct-assignment').checked;
    const medicationCategory = document.getElementById("t-rezept").checked ? '02' : "00"

    const data = {
        patient: {
            name: {
                prefix: document.getElementById('name-prefix').value || undefined,
                given: document.getElementById('first-name').value || undefined,
                family: document.getElementById('last-name').value || undefined,
            },
            kvnr: document.getElementById('kvnr').value || undefined
        },
        coverage: {
            insurantState: document.getElementById('selected-istate').value,
            personGroup: document.getElementById('selected-pgroup').value,
            name: document.getElementById('insuranceName').value || undefined,
            iknr: document.getElementById('iknr').value || undefined,
        },
        practitioner: {
            anr: {
                type: document.getElementById("anr-select").value,
                value: document.getElementById("anr").value || undefined
                }
        },
        organization: {
            bsnr: document.getElementById("bsnr").value || undefined
        }
    };
    // set the coverage type accordingly
    const selectedIType = document.getElementById('selected-itype').value
    const isPayor = selectedIType === 'SKT' || selectedIType === 'UK'
    if (isPayor) {
        data.coverage.payorType = selectedIType
    } else {
        data.coverage.type = selectedIType
    }

    if (ptype === 'pharmaceutical') {
        data.medication = {
            type: document.getElementById('selected-med-type').value,
            category: medicationCategory,
            name: document.getElementById('medication').value,
            pzn: document.getElementById('pzn').value,
        }
        data.medicationRequest = {
            empIdentifier: document.getElementById('emp-identifier').value || undefined,
            substitutionAllowed: document.getElementById('substitution-allowed').checked,
            emergencyFee: document.getElementById('emergency-fee').checked,
            note: document.getElementById('freetext').value,
        }
    } else {
        data.healthAppRequest = {
            name: document.getElementById('medication').value,
            pzn: document.getElementById('pzn').value
        }
    }

    const asvnrInput = document.getElementById('asvnr').value;
    if(asvnrInput !== ""){
        data.practitionerRole ={
            asvnr: asvnrInput
        }
    }

    const accidentType = document.getElementById('selected-accident-type').value;
    if(accidentType !== "0"){
        fillAccidentValues(data, accidentType);
    }

    const isMvo = document.getElementById('multiple-prescription').checked;
    if(isMvo){
        const mvoStartDate = document.getElementById('mvo-start-date').value;
        const mvoPeriod = Number(document.getElementById('mvo-period').value);
        const mvoAmount = Number(document.getElementById('mvo-amount').value);

        const finalizedBodyArray = [];
        let tempData = data;

        tempData.medicationRequest.mvo = {
            numerator: 1,
            denominator: mvoAmount,
            startDate: formatDate(true, new Date(mvoStartDate)),
            endDate: formatDate(true, new Date(mvoStartDate)),
        }
        //Generate the faked base prescription as a template
        fetch(`/api/faker/kbv/${ptype}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'X-XSRF-Token': csrfToken,
                'x-actor': actor
            },
            credentials: "same-origin",
            body: JSON.stringify(tempData)
        })
            .then(response => checkForLogin(response))
            .then(async response => {
                const responseBody = response.ok ? await response.json() : await response.text()
                return [response.ok, responseBody]
            })
            .then(response => {
                const [ok, data] = response;
                if (ok) {
                    for(let i = 0; i < mvoAmount; i++) {
                        let tempBody = data;
                        const start = new Date(mvoStartDate);
                        start.setDate(start.getDate() + (i * mvoPeriod));
                        const end = new Date(start);
                        end.setDate(end.getDate() + mvoPeriod);

                        tempBody.medicationRequest.mvo.numerator = i + 1;
                        tempBody.medicationRequest.mvo.denominator = mvoAmount;
                        tempBody.medicationRequest.mvo.startDate = formatDate(true, start);
                        tempBody.medicationRequest.mvo.endDate = formatDate(true, end);
                        finalizedBodyArray.push(JSON.stringify(tempBody));
                    }
                    for(let i = 0; i < mvoAmount; i++){
                        shouldPrescribe ?
                            prescribeThroughHermes(ptype, direct, csrfToken, actor, finalizedBodyArray[i]) :
                            fakeThroughHermes(ptype,csrfToken, finalizedBodyArray[i]);
                    }
                }
            })
            .catch(handleUnexpectedFetchError);
    } else {
        const body = JSON.stringify(data)

        if (shouldPrescribe) {
            prescribeThroughHermes(ptype, direct, csrfToken, actor, body)
        } else {
            fakeThroughHermes(ptype,csrfToken, body)
        }
    }
}

function submitPrescribeForm() {
    const isFhirMode = document.getElementById('fhir-mode').checked;
    const csrfToken = getXsrfToken();
    if (isFhirMode)
        sendToFhir(csrfToken)
    else
        sendToHermes(csrfToken)
}

function prescribeThroughFhir(ptype, direct, customKvnrParam, csrfToken, actor, data){
    showSpinner();
    fetch(`/api/erp/${ptype}?direct=${direct}${customKvnrParam}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/fhir+xml',
            'X-XSRF-Token': csrfToken,
            'x-actor': actor
        },
        credentials: "same-origin",
        body: data
    })
        .then(response => checkForLogin(response))
        .then(async response => [response.ok, await response.json()])
        .then(response => {
            hideSpinner();
            const [ok, data] = response;
            const codeBox = getHighlightedCodeBox(data)
            codeBox.prepend(createEditPrescriptionButton(data))
            document.getElementById('response-container').prepend(codeBox);
            if (ok) {
                const medication = (data.prescription.medication || data.prescription.healthAppRequest).name || 'N/A';
                appendAlert(`E-Rezept für ${medication} (${data.taskId}) erfolgreich ausgestellt`, "success")
            } else {
                appendAlert(`Fehler: ${data.message}`, "danger")
            }
        })
        .catch(handleUnexpectedFetchError);
}

function prescribeThroughHermes (ptype, direct, csrfToken, actor, body){
    showSpinner();
    fetch(`/api/erp/${ptype}?direct=${direct}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-Token': csrfToken,   // required for POST
            'x-actor': actor
        },
        credentials: "same-origin",
        body: body
    })
        .then(response => checkForLogin(response))
        .then(async response => [response.ok, await response.json()])
        .then(response => {
            hideSpinner();
            const [ok, data] = response;
            const codeBox = getHighlightedCodeBox(data)
            codeBox.prepend(createEditPrescriptionButton(data))
            document.getElementById('response-container').prepend(codeBox);
            if (ok) {
                appendAlert(`E-Rezept für ${data.prescription.medication.name} (${data.taskId}) erfolgreich ausgestellt`, "success")
            } else {
                appendAlert(`Fehler: ${data.message}`, "danger")
            }
        })
        .catch(handleUnexpectedFetchError);
}

function fakeThroughHermes (ptype, csrfToken, body){
    showSpinner();
    fetch(`/api/faker/kbv/${ptype}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/fhir+xml',
            'X-XSRF-Token': csrfToken
        },
        credentials: "same-origin",
        body: body
    })
        .then(response => checkForLogin(response))
        .then(async response => {
            const responseBody = response.ok ? await response.text() : await response.json()
            return [response.ok, responseBody]
        })
        .then(response => {
            hideSpinner();
            const [ok, data] = response;
            document.getElementById('response-container').prepend(getHighlightedCodeBox(data));
            if (!ok) appendAlert(`Fehler: ${data.message}`, "danger")
        })
        .catch(handleUnexpectedFetchError);
}

function fetchAndPasteExample() {
    let url = document.getElementById('example-url').value;
    if (!url) {
        appendAlert('Bitte geben Sie eine gültige URL ein.', "warning");
        return;
    }
    // only allow .xml files
    if (!url.toLowerCase().endsWith(".xml")) {
        appendAlert("Die URL muss auf .xml enden", "warning")
        return
    }
    url = toRawGitHubUrl(url)

    fetch(url)
        .then(response => {
            // if (!response.ok) throw new Error('Fehler beim Laden der Datei');
            return response.text();
        })
        .then(text => {
            const code = document.getElementById('fhir-input');

            // this will trigger the automatic paste eventListener
            // Create a DataTransfer and ClipboardEvent for paste
            const clipboardData = new DataTransfer();
            clipboardData.setData('text', text);

            const pasteEvent = new ClipboardEvent('paste', {
                clipboardData: clipboardData,
                bubbles: true
            });

            // Dispatch the paste event
            code.dispatchEvent(pasteEvent);
        })
        .catch(err => appendAlert('Fehler: ' + err.message, "danger"));
}

function toRawGitHubUrl(url) {
    // Example: https://github.com/org/repo/blob/branch/path/to/file
    // → https://raw.githubusercontent.com/org/repo/branch/path/to/file
    return url.replace(
        /^https:\/\/github\.com\/([^/]+)\/([^/]+)\/blob\/([^/]+)\/(.+)$/,
        'https://raw.githubusercontent.com/$1/$2/$3/$4'
    );
}
