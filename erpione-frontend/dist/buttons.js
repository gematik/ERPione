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

function serializeFragment(fragment) {
    const temp = document.createElement('div');
    temp.appendChild(fragment.cloneNode(true));
    return temp.innerHTML;
}

function innerHtmlForRawDataButton(data) {
    const fragment = createRawDataButton(data);
    return serializeFragment(fragment);
}

function createEditPrescriptionButton(data) {
    const acq = (data.accessCode) ? `&ac=${data.accessCode}` : '';
    const editPrescriptionQuery = `pid=${data.taskId}${acq}`;
    const locationHref = `prescription.html?${editPrescriptionQuery}`

    const group = document.createElement("div")
    group.className = "btn-group"

    const button = document.createElement("button")
    button.innerText = `E-Rezept ${data.taskId} bearbeiten`
    button.className = "btn btn-primary mb-2"
    button.onclick = function () {
        location.href = locationHref
    }
    button.setAttribute("role", "button")
    button.setAttribute("aria-expanded", "false")

    group.appendChild(button)
    return group
}

function createRawDataButton(data) {
    const generateId = Math.random().toString(36).substring(2, 15);

    // Create button
    const button = document.createElement("button");
    button.className = "btn btn-outline-info";
    button.setAttribute("data-bs-toggle", "collapse");
    button.setAttribute("href", `#raw-data-collapse-${generateId}`);
    button.setAttribute("role", "button");
    button.setAttribute("aria-expanded", "false");
    button.setAttribute("aria-controls", `raw-data-collapse-${generateId}`);
    button.innerText = "JSON Data";


    // Button wrapper
    const pflex = document.createElement("p");
    pflex.className = "d-inline-flex gap-1";
    pflex.appendChild(button);

    // Highlighted code
    // const prehl = getHighlightedData(data);
    const prehl = getHighlightedCodeBox(data);

    prehl.classList.add("card", "card-body")
    const cardBody = prehl;
    // Card body
    // const cardBody = document.createElement("div");
    // cardBody.className = "card card-body";
    // cardBody.id = `data-body-collapse-${generateId}`;
    // cardBody.appendChild(prehl);

    // Collapse div
    const collapse = document.createElement("div");
    collapse.className = "collapse";
    collapse.id = `raw-data-collapse-${generateId}`;
    collapse.appendChild(cardBody);

    // Container fragment
    const fragment = document.createDocumentFragment();
    fragment.appendChild(pflex);
    fragment.appendChild(collapse);

    return fragment;
}


function prepareData(data) {
    let sdata;
    let language = 'json';
    if (data instanceof Object || data instanceof Array) {
        sdata = JSON.stringify(data, null, 4)
    }
    else if (data.startsWith("<")) {
        sdata = escapeXml(prettifyXml(data))
        language = 'xml'
    } else {
        const json = JSON.parse(data)
        sdata = JSON.stringify(json, null, 4)
    }

    return [language, sdata];
}

function escapeXml(xml) {
    return xml
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

function getHighlightedData(data, editable = false) {
    const [language, sdata] = prepareData(data);

    const pre = document.createElement("pre");
    pre.innerHTML = `<code class="language-${language}" contenteditable="${editable}">${sdata}</code>`

    // Highlight the code
    pre.querySelector('code').classList.add('hljs');
    if (window.hljs) hljs.highlightElement(pre.querySelector('code'));

    return pre
}

function getHighlightedCodeBox(data, label = "Data", editable = false) {
    const container = document.createElement("div");
    container.className = "container border rounded bg-info-subtle p-2 position-relative";

    // Create the copy button
    const copyBtn = document.createElement("button");
    copyBtn.type = "button";
    copyBtn.className = "btn btn-sm btn-light";
    copyBtn.title = "Copy to clipboard";
    // Use Bootstrap icon if available, else fallback to Unicode
    copyBtn.innerHTML = '📋';

    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.className = "btn btn-sm btn-light";
    removeBtn.title = "Remove Codebox from view";
    removeBtn.innerHTML = '✖️';

    const buttonGroup = document.createElement("div");
    buttonGroup.className =
    "position-absolute top-0 end-0 m-2 d-flex gap-1";
    buttonGroup.appendChild(copyBtn);
    buttonGroup.appendChild(removeBtn);

    // Highlighted code
    const pre = getHighlightedData(data, editable);
    container.appendChild(buttonGroup);
    container.appendChild(pre);

    // Copy functionality
    copyBtn.addEventListener("click", function () {
        // Get the code text
        const codeElem = pre.querySelector('code');
        if (codeElem) {
            const text = codeElem.innerText;
            navigator.clipboard.writeText(text).then(() => {
                copyBtn.classList.remove("btn-light");
                copyBtn.classList.add("btn-success");
                setTimeout(() => {
                    copyBtn.classList.remove("btn-success");
                    copyBtn.classList.add("btn-light");
                }, 1200);
            });
        }
    });
    //Remove current codebox
    removeBtn.addEventListener("click", function () {
        container.remove();
    })

    return container;
}

function startTutorial() {
    Tutorial.start();
    const newUserBanner = findElement('new-user-banner');
    newUserBanner.classList.toggle('d-none', true);
    newUserBanner.classList.toggle('d-flex', false);
    let userData = JSON.parse(localStorage.getItem('userData'));
    if(userData){
        userData.isNewUser = false;
        localStorage.setItem('userData', JSON.stringify(userData));
    }
}

// Source - https://stackoverflow.com/a/47317538
// Posted by Klesun, modified by community. See post 'Timeline' for change history
// Retrieved 2026-10-13, License - CC BY-SA 4.0
var prettifyXml = function(sourceXml)
{
    var xmlDoc = new DOMParser().parseFromString(sourceXml, 'application/xml');
    var xsltDoc = new DOMParser().parseFromString([
        // describes how we want to modify the XML - indent everything
        '<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform">',
        '  <xsl:strip-space elements="*"/>',
        '  <xsl:template match="para[content-style][not(text())]">', // change to just text() to strip space in text nodes
        '    <xsl:value-of select="normalize-space(.)"/>',
        '  </xsl:template>',
        '  <xsl:template match="node()|@*">',
        '    <xsl:copy><xsl:apply-templates select="node()|@*"/></xsl:copy>',
        '  </xsl:template>',
        '  <xsl:output indent="yes"/>',
        '</xsl:stylesheet>',
    ].join('\n'), 'application/xml');

    var xsltProcessor = new XSLTProcessor();
    xsltProcessor.importStylesheet(xsltDoc);
    var resultDoc = xsltProcessor.transformToDocument(xmlDoc);
    var resultXml = new XMLSerializer().serializeToString(resultDoc);
    return resultXml;
};
