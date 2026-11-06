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

function getParameterByName(name, url = window.location.href) {
    name = name.replace(/[\[\]]/g, '\\$&');
    const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

function initializePrescriptionContainer() {
    const responseContainer = document.getElementById('response-container');
    responseContainer.innerHTML = '';
    responseContainer.className = '';

    const pid = getParameterByName('pid');
    if (!pid) {
        responseContainer.innerHTML = '<p>Keine Rezept-ID angegeben.</p>';
        return;
    }

    showSpinner()
    fetch(`/api/erp/prescriptions/${pid}`)
        .then(response => checkForLogin(response))
        .then(response => response.json())
        .then(data => {
            hideSpinner()
            if (!data) {
                responseContainer.innerHTML = '<p>Keine E-Rezepte gefunden.</p>';
                return;
            }
            renderPrescriptionData(responseContainer, data)

        })
        .catch(handleUnexpectedFetchError);
}

function renderPrescriptionData(parent, data) {
    injectActionButtonsFor(data)
    parent.innerHTML = createPrescriptionCard(data) + innerHtmlForRawDataButton(data);
}

function createPrescriptionCard(erpTask) {
    if (!erpTask) return '';

    const flowType = erpTask.flowType || 'N/A';
    const prescription = erpTask.prescription || {};
    const taskId = erpTask.taskId || prescription.prescriptionId || 'N/A';
    const ac = erpTask.accessCode || 'N/A';

    const medication = prescription.medication || prescription.healthAppRequest || {};
    const mr = prescription.medicationRequest || {};

    const patient = prescription.patient || {};
    const coverage = prescription.coverage || {};

    const practitioner = prescription.practitioner || {};
    const organization = prescription.organization || {};


    return `
    <div class="container-fluid">
        <div class="text-start">
                ${prescriptionTaskInfo(erpTask)}
                ${medicalPrescriptionInfo(flowType, medication, mr)}
                ${patientInfo(patient, coverage)}
                ${practitionerInfo(practitioner, organization)}
        </div>
        <div class="p-3">
            <img src="/api/erp/prescriptions/dmc/${taskId}?ac=${ac}" alt="DMC Image" style="max-width: 100%; max-height: 200px; border-radius: 8px; border: 1px solid #ccc;" />
        </div>
    </div>
</div>`
}

function medicalPrescriptionInfo(flowType, medication, mr) {
    const name = medication.name || 'N/A';
    const pzn = medication.pzn || `${medication.type} ${medication.category}`;
    const pznLabel = (medication.pzn) ? 'PZN' : 'Typ';
    // const freeText = medication.freeText || '';  // TODO: does not work properly
    const note = mr.note || ''
    const dosage = mr.dosage || ''
    const title = getTitleForFlowType(flowType);

    return `
<table class="table table-striped">
    <thead>
        <tr>
            <th colspan="2">${title}</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>${name}</td>
        </tr>
        <tr>
            <td>${pznLabel}</td>
            <td>${pzn}</td>
        </tr>
        ${getTableRowIfPresentValue("Dosierung", dosage)}
        ${getTableRowIfPresentValue("Notiz", note)}
    </tbody>
</table>`
}

function prescriptionTaskInfo(erpTask) {
    if (!erpTask.taskId) {
        return serializeFragment(getHighlightedCodeBox(erpTask));
    }
    const taskId = erpTask.taskId;
    const ac = erpTask.accessCode || 'N/A';
    const secret = erpTask.secret || '';
    const lastMedicationDispense = erpTask.lastMedicationDispense || '';
    const status = calculateSyntheticStatus(erpTask);
    const authoredOn = erpTask.authoredOn || 'N/A';
    const expiryDate = erpTask.expiryDate || 'N/A';
    const acceptDate = erpTask.acceptDate || 'N/A';

    return `
<table class="table table-striped mt-3">
    <thead>
        <tr>
            <td  style="background-color: ${getColorForStatus(status)}">Task ${status}</td>
            <td style="word-break: break-all; background-color: ${getColorForStatus(status)}">
                <div class="d-flex justify-content-between align-items-center">
                    <span id="prescription-id" class="me-2">${taskId}</span>
                    <button class="btn btn-sm" onclick="copyToClipboard('${taskId}')" title="Copy PrescriptionId">📋</button>
                </div>
            </td>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>AccessCode</td>
            <td style="word-break: break-all;">
                <div class="d-flex justify-content-between align-items-center">
                    <span id="access-code" class="me-2">${ac}</span>
                    <button class="btn btn-sm" onclick="copyToClipboard('${ac}')" title="Copy AccessCode">📋</button>
                </div>
            </td>
        </tr>
        <tr>
            <td>Secret</td>
            <td style="word-break: break-all;">
                <div class="d-flex justify-content-between align-items-center">
                    <span id="secret" class="me-2">${secret}</span>
                    <button class="btn btn-sm" onclick="copyToClipboard('${secret}')" title="Copy Secret">📋</button>
                </div>
            </td>
        </tr>
        <tr>
            <td>Erstelldatum</td>
            <td>${authoredOn}</td>
        </tr>
        <tr>
            <td>Einlösedatum</td>
            <td>${acceptDate}</td>
        </tr>
        <tr>
            <td>Verfalldatum</td>
            <td>${expiryDate}</td>
        </tr>
        ${getTableRowIfPresentValue("Dispensierung", lastMedicationDispense)}
    </tbody>
</table>`
}

function practitionerInfo(practitioner, organization) {
    const practitionerName = `${practitioner.name.prefix || ''} ${practitioner.name.given || 'N/A'} ${practitioner.name.family || 'N/A'}`.trim();
    const anr = practitioner.anr || {};
    const anrType = anr.type || 'N/A';
    const anrValue = anr.value || 'N/A';

    const bsnr = organization.bsnr || 'N/A';
    const officeName = organization.officeName || 'N/A';
    const address = `${organization.postal || 'N/A'} ${organization.city || 'N/A'} ${organization.street || 'N/A'}`.trim();

    return `
<table class="table table-striped">
    <thead>
        <tr>
            <th colspan="2">Verordner/-in</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>${practitionerName}</td>
        </tr>
        <tr>
            <td>${anrType}</td>
            <td>${anrValue}</td>
        </tr>
        <tr>
            <td>BSNR</td>
            <td>${bsnr}</td>
        </tr>
        <tr>
            <td>Praxis</td>
            <td>${officeName}</td>
        </tr>
        <tr>
            <td>Adresse</td>
            <td>${address}</td>
        </tr>
    </tbody>
</table>`
}

function patientInfo(patient, coverage) {
    const itype = coverage.type || coverage.payorType || 'N/A';
    const patientName = `${patient.name.prefix || ''} ${patient.name.given || 'N/A'} ${patient.name.family || 'N/A'}`.trim();
    const ktrName = `${coverage.name || 'N/A'} (${coverage.iknr || 'N/A'})`;
    const kvnr = patient.kvnr || 'N/A';
    const birthDate = patient.birthDate || 'N/A';
    const address = `${patient.postal || 'N/A'} / ${patient.city || 'N/A'}, ${patient.street || 'N/A'}`.trim();


    return `
<table class="table table-striped">
    <thead>
        <tr>
            <th colspan="2">Versicherte/-r</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Name</td>
            <td>${patientName}</td>
        </tr>
        <tr>
            <td>${itype}</td>
            <td>${ktrName}</td>
        </tr>
        <tr>
            <td>KVNR</td>
            <td>${kvnr}</td>
        </tr>
        <tr>
            <td>Geburtsdatum</td>
            <td>${birthDate}</td>
        </tr>
        <tr>
            <td>Adresse</td>
            <td>${address}</td>
        </tr>
    </tbody>
</table>`
}

function getPrescriptionTitlePostfix(erpTask) {
    let title = '';

    const prescription = erpTask.prescription || {};
    const flowType = erpTask.flowType || '';
    const medication = prescription.medication || prescription.healthAppRequest || {};
    const fields = []
    switch(flowType) {
        case '160':
        case '169':
        case '200':
        case '209':
            fields.push(medication.type, medication.category)
            break;
        case '162':
            fields.push("Digitale Gesundheitsanwendung", medication.category)
            break;
        case '165':
        case '166':
            fields.push(medication.category)
            break;
         default:
             fields.push("FlowType " + flowType)
    }

    fields.map(it => it ? it.trim() : '').forEach(it => title += `${it} `)
    return title
}

function getTitleForFlowType(flowType) {
    switch(flowType) {
        case '160':
        case '169':
        case '200':
        case '209':
            return "apothekenpflichtiges Medikament"
        case '162':
            return "Digitales Gesundheitsanwendung"
        case '165':
        case '166':
            return "⚠️ Teratogenes Medikament"
        default:
            return "Medizinische Verordnung"
    }
}

function getTableRowIfPresentValue(label, value) {
    if (!value) return ``
    return `
        <tr>
            <td>${label}</td>
            <td>${value}</td>
        </tr>`
}

function getColorForStatus(status) {
    let color = ''
    switch (status) {
        case 'ready':
            color = 'rgb(0, 154, 124, 0.4)'
            break
        case 'in-progress':
            color = 'rgb(255, 154, 24, 0.4)'
            break
        case 'dispensed':
            color = 'rgb(154, 24, 255, 0.4)'
            break
        case 'completed':
            color = 'rgb(24, 24, 154, 0.4)'
            break
        default:
            color = 'rgb(255, 125, 0, 0.8)'
    }
    return color
}


function getColorForFlowType(flowType) {
    let color = '';
    switch (flowType) {
        case '160':
        case '200':
            color = 'rgb(0, 123, 255, 0.1)';
            break;
        case '169':
        case '209':
            color = 'rgb(111, 66, 193, 0.1)';
            break;
        case '162':
            color = 'rgb(32, 201, 151, 0.1)';
            break;
        case '165':
        case '166':
            color = 'rgb(255, 140, 80, 0.2)';
            break;
        default:
            color = 'rgb(255, 90, 90, 0.2)';
    }
    return color;
}

function calculateSyntheticStatus(erpTask) {
    if (!erpTask) return 'N/A';
    if(erpTask.flowType === '162' && erpTask.status === 'completed') {
        //TODO
        // EVDGA specific logic here
    }
    const isDispensed = erpTask.lastMedicationDispense !== undefined;
    return isDispensed && (erpTask.status !== 'completed') ? 'dispensed' : erpTask.status || 'N/A';
}

function calculateDispenseDataFor(erpTask) {
    // now create what we would dispense depending on the prescribed medication
    const data = {}
    data.dispensation = [
        {
            medication: erpTask.prescription.medication,
            dispense: {
                whenHandedOver: formatAsGermanDate(new Date()),
                whenPrepared: formatAsGermanDate(new Date()),
                dosageInstructions: [ "1-0-1" ],
                notes: ["maximal 3 Stück pro Tag" ]
            },
            batch: {
                lotNumber: generateLotNumber(),
                expiryDate: formatAsGermanDate(randomFutureExpiryDate())
            }
        }
    ]
    return data
}
