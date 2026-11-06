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

let currentErpTask = null;
let dispenseEData = null; // hermes dispense data to be used (MedicationDispenseEBundleIn)
let lastHermesText = null; // last hermes-input text that was parsed (skip re-parsing unchanged input)
let currentSelectedActor = null;
let actorList = [];
fetchActorList();

function fetchActorList() {
    fetch('/api/actors')
        .then(response => checkForLogin(response))
        .then(response => response.json())
        .then(data => {
            data.forEach(actor => {
                actorList.push(actor)
            })
        })
        .catch(handleUnexpectedFetchError);
}

document.addEventListener('click', (event) => {
    const id = event.target.id;
    if (id === 'claim-prescription-btn') {
        event.preventDefault();
        claimPrescription();
    } else if(id === 'return-prescription-btn') {
        event.preventDefault();
        returnPrescription();
    } else if(id === 'close-prescription-btn') {
        event.preventDefault();
        closePrescription();
    } else if(id === 'delete-prescription-btn') {
        event.preventDefault();
        deletePrescription();
    } else if(id === 'dispense-prescription-btn') {
        event.preventDefault();
        dispensePrescription();
    } else if(id === 'decline-prescription-btn') {
        event.preventDefault();
        declinePrescription();
    } else if(id === 'create-chargeitem-btn') {
        event.preventDefault();
        createChargeItem();
    } else if(id === 'read-receipt-btn') {
        event.preventDefault();
        readReceipt();
    } else if (id === 'open-com-reply-overlay-btn') {
        event.preventDefault();
        currentSelectedActor = document.getElementById('selected-actor').value;
        openComReplyOverlay(currentErpTask, currentSelectedActor);
    } else if (id === 'open-put-dialog-btn') {
        event.preventDefault();
        currentSelectedActor = document.getElementById('selected-actor').value;
        openPutDialogOverlay(currentErpTask, currentSelectedActor);
    } else if (id === 'open-check-chargeitem-overlay-btn') {
        event.preventDefault();
        openCheckChargeItemOverlay();
    }
});

//TODO the ERPIONE might be missing a status for the declined DIGA, or if its a virtual status, a way to determine it
function injectActionButtonsFor(erpTask) {
    currentErpTask = erpTask;
    dispenseEData = calculateDispenseDataFor(erpTask)
    resetHermesInputState();

    const actionButtonsContainer = document.getElementById('prescription-action-buttons');

    const ptypeLabel = (erpTask.flowType === '162') ? "EVDGA" : "E-Rezept"
    const closeLabel = (erpTask.flowType === '162') ? "bewilligen" : "schließen"

    const status = calculateSyntheticStatus(erpTask);

    switch (status) {
        case "ready": {
            hideInputMasks();
            actionButtonsContainer.innerHTML = `<button class="btn btn-success" id="claim-prescription-btn">${ptypeLabel} annehmen</button>`
            actionButtonsContainer.innerHTML += `<button class="btn btn-outline-secondary" id="open-com-reply-overlay-btn">Communication Reply</button>`
            actionButtonsContainer.innerHTML += `<button class="btn btn-outline-danger" id="delete-prescription-btn">${ptypeLabel} löschen</button>`
            if (erpTask.flowType !== '162'){
            }
            break
        }
        case "in-progress": {
            showInputEditor();
            actionButtonsContainer.innerHTML = `<button class="btn btn-success" id="close-prescription-btn">${ptypeLabel} ${closeLabel}</button>`

            if (erpTask.flowType === '162'){
                actionButtonsContainer.innerHTML += `<button class="btn btn-outline-danger" id="decline-prescription-btn">${ptypeLabel} zurückweisen</button>`
            } else {
                actionButtonsContainer.innerHTML += `<button class="btn btn-outline-success" id="dispense-prescription-btn">${ptypeLabel} dispensieren</button>`
                actionButtonsContainer.innerHTML += `<button class="btn btn-outline-danger" id="delete-prescription-btn">${ptypeLabel} löschen</button>`
            }

            actionButtonsContainer.innerHTML += `<button class="btn btn-outline-secondary" id="return-prescription-btn">${ptypeLabel} zurückgeben</button>`
            actionButtonsContainer.innerHTML += `<button class="btn btn-outline-secondary" id="open-com-reply-overlay-btn">Communication Reply</button>`
            updateFhirMode();
            break
        }
        case 'dispensed': {
            showInputEditor();
            actionButtonsContainer.innerHTML = `<button class="btn btn-success" id="close-prescription-btn">${ptypeLabel} ${closeLabel}</button>`
            actionButtonsContainer.innerHTML += `<button class="btn btn-outline-secondary" id="open-com-reply-overlay-btn">Communication Reply</button>`
            updateFhirMode();
            break
        }
        case 'completed': {
            hideInputMasks();
            actionButtonsContainer.innerHTML = ``
            if(erpTask.flowType.startsWith('2')){
                actionButtonsContainer.innerHTML += `<button class="btn btn-success" id="create-chargeitem-btn">Kostenbeleg erzeugen</button>`
                actionButtonsContainer.innerHTML += `<button class="btn btn-outline-primary" id="open-put-dialog-btn">Kostenbeleg anpassen</button>`
                actionButtonsContainer.innerHTML += `<button class="btn btn-outline-info" id="open-check-chargeitem-overlay-btn">Kostenbeleg prüfen</button>`
            }

            actionButtonsContainer.innerHTML += `<button class="btn btn-outline-secondary" id="read-receipt-btn">Quittung erneut abrufen</button>`
            break
        }
        default: {
            hideInputMasks();
            actionButtonsContainer.innerHTML = ``
            break
        }
    }
}

