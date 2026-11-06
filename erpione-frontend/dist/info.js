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

function startInfoRequests() {
    showSpinner()
    fetch('/api/info')
        .then(response => checkForLogin(response))
        .then(async response => {
            const json = await response.json();
            return [response.ok, json];
        })
        .then(result => {
            const [ok, data] = result;
            hideSpinner();
            const erpioneStatus = findElement('erpione-status');
            if(!ok){
                erpioneStatus.classList.toggle("text-success", false);
                erpioneStatus.textContent = "Erpione ist nicht erreichbar";
                erpioneStatus.classList.toggle("text-danger", true);
                return;
            }
            erpioneStatus.classList.toggle("text-danger", false);
            erpioneStatus.textContent = "Erpione erreichbar";
            erpioneStatus.classList.toggle("text-success", true);

            const version = findElement('erpione-build-version');
            version.textContent = data.build.version ? data.build.version : 'LOCAL';
            const buildDate = findElement('erpione-build-date');
            buildDate.textContent = data.build.buildDate ? data.build.buildDate : 'Unbekannt';
            const fhirView = findElement('erpione-fhir-version');
            fhirView.textContent = data.fhir.view;
            const erpUrl = findElement('erpione-erp-url');
            erpUrl.textContent = data.ti.services.erp.url;
            const idpUrl = findElement('erpione-idp-url');
            idpUrl.textContent = data.ti.services.idp.url;
            const vauCert = findElement('erpione-vau-certificate');
            vauCert.textContent = data.ti.services.erp.certificate;

            const container = findElement('meta-container');
            container.append(createRawDataButton(data));
            pingFdHealth();
        }).catch(handleUnexpectedFetchError);
}

function pingFdHealth() {
    showSpinner()
    fetch('/api/erp/health', {
        credentials: "same-origin",
        headers: {
            'x-actor': "machaon"
        }
    })
        .then(response => checkForLogin(response))
        .then(async response => {
            const json = await response.json();
            return [response.ok, json];
        })
        .then(result => {
            const [ok, data] = result;
            hideSpinner();
            const fdStatus = findElement('fd-health');
            const fdLatency = findElement('fd-latency');
            if(!ok) {
                fdStatus.classList.toggle("text-success", false);
                fdStatus.textContent = "Fachdienst unerreichbar";
                fdStatus.classList.toggle("text-danger", true);
                return;
            }

            if(data.status === 'OK') {
                fdStatus.classList.toggle("text-danger", false);
                fdStatus.textContent = "Fachdienst erreichbar";
                fdStatus.classList.toggle("text-success", true);
                fdLatency.textContent = `${data.latency} ms`;
            }
            populateFdInfo();
        }).catch(handleUnexpectedFetchError);
}

function populateFdInfo() {
    showSpinner()
    fetch('/api/erp/meta', {
        credentials: "same-origin",
        headers: {
            'x-actor': "machaon"
        }
    })
        .then(response => checkForLogin(response))
        .then(async response => {
            const json = await response.json();
            return [response.ok, json];
        })
        .then(result => {
            const [ok, data] = result;
            hideSpinner();
            if(!ok){
                const fdStatus = findElement('fd-health');
                fdStatus.classList.toggle("text-success", false);
                fdStatus.textContent = "Fachdienst VAU-Verbindung gestört";
                fdStatus.classList.toggle("text-danger", true);
                return;
            }

            const software = data.software || {}
            const implementation = data.implementation || {}
            const title = findElement('fd-title');
            title.textContent = data.title || 'E-Rezept Fachdienst: error fetching information';
            const fdSoftware = findElement('fd-software');
            fdSoftware.textContent = `${implementation.description} ${software.name}`;
            const fdVersion = findElement('fd-version');
            fdVersion.textContent = software.version;
            const fdReleaseDate = findElement('fd-release-date');
            fdReleaseDate.textContent = software.releaseDate;
            const fdBuildDate = findElement('fd-build-date');
            fdBuildDate.textContent = data.date;

            const container = findElement('fd-container');
            container.append(createRawDataButton(data));
        }).catch(handleUnexpectedFetchError);
}
