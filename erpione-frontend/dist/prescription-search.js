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

async function claimExternalPrescription(endpoint, actorId, alertMessage) {
    const csrfToken = getXsrfToken();
    showSpinner();
    return fetch(endpoint, {
        method: 'POST',
        headers: {
            'X-XSRF-Token': csrfToken,
            'x-actor': actorId,
        },
        credentials: "same-origin"
    })
        .then(response => checkForLogin(response))
        .then(async response => {
            const data = await response.text();
            return [response.ok, data];
        })
        .then(result => {
            const [ok, data] = result;
            hideSpinner();
            if (!ok) {
                appendAlert('Aktion konnte nicht ausgeführt werden', 'danger');
                console.log(data);
                document.getElementById('response-container').prepend(getHighlightedCodeBox(data));
            } else {
                submitPrescriptionSearchForm(0);
                appendAlert(alertMessage, 'success');
                document.getElementById('external-pid').value = '';
                document.getElementById('external-ac').value = '';
            }
        })
        .catch(handleUnexpectedFetchError);
}

async function importExternalPrescription() {
    const prescriptionId = document.getElementById('external-pid').value;
    const accessCode = document.getElementById('external-ac').value;
    const actor = document.getElementById('actor-selection').value.toLowerCase();

    if (!prescriptionId || !accessCode) {
        appendAlert("Um ein Rezept zu importieren, müssen sowohl die Rezept-ID als auch der Zugangscode angegeben werden.", "warning");
        return;
    }
    if (!actor) {
        appendAlert("Bitte wählen Sie eine annehmende Stelle aus, um das Rezept zu importieren.", "warning");
        return;
    }
    if (prescriptionId.startsWith("162") && actor !== "thessaly") {
        appendAlert("EVDGA müssen mit einem Kostenträger angenommen werden.", "warning");
        return;
    } else if (!prescriptionId.startsWith("162") && actor === "thessaly") {
        appendAlert("Rezepte müssen mit einer Apotheke angenommen werden.", "warning");
        return;
    }

    await claimExternalPrescription(
        `/api/erp/${encodeURIComponent(prescriptionId)}/claim?ac=${encodeURIComponent(accessCode)}`,
        actor,
        `Externes Rezept ${prescriptionId} erfolgreich akzeptiert`
    );
}

const prescriptionPageSizeStorageKey = 'prescriptionPageSize';

function handlePageSizeStorageError(error) {
    if (!(error instanceof DOMException) ||
        !['SecurityError', 'QuotaExceededError'].includes(error.name)) {
        throw error;
    }
    console.warn('Page size cannot be remembered in this browser:', error);
}

function initializePrescriptionPageSize() {
    const select = document.getElementById('page-size');
    let savedSize;
    try {
        savedSize = localStorage.getItem(prescriptionPageSizeStorageKey);
    } catch (error) {
        handlePageSizeStorageError(error);
    }
    select.value = Array.from(select.options).some(option => option.value === savedSize) ? savedSize : '10';
}

function changePrescriptionPageSize() {
    const size = document.getElementById('page-size').value;
    try {
        localStorage.setItem(prescriptionPageSizeStorageKey, size);
    } catch (error) {
        handlePageSizeStorageError(error);
    }
    submitPrescriptionSearchForm(0);
}

function submitPrescriptionSearchForm(offset) {
    const kvnr = document.getElementById('kvnr-input').value;
    const tid = document.getElementById('tid-input').value;
    const iknr = document.getElementById('iknr-input').value;
    const flowType = document.getElementById('selected-flow-type').value;
    const pstatus = document.getElementById('selected-prescription-status').value;

    const fetchQueryParams = []

    if (kvnr) fetchQueryParams.push(`kvnr=${encodeURIComponent(kvnr)}`);
    if (tid) fetchQueryParams.push(`tid=${encodeURIComponent(tid)}`);
    if (flowType) fetchQueryParams.push(`flowType=${encodeURIComponent(flowType)}`);
    if (pstatus) fetchQueryParams.push(`status=${encodeURIComponent(pstatus)}`);
    if (iknr) fetchQueryParams.push(`iknr=${encodeURIComponent(iknr)}`);

    const pageSize = document.getElementById('page-size').value;
    fetchQueryParams.push(`size=${encodeURIComponent(pageSize)}`, `page=${encodeURIComponent(offset)}`);

    const fetchQuery = `?${fetchQueryParams.join('&')}`;
    const container = document.getElementById('response-container');
    const pagination = document.getElementById('pagination-container');
    container.innerHTML = '';
    pagination.innerHTML = '';

    showSpinner()
    fetch(`/api/erp/prescriptions${fetchQuery}`)
        .then(response => checkForLogin(response))
        .then(response => response.json())
        .then(data => {
            hideSpinner()
            if (!data) {
                container.innerHTML = '<p>Keine E-Rezepte gefunden.</p>';
                return;
            }
            createPaginationControls(data)
            createAccordion(container, data.content, createPrescriptionAccordionItem)
        })
        .catch(handleUnexpectedFetchError);
}