function updateFhirMode() {
    const mode = document.getElementById('fhir-mode').checked;
    const fhirStyle = mode ? '' : 'none';
    const hermesStyle = mode ? 'none' : '';

    const fhirMask = document.getElementById('fhir-input-mask');
    const hermesMask = document.getElementById('hermes-input-mask');
    fhirMask.style.display = fhirStyle;
    hermesMask.style.display = hermesStyle;

    if (currentErpTask == null) return;

    if (mode) {
        // Note: in fhir-mode we do always fetch the FHIR data from Hermes,
        // because the user might have changed the hermes input, and we want to reflect that in the FHIR output.
        // Commit any pending edit (input may still have focus) before fetching.
        tryParseHermesInput();
        const fhirEditor = getFhirEditor();
        fhirEditor.refresh();
        fetchFhirDispenseData().then(fhirData => {
            if (fhirData) fhirEditor.setValue(prettifyXml(fhirData));
        });

    } else {
        // check hermes input has already text
        const hermesEditor = getHermesEditor();
        hermesEditor.refresh();
        if (hermesEditor.getValue().trim() === '') {
            hermesEditor.setValue(JSON.stringify(dispenseEData, null, 4));
        }
    }
}

function showInputEditor() {
    document.getElementById('input-editor').style.display = 'block';
}

function hideInputMasks() {
    document.getElementById('input-editor').style.display = 'none';
}

let fhirEditor = null;
let hermesEditor = null;

function createCodeEditor(textareaId, mode) {
    const textarea = document.getElementById(textareaId);
    if (!textarea || typeof CodeMirror === 'undefined') return null;
    return CodeMirror.fromTextArea(textarea, {
        mode: mode,
        lineNumbers: true,
        lineWrapping: true,
        indentUnit: 4,
        tabSize: 4,
        indentWithTabs: false,
        extraKeys: {Tab: cm => cm.replaceSelection(' '.repeat(cm.getOption('indentUnit')))}
    });
}

function getFhirEditor() {
    if (!fhirEditor) fhirEditor = createCodeEditor('fhir-input', 'application/xml');
    return fhirEditor;
}

function getHermesEditor() {
    if (!hermesEditor) {
        hermesEditor = createCodeEditor('hermes-input', 'application/json');
        if (hermesEditor) {
            hermesEditor.on('blur', tryParseHermesInput);
            hermesEditor.getWrapperElement().addEventListener('mouseleave', tryParseHermesInput);
        }
    }
    return hermesEditor;
}

function getFhirInputText() {
    return fhirEditor ? fhirEditor.getValue() : '';
}

function getHermesInputText() {
    return hermesEditor ? hermesEditor.getValue().trim() : '';
}

/** Forget the last parsed text and clear any validation hint (e.g. after dispenseEData was regenerated). */
function resetHermesInputState() {
    lastHermesText = null;
    showHermesInputError(null);
}

/**
 * Parses the current hermes-input text. On valid JSON the result is stored in dispenseEData,
 * on invalid JSON an inline hint is shown and dispenseEData keeps its last valid value.
 * Returns true if dispenseEData reflects the current input.
 */
