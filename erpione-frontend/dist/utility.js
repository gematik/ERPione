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

function findElement(id) {
    return document.getElementById(id);
}

function randomAsvNumber(){
    let asvNumber = "00";
    let checksum = 0;

    for (let i = 0; i < 6; i++) {
        let randomDigit = Math.floor(Math.random() * 10) % 10;
        asvNumber += randomDigit;

        checksum += i % 2 === 0 ? randomDigit * 4 : randomDigit * 9;
    }
    asvNumber += checksum % 10 === 0 ? 0 : 10 - (checksum % 10); //last digit is difference of checksum to next multiple of 10
    document.getElementById("asvnr").value = asvNumber;
}

function randomDigits(count) {
    let digits = "";
    for (let i = 0; i < count; i++) {
        digits += Math.floor(Math.random() * 10);
    }
    return digits;
}

function randomBsnr() {
    // WOP code 01-98 followed by 7 random digits
    let wop = String(Math.floor(Math.random() * 98) + 1).padStart(2, "0");
    return wop + randomDigits(7);
}

// Control digit: 1st/3rd/5th digit weighted 4, 2nd/4th/6th weighted 9; result = 10 - (sum % 10), with 10 -> 0
function generateControlNo(docId) {
    let sum = 0;
    for (let i = 0; i < docId.length; i++) {
        sum += parseInt(docId[i], 10) * (i % 2 === 0 ? 4 : 9);
    }
    return (10 - (sum % 10)) % 10;
}

function randomLanr() {
    let docId = randomDigits(6);
    return docId + generateControlNo(docId) + randomDigits(2);
}

function randomZanr() {
    let docId = randomDigits(6);
    let sectionNo = Math.random() < 0.5 ? "50" : "91";
    return docId + generateControlNo(docId) + sectionNo;
}

function randomAnrBsnrNumbers() {
    const anrType = document.getElementById("anr-select").value

    let anr = anrType === "ZANR" ? randomZanr() : randomLanr()
    let bsnr = randomBsnr()

    document.getElementById("anr").value = anr;
    document.getElementById("bsnr").value = bsnr;
}

function createAccordion(parent, data, itemRenderer) {
    if (!data) {
        parent.innerHTML = '<p>Keine Elemente gefunden!</p>';
        return;
    }
    let accordion = document.createElement('div')
    accordion.className = 'accordion'
    parent.append(accordion)

    data.forEach(it => {
        let card = document.createElement('div')
        card.innerHTML = itemRenderer(it)
        accordion.append(card)
    })
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(function() {
        // Optionally show a confirmation (e.g., toast or alert)
        appendAlert('Copied to clipboard', 'success');
    }, function(err) {
        appendAlert('Failed to copy: ' + err, 'danger');
    });
}

function showSpinner() {
    let spinner = document.getElementById('fetch-spinner-backdrop');
    if (spinner) spinner.style.display = 'flex';
}

function hideSpinner() {
    const spinner = document.getElementById('fetch-spinner-backdrop');
    if (spinner) spinner.style.display = 'none';
}

function getXsrfToken() {
    const token = (RegExp(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/).exec(document.cookie) || [])[1] || ''

    return decodeURIComponent(token)
}

/**
 * if response from server is not authenticated then relocate to /login
 * @param response
 */
function checkForLogin(response) {
    if (response.redirected || response.status === 401) {
        window.location.replace('/oauth2/authorization/oidc');
        return Promise.reject(new Error('redirecting to login'));
    }

    return response;
}

const appendAlert = (message, type) => {
    const alertDiv = document.getElementById('alert-container')
    if (!alertDiv) return

    const existingAlert = alertDiv.querySelector('.alert')
    if (existingAlert && existingAlert.parentElement) {
        if (existingAlert.dataset.timeoutId) {
            clearTimeout(Number(existingAlert.dataset.timeoutId))
        }
        existingAlert.parentElement.remove()
    }

    const wrapper = document.createElement('div')
    wrapper.innerHTML = [
        `<div class="alert alert-${type} alert-dismissible fade show" role="alert">`,
        `   <div>${message}</div>`,
        '   <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>',
        '</div>'
    ].join('')

    alertDiv.append(wrapper)

    const timeoutId = setTimeout(function() {
        if (wrapper.parentElement === alertDiv) {
            alertDiv.removeChild(wrapper)
        }
    }, 6000);

    const alertElement = wrapper.querySelector('.alert')
    if (alertElement) {
        alertElement.dataset.timeoutId = String(timeoutId)
        alertElement.addEventListener('closed.bs.alert', function() {
            clearTimeout(timeoutId)
            if (wrapper.parentElement === alertDiv) {
                alertDiv.removeChild(wrapper)
            }
        })
    }
}