function createPaginationControls(data) {
    const pageable = data.pageable || {};
    const pageNumber = pageable.pageNumber || 0;
    const totalPages = data.totalPages || 0;

    if (totalPages <= 1) return;

    const container = document.getElementById('pagination-container');
    const pageNav = document.createElement('nav');
    pageNav.setAttribute('aria-label', 'Seitennavigation');
    const pagination = document.createElement('ul');
    pagination.className = 'pagination mb-0';
    pageNav.appendChild(pagination);
    container.appendChild(pageNav);

    const lastPage = totalPages - 1;
    const isFirst = pageNumber === 0;
    const isLast = pageNumber === lastPage;

    pagination.appendChild(createPageItem('\u00AB', 0, {disabled: isFirst, ariaLabel: 'Erste Seite'}));
    pagination.appendChild(createPageItem('\u2039', pageNumber - 1, {disabled: isFirst, ariaLabel: 'Vorherige Seite'}));

    // Sliding window of pages around the current page, clamped to the valid range
    const windowSize = 5;
    let start = Math.max(0, pageNumber - Math.floor(windowSize / 2));
    let end = Math.min(lastPage, start + windowSize - 1);
    start = Math.max(0, end - windowSize + 1);

    if (start > 0) {
        pagination.appendChild(createPageItem('1', 0));
        if (start > 1) pagination.appendChild(createPageItem('\u2026', null, {disabled: true}));
    }
    for (let i = start; i <= end; i++) {
        pagination.appendChild(createPageItem((i + 1).toString(), i, {active: i === pageNumber}));
    }
    if (end < lastPage) {
        if (end < lastPage - 1) pagination.appendChild(createPageItem('\u2026', null, {disabled: true}));
        pagination.appendChild(createPageItem(totalPages.toString(), lastPage));
    }

    pagination.appendChild(createPageItem('\u203A', pageNumber + 1, {disabled: isLast, ariaLabel: 'Nächste Seite'}));
    pagination.appendChild(createPageItem('\u00BB', lastPage, {disabled: isLast, ariaLabel: 'Letzte Seite'}));
}

function createPageItem(label, page, {active = false, disabled = false, ariaLabel} = {}) {
    const pageItem = document.createElement('li');
    pageItem.className = 'page-item' + (active ? ' active' : '') + (disabled ? ' disabled' : '');
    if (active) pageItem.setAttribute('aria-current', 'page');

    const pageLink = document.createElement(disabled ? 'span' : 'a');
    pageLink.className = 'page-link';
    pageLink.textContent = label;
    if (ariaLabel) pageLink.setAttribute('aria-label', ariaLabel);
    if (!disabled) {
        pageLink.href = '#';
        pageLink.onclick = (event) => {
            event.preventDefault();
            submitPrescriptionSearchForm(page);
        };
    }
    pageItem.appendChild(pageLink);
    return pageItem;
}


function createPrescriptionAccordionItem(erpTask) {
    if (!erpTask) return '';

    const flowType = erpTask.flowType || 'N/A';

    const status = calculateSyntheticStatus(erpTask);



    const taskId = erpTask.taskId || 'N/A';
    const ac = erpTask.accessCode;

    const prescription = erpTask.prescription || {};
    const medication = prescription.medication || prescription.healthAppRequest || {};
    const mr = prescription.medicationRequest || {};

    const patient = prescription.patient || {};
    const coverage = prescription.coverage || {};

    const practitioner = prescription.practitioner || {};
    const organization = prescription.organization || {};

    const titlePostfix = getPrescriptionTitlePostfix(erpTask)
    const pcolor = getColorForFlowType(flowType);
    const statusColor = getColorForStatus(status)

    const acq = (ac) ? `&ac=${ac}` : '';
    const editPrescriptionQuery = `pid=${taskId}${acq}`;
    return `
<div class="accordion-item">
    <div class="accordion-header">
        <button class="accordion-button collapsed" style="background-color: ${pcolor}" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-prescription-${taskId}"
                aria-expanded="true" aria-controls="collapse-prescription-${taskId}">
            <div class="d-flex flex-column pt-0 pb-0">
                <div><span class="fw-bold h6">${medication.name}</span> - ${titlePostfix}</div>
                <small class="text-muted" style="font-size: 0.85em;">Status <span style="background-color: ${statusColor}">${status}</span> | ${erpTask.taskId}</small>
                <small class="text-muted" style="font-size: 0.85em;">ausgestellt am ${erpTask.authoredOn}</small>
            </div>
        </button>
    </div>
    <div id="collapse-prescription-${taskId}" class="accordion-collapse collapse is-open" data-bs-parent="#accordionExample">
        <div class="container-fluid accordion-body">
            <div class="row align-items-end">
                <div class="btn-group">
                    <button class="btn btn-success mb-3" role="button" aria-expanded="false"
                        onclick="window.location.href='prescription.html?${editPrescriptionQuery}'">
                        Details anzeigen und einlösen
                    </button>
            </div>
            </div>
            <div class="row align-items-start">
                <div class="col text-start border border-dark-subtle rounded">
                        ${prescriptionTaskInfo(erpTask)}
                        ${medicalPrescriptionInfo(flowType, medication, mr)}
                        ${patientInfo(patient, coverage)}
                        ${practitionerInfo(practitioner, organization)}
                </div>
            </div>
        </div>
    </div>
</div>`
}