function tryParseHermesInput() {
    const editor = getHermesEditor();
    if (!editor) return false;

    const raw = editor.getValue();
    const text = raw.trim();
    if (text === lastHermesText) return hermesInputValid;
    lastHermesText = text;

    if (text === '') {
        showHermesInputError(null);
        hermesInputValid = true;
        return true;
    }

    try {
        // parse the untrimmed value so reported line numbers match the editor gutter
        dispenseEData = JSON.parse(raw);
        showHermesInputError(null);
        hermesInputValid = true;
        return true;
    } catch (err) {
        const [message, line] = describeJsonError(err, raw);
        showHermesInputError(message, line);
        hermesInputValid = false;
        return false;
    }
}

let hermesInputValid = true;
let hermesErrorLine = null;

function showHermesInputError(message, line = null) {
    const editor = hermesEditor;
    const errorBox = document.getElementById('hermes-input-error');
    if (editor) {
        editor.getWrapperElement().classList.toggle('border-danger', !!message);
        if (hermesErrorLine !== null) editor.removeLineClass(hermesErrorLine, 'background', 'cm-error-line');
        hermesErrorLine = null;
        if (message && line !== null) {
            const idx = Math.min(line - 1, editor.lineCount() - 1); // CodeMirror lines are 0-based
            editor.addLineClass(idx, 'background', 'cm-error-line');
            hermesErrorLine = idx;
        }
    }
    if (!errorBox) return;
    errorBox.textContent = message || '';
    errorBox.hidden = !message;
}

/** Returns [message, line] with a human-readable hint derived from a JSON.parse SyntaxError. */
function describeJsonError(err, text) {
    const message = err && err.message ? err.message : 'Unbekannter Fehler';
    // Chromium/Safari: "... in JSON at position 42"
    const posMatch = /position (\d+)/.exec(message);
    if (posMatch) {
        const pos = Number(posMatch[1]);
        const before = text.slice(0, pos);
        const line = before.split('\n').length;
        const column = pos - before.lastIndexOf('\n');
        return [`Ungültiges JSON (Zeile ${line}, Spalte ${column}): ${message}`, line];
    }
    // Firefox: "... at line X column Y of the JSON data"
    const lineMatch = /line (\d+)/.exec(message);
    return [`Ungültiges JSON: ${message}`, lineMatch ? Number(lineMatch[1]) : null];
}

function prescriptionActionScaffold(endpoint, method, actorId, alertMessage, contentType = undefined, body = undefined) {
    const csrfToken = getXsrfToken();
    showSpinner()
    fetch(endpoint, {
        method: method,
        headers: {
            ...(contentType ? {'Content-Type': contentType} : {}),
            'X-XSRF-Token': csrfToken,
            'x-actor': actorId,
        },
        ...(body ? {body} : {}),
        credentials: "same-origin"
    })
        .then(response => checkForLogin(response))
        .then(async response => {
            const json = await response.json();
            return [response.ok, json];
        })
        .then(result => {
            const [ok, json] = result;
            if (!ok) {
                appendAlert('Aktion konnte nicht ausgeführt werden', 'danger');
                document.getElementById('response-container').prepend(getHighlightedCodeBox(json));
                hideSpinner();
            } else {
                appendAlert(alertMessage, 'success')
                initializePrescriptionContainer();
            }
        })
        .catch(handleUnexpectedFetchError);
}

function actorHasCorrectPermissions(actorId, erpTask, prescriptionAction){
    if(erpTask === null) {
        return false;
    }
    const actor = actorList.find(actor => (actor.mnemonicId === actorId) || (actor.mnemonic === actorId));
    const actorRole = actor.role;
    const isEVDGA = erpTask.flowType === '162';

    switch (prescriptionAction) {
        case 'claim':
            if(isEVDGA && actorRole !== 'Kostenträger') {
                appendAlert('Nur Kostenträger können EVDGA annehmen', 'danger');
                return false;
            } else if(!isEVDGA && actorRole !== 'Apotheke') {
                appendAlert('Nur Apotheken dürfen Rezepte annehmen', 'danger');
                return false;
            }
            break
        case 'return':
            if(isEVDGA && actorRole !== 'Kostenträger') {
                appendAlert('Nur Kostenträger können EVDGA zurückgeben', 'danger');
                return false;

            } else if(!isEVDGA && actorRole !== 'Apotheke') {
                appendAlert('Nur Apotheken dürfen Rezepte zurückgeben', 'danger');
                return false;
            }
            break
        case 'close':
            if(isEVDGA && actorRole !== 'Kostenträger') {
                appendAlert('Nur Kostenträger können EVDGA freischalten', 'danger');
                return false;
            } else if(!isEVDGA && actorRole !== 'Apotheke') {
                appendAlert('Nur Apotheken dürfen Rezepte schließen', 'danger');
                return false;
            }
            break
        case 'delete':
            if( isEVDGA && actorRole !== 'Kostenträger') {
                appendAlert('Nur Kostenträger dürfen EVDGA löschen', 'danger');
                return false;
            } else if( !isEVDGA && actorRole !== 'Apotheke') {
                appendAlert('Nur Apotheken dürfen Rezepte löschen die in Einlösung sind', 'danger');
                return false;
            }
            break
        case 'dispense':
           if(actorRole !== 'Apotheke') {
                appendAlert('Nur Apotheken dürfen Rezepte dispensieren', 'danger');
                return false;
            }
            break;
        case 'decline':
            if(isEVDGA && actorRole !== 'Kostenträger') {
                appendAlert('Nur Kostenträger können EVDGA ablehnen', 'danger');
                return false;
            }
            break
        case 'createChargeItem':
            if(actorRole !== 'Apotheke') {
                appendAlert('Nur Apotheken dürfen Kostenbelege ausstellen', 'danger');
                return;
            }
            break
        default:
            appendAlert('Unbekannte Aktion: ' + prescriptionAction, 'danger');
            break;
    }
    return true;
}

