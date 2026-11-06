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

document.addEventListener('DOMContentLoaded', () => {
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(element => {
        new bootstrap.Tooltip(element);
    });
});


function toggleAccidentInputs(){
    const accidentType = document.getElementById("selected-accident-type").value;

    //Accident Workplace Field
    const workplace = document.getElementById("accident-workplace-group");
    workplace.classList.toggle("d-none", accidentType !== "2");
    //Accident Date Field
    const accidentDateGroup = document.getElementById("accident-date-group");
    accidentDateGroup.classList.toggle("d-none", accidentType === "0" || accidentType === "4");
}

function toggleMvoFields() {
    const shouldShowMvoFields = document.getElementById('multiple-prescription').checked;
    const mvoFields = document.getElementById('multiple-prescription-container');

    const mvoStart = document.getElementById('mvo-start-date');
    if(mvoStart.value === ""){
        mvoStart.valueAsDate = new Date();
    }

    mvoFields.classList.remove('is-opening', 'is-closing');
    mvoFields.classList.add(shouldShowMvoFields ? 'is-opening' : 'is-closing');

    mvoFields.classList.toggle('is-collapsed', !shouldShowMvoFields);
    mvoFields.setAttribute('aria-hidden', String(!shouldShowMvoFields));
}

function updatePrescriptionType() {
    const ptype = document.getElementById('selected-ptype').value;

    const forPharma = ptype === 'pharmaceutical';
    const forEVDGA = !forPharma;
    const disabledFieldsForEVDGA = ["t-rezept", "direct-assignment"];

    disabledFieldsForEVDGA.forEach(id => {
        const el = document.getElementById(id);
        if(el) el.disabled = forEVDGA;
    })

    const kbvBundleProfileName = document.getElementById('kbv-bundle-profile-name')
    if (forEVDGA) {
        kbvBundleProfileName.innerText = "KBV_PR_EVDGA_Bundle"
    } else {
        kbvBundleProfileName.innerText = "KBV_PR_ERP_Bundle"
    }
}

function updateFhirMode(mode) {
    let hermesStyle = 'none'
    let fhirStyle = 'none'
    let hermesOnlyToggle = false
    let sendButtonStyle = 'btn-outline-success'
    let sendButtonText = 'E-Rezept ausstellen'

    // when using fhir, choosing T-Rezept is not possible
    const trezeptCheckBox = document.getElementById("t-rezept")

    switch (mode) {
        case 'faker':
            hermesStyle = 'block'
            sendButtonStyle = 'btn btn-outline-secondary'
            sendButtonText = 'FHIR generieren'
            break;
        case 'hermes':
            hermesStyle = 'block'
            break;
        case 'fhir':
            fhirStyle = 'block'
            hermesOnlyToggle = true
            trezeptCheckBox.checked = false
            break;
    }

    trezeptCheckBox.disabled = hermesOnlyToggle

    const sendbtn = document.getElementById('send-request');
    sendbtn.textContent = sendButtonText;
    sendbtn.className = `mt-3 col btn ${sendButtonStyle}`;

    const inputMask = document.getElementById('input-mask');
    inputMask.style.display = hermesStyle;

    const fhirMask = document.getElementById('fhir-input-mask');
    fhirMask.style.display = fhirStyle;
}
