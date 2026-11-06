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

function getActorRoleColor(role) {
    let color;
    switch (role) {
        case 'Gesundheitseinrichtung':
            color = 'rgb(34, 84, 148, 0.2)';
            break;
        case 'Apotheke':
            color = 'rgb(0, 154, 124, 0.2)';
            break;
        case 'Kostenträger':
            color = 'rgb(230, 230, 0, 0.2)';
            break;
        case 'nationale Kontaktstelle zum europäischen Gesundheitswesen':
            color = 'rgb(250, 30, 30, 0.2)';
            break;
        default:
            color = 'rgb(230, 230, 230, 0.2)';
    }
    return color;
}

function copyActorMnemonic(event, mnemonic) {
    event.preventDefault();
    event.stopPropagation();

    //replace html syntax
    mnemonic = String(mnemonic)
        .replace(/&/g, '&amp;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

    navigator.clipboard.writeText(mnemonic)
        .then(() => appendAlert('Mnemonic des Akteurs in die Zwischenablage kopiert', 'success'))
        .catch(err => appendAlert('Fehler beim Kopieren des Mnemonics: ' + err, 'danger'));
}

function createActorItem(actor) {
    let name = actor.name
    let mnemonic = actor.mnemonic
    const mid = actor.mnemonicId
    let smcb = actor.smcb
    let countryCode = actor.countryCode
    let practitioner = actor.practitioner || null
    let practitionerPostfix = (practitioner) ? `${practitioner.role} ${practitioner.name}` : name
    const actorRoleColor = getActorRoleColor(actor.role)
    const actorRoleClass = (actor.role === 'Gesundheitseinrichtung') ? 'actor-role-GHE' : (actor.role === 'Apotheke') ? 'actor-role-PHA' : (actor.role === 'Kostenträger') ? 'actor-role-KTR' : (actor.role === 'nationale Kontaktstelle zum europäischen Gesundheitswesen') ? 'actor-role-NCP' : ''
    const overlayClass = actorRoleClass === 'actor-role-NCP' ? 'eu-image-overlay' : '';
    return `
<div class="accordion-item" id="collapsable-actor-item-${mid}">
    <div class="accordion-header">
        <div class="d-flex align-items-stretch gap-2" style="background-color: ${actorRoleColor}">
                <button type="button"
                        class="btn btn-outline-primary flex-shrink-0 ms-2 my-2"
                        style="--bs-btn-border-color: ${actorRoleColor}; --bs-btn-hover-border-color: ${actorRoleColor}; --bs-btn-hover-bg: ${actorRoleColor};"
                        title="Copy actor mnemonic"
                        aria-label="Copy actor mnemonic"
                        onclick="copyActorMnemonic(event, '${mnemonic}')">
                    📋
                </button>
                <button class="accordion-button collapsed flex-grow-1" style="background-color: ${actorRoleColor}" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-actor-${mid}"
                        aria-expanded="true" aria-controls="collapse-actor-${mid}">
                <div class="d-flex flex-column pt-0 pb-0 text-start">
                    <div><span class="fw-bold h6">${mnemonic}</span> - ${practitionerPostfix}</div>
                    <small class="text-muted" style="font-size: 0.85em;">${smcb.profession}: ${name} (${countryCode})</small>
                </div>
                </button>
        </div>
    </div>
    <div id="collapse-actor-${mid}" class="accordion-collapse collapse" data-bs-parent="#collapsable-actor-item-${mid}">
        <div class="container accordion-body">
                <div class="row align-items-start">
                    <div class="col text-start border bg-light">
                            ${createInstitutionCard(name, smcb, countryCode)}
                            ${createPractitionerCard(practitioner)}
                            ${innerHtmlForRawDataButton(actor)}
                    </div>
                    <div class="col-auto">
                        <div class="p-3 ${overlayClass}">
                            <img src="img/${mnemonic.toLowerCase()}.png" class="img-thumbnail ${actorRoleClass}" style="max-height: 4em;" alt="${mnemonic}">
                        </div>
                    </div>
                </div>
        </div>
    </div>
</div>`
}

function createInstitutionCard(name, smcb, countryCode) {
    if (!smcb) return '';

    let profession = smcb.profession

    return `
<table class="table table-striped">
    <thead>
        <tr>
            <th colspan="2">${profession}: ${name} (${countryCode})</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Profession</td>
            <td>${smcb.profession} (OID: ${smcb.professionOid})</td>
        </tr>
        <tr>
            <td>TelematikId</td>
            <td>${smcb.telematikId}</td>
        </tr>
        <tr>
            <td>Valid until</td>
            <td>${smcb.validUntil}</td>
        </tr>
    </tbody>
</table>
`
}

function createPractitionerCard(practitioner) {
    if (!practitioner || !practitioner.hba) return '';

    let name = practitioner.name
    let hba = practitioner.hba
    let profession = hba.profession

    return `
<table class="table table-striped">
    <thead>
        <tr>
            <th colspan="2">${profession}: ${name}</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Profession</td>
            <td>${hba.profession}  (OID: ${hba.professionOid})</td>
        </tr>
        <tr>
            <td>TelematikId</td>
            <td>${hba.telematikId}</td>
        </tr>
        <tr>
            <td>Valid until</td>
            <td>${hba.validUntil}</td>
        </tr>
    </tbody>
</table>
`
}

function updateActorThumbnail() {
    const actor = document.getElementById('selected-actor');
    const mnemonic = actor.value;

    const actorImg = document.getElementById('actor-thumbnail');
    actorImg.src = `img/${mnemonic.toLowerCase()}.png`
}