// Formats a Date as "dd.MM.yyyy"
function formatAsGermanDate(date) {
    const dd = String(date.getDate()).padStart(2, '0');
    const MM = String(date.getMonth() + 1).padStart(2, '0');
    const yyyy = date.getFullYear();
    return `${dd}.${MM}.${yyyy}`;
}

// Generates a random pharmaceutical-style lot number, e.g. "A2B7K491"
function generateLotNumber(length = 8) {
    const letters = 'ABJKLPRW'; // no I, O, Q to avoid confusion with digits
    const digits = '0123456789';
    const pick = (chars) => chars[Math.floor(Math.random() * chars.length)];

    let lot = pick(letters);
    for (let i = 1; i < length; i++) {
        lot += pick(Math.random() < 0.3 ? letters : digits);
    }
    return lot;
}

// Returns a random date between 1 and 12 months in the future
function randomFutureExpiryDate() {
    const monthsAhead = 1 + Math.floor(Math.random() * 12);
    const date = new Date();
    date.setMonth(date.getMonth() + monthsAhead);
    return date;
}

function claimPrescription() {
    const actorId = document.getElementById('selected-actor').value;

    if(!actorHasCorrectPermissions(actorId, currentErpTask, 'claim')) return;

    const pid = getParameterByName('pid');
    const ac = getParameterByName('ac') || null;
    const acQuery = (ac) ? `?ac=${encodeURIComponent(ac)}` : ''
    const endpoint = `/api/erp/${encodeURIComponent(pid)}/claim${acQuery}`;

    prescriptionActionScaffold(
        endpoint,
        'POST',
        actorId,
        `Rezept ${pid} erfolgreich akzeptiert`
    );
}

function returnPrescription() {
    const actorId = document.getElementById('selected-actor').value;

    if(!actorHasCorrectPermissions(actorId, currentErpTask, 'return')) return;

    const pid = getParameterByName('pid');
    const endpoint = `/api/erp/${encodeURIComponent(pid)}/return`;

    prescriptionActionScaffold(
        endpoint,
        'POST',
        actorId,
        `Rezept ${pid} erfolgreich zurückgegeben`
    );
}

function closePrescription() {
    const actorId = document.getElementById('selected-actor').value;

    if(!actorHasCorrectPermissions(actorId, currentErpTask, 'close')) return;

    const pid = getParameterByName('pid');
    const operation = pid.startsWith("162") ? "approve" : "close"
    const endpoint = `/api/erp/${encodeURIComponent(pid)}/${operation}`;

    const fhirMode = document.getElementById('fhir-mode').checked;
    const contentType = fhirMode ? "application/fhir+xml" : "application/json";

    let fhirData = getFhirInputText();
    const hermesData = getHermesInputText();

    // TODO: we only generate the FHIR for GEM_ERP_PR_PAR_DispenseOperation_Input, but here we need GEM_ERP_PR_PAR_CloseOperation_Input
    fhirData = fhirData.replace('GEM_ERP_PR_PAR_DispenseOperation_Input', 'GEM_ERP_PR_PAR_CloseOperation_Input')

    const body = fhirMode ? fhirData : hermesData;

    prescriptionActionScaffold(
        endpoint,
        'POST',
        actorId,
        `Rezept ${pid} erfolgreich dispensiert und geschlossen`,
        contentType,
        body
    );
}

function dispensePrescription() {
    let actorId = document.getElementById('selected-actor').value;
    if(!actorHasCorrectPermissions(actorId, currentErpTask, 'dispense')) return;

    const pid = getParameterByName('pid');
    const endpoint = `/api/erp/${encodeURIComponent(pid)}/dispense`;

    const fhirMode = document.getElementById('fhir-mode').checked;
    const contentType = fhirMode ? "application/fhir+xml" : "application/json";

    let fhirData = getFhirInputText();
    const hermesData = getHermesInputText();

    // TODO: FHIR profile might be wrong with GEM_ERP_PR_PAR_CloseOperation_Input, but here we need GEM_ERP_PR_PAR_DispenseOperation_Input
    fhirData = fhirData.replace('GEM_ERP_PR_PAR_CloseOperation_Input', 'GEM_ERP_PR_PAR_DispenseOperation_Input')

    const body = fhirMode ? fhirData : hermesData;

    prescriptionActionScaffold(
        endpoint,
        'POST',
        actorId,
        `Rezept ${pid} erfolgreich dispensiert`,
        contentType,
        body
    );
}

function deletePrescription() {
    let actorId = document.getElementById('selected-actor').value;

    if(!actorHasCorrectPermissions(actorId, currentErpTask, 'delete')) return;

    if(currentErpTask.status === 'ready') {
        actorId = 'Machaon'; //We cheat and allow deletion of ready tasks through a default prescribing actor
    }
    const pid = getParameterByName('pid');
    const endpoint = `/api/erp/${encodeURIComponent(pid)}/abort`;
    prescriptionActionScaffold(
        endpoint,
        'DELETE',
        actorId,
        `Rezept ${pid} erfolgreich gelöscht`
    );
}


function declinePrescription() {
    const actorId = document.getElementById('selected-actor').value;

    if(!actorHasCorrectPermissions(actorId, currentErpTask, 'decline')) return;

    const pid = getParameterByName('pid');
    const endpoint = `/api/erp/${encodeURIComponent(pid)}/decline`;
    prescriptionActionScaffold(
        endpoint,
        'POST',
        actorId, `
        Verordnung ${pid} erfolgreich zurückgewiesen`
    );
}

function createChargeItem() {
    const actorId = document.getElementById('selected-actor').value;

    if(!actorHasCorrectPermissions(actorId, currentErpTask, 'createChargeItem')) return;

    const pid = getParameterByName('pid');
    const endpoint = `/api/erp/chargeitems/${encodeURIComponent(pid)}`;
    prescriptionActionScaffold(
        endpoint,
        'POST',
        actorId,
        `Kostenbeleg für ${pid} erfolgreich eingestellt`,
        'application/json',
        '{}'
    );
}

function readReceipt() {
    //Promise-Handling differs slighty which made including it in the refactor too unwieldy
    const actorId = document.getElementById('selected-actor').value;
    const pid = getParameterByName('pid');

    const csrfToken = getXsrfToken();
    showSpinner()
    fetch(`/api/erp/receipts/${encodeURIComponent(pid)}`, {
        method: 'GET',
        headers: {
            'X-XSRF-Token': csrfToken,
            'x-actor': actorId,
            'accept': 'application/fhir+xml'
        },
        credentials: "same-origin",
    })
        .then(response => checkForLogin(response))
        .then(async response => {
            const text = await response.text();
            return [response.ok, text];
        })
        .then(result => {
            hideSpinner()
            const [ok, text] = result;
            document.getElementById('response-container').prepend(getHighlightedCodeBox(text));
            if (ok) {
                appendAlert(`Rezeptinformationen für ${pid} erfolgreich abgerufen`, 'success');
            }
        })
        .catch(handleUnexpectedFetchError);
}

/**
 * Use the generated dispenseEData to fetch the corresponding FHIR MedicationDispense resource from Hermes.
 */
function fetchFhirDispenseData() {
    const csrfToken = getXsrfToken();
    showSpinner()
    return fetch(`/api/faker/gem/dispense`, {
        method: 'POST',
        headers: {
            'X-XSRF-Token': csrfToken,
            'accept': 'application/xml',
            'content-type': 'application/json',
        },
        credentials: "same-origin",
        body: JSON.stringify(dispenseEData)
    })
    .then(response => checkForLogin(response))
    .then(async response => {
        const text = await response.text();
        return [response.ok, text];
    })
    .then(result => {
        hideSpinner()
        const [ok, text] = result;
        if (ok) {
            return text
        }
    })
    .catch(handleUnexpectedFetchError);
}
