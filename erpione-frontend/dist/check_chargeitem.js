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

(function () {
    const PDFJS_VERSION = '3.11.174';
    const PDFJS_WORKER_SRC = `https://cdn.jsdelivr.net/npm/pdfjs-dist@${PDFJS_VERSION}/build/pdf.worker.min.js`;

    let modalReadyPromise = null;
    let modalInstance = null;
    let currentObjectUrls = [];

    function openCheckChargeItemOverlay() {
        ensureModalReady()
            .then(() => {
                resetState();
                modalInstance.show();
            })
            .catch((err) => {
                appendAlertSafely(resolveErrorMessage(err, 'Unexpected error while opening check charge item overlay'), 'danger');
            });
    }

    function ensureModalReady() {
        if (modalReadyPromise) return modalReadyPromise;
        modalReadyPromise = fetch('check_chargeitem.html')
            .then((response) => {
                if (!response.ok) throw new Error('Unable to load check charge item overlay');
                return response.text();
            })
            .then((html) => {
                if (!document.getElementById('check-chargeitem-modal')) {
                    const wrapper = document.createElement('div');
                    wrapper.innerHTML = html;
                    document.body.appendChild(wrapper.querySelector('#check-chargeitem-modal'));
                }
                const modalElement = document.getElementById('check-chargeitem-modal');
                modalInstance = new bootstrap.Modal(modalElement);

                const fileInput = document.getElementById('check-chargeitem-file');
                const checkBtn = document.getElementById('check-chargeitem-check-btn');

                fileInput.addEventListener('change', () => {
                    hideError();
                    hideResult();
                    checkBtn.disabled = !(fileInput.files && fileInput.files.length > 0);
                });

                checkBtn.addEventListener('click', () => {
                    const file = fileInput.files && fileInput.files[0];
                    if (!file) return;
                    checkPdf(file);
                });

                modalElement.addEventListener('hidden.bs.modal', resetState);
            });
        return modalReadyPromise;
    }

    function checkPdf(file) {
        hideError();
        hideResult();

        if (!window.pdfjsLib) {
            showError('Die PDF-Bibliothek konnte nicht geladen werden.');
            return;
        }
        pdfjsLib.GlobalWorkerOptions.workerSrc = PDFJS_WORKER_SRC;

        showSpinner();
        file.arrayBuffer()
            .then((data) => pdfjsLib.getDocument({data}).promise)
            .then((pdf) => collectAttachments(pdf))
            .then((attachments) => {
                hideSpinner();
                const matches = attachments.filter(isPkcs7Attachment);
                if (matches.length === 0) {
                    showError('Die PDF enthält keine PS7-Datei (PKCS#7).');
                    return;
                }
                showResult(matches);
            })
            .catch((err) => {
                hideSpinner();
                const isPdfError = err && (err.name === 'InvalidPDFException' || err.name === 'MissingPDFException' || err.name === 'PasswordException');
                showError(isPdfError
                    ? 'Die Datei konnte nicht als PDF gelesen werden.'
                    : resolveErrorMessage(err, 'Unerwarteter Fehler beim Prüfen der PDF.'));
            });
    }

    // Collects document-level embedded files and page-level FileAttachment annotations.
    function collectAttachments(pdf) {
        const result = [];
        return pdf.getAttachments()
            .then((attachments) => {
                if (attachments) {
                    Object.values(attachments).forEach((att) => result.push(att));
                }
                const pagePromises = [];
                for (let i = 1; i <= pdf.numPages; i++) {
                    pagePromises.push(
                        pdf.getPage(i)
                            .then((page) => page.getAnnotations())
                            .then((annotations) => {
                                annotations
                                    .filter((a) => a.subtype === 'FileAttachment' && a.file && a.file.content)
                                    .forEach((a) => result.push(a.file));
                            })
                    );
                }
                return Promise.all(pagePromises);
            })
            .then(() => result);
    }

    function isPkcs7Attachment(att) {
        if (!att || !att.content) return false;
        const name = (att.filename || att.rawFilename || '').toLowerCase();
        if (name.endsWith('.p7s')) return true;
        // pdf.js does not consistently expose the embedded file's MIME type; check if present.
        const mime = (att.mimeType || att.subtype || att.contentType || '').toLowerCase();
        return mime.includes('pkcs7');
    }

    function showResult(matches) {
        revokeObjectUrls();
        const list = document.getElementById('check-chargeitem-result-list');
        list.innerHTML = '';
        const usedNames = new Map();

        matches.forEach((att) => {
            const displayName = att.filename || 'kostenbeleg.p7s';
            const downloadName = uniqueDownloadName(displayName, usedNames);
            const blob = new Blob([att.content], {type: 'application/pkcs7-mime'});
            const url = URL.createObjectURL(blob);
            currentObjectUrls.push(url);

            const item = document.createElement('li');
            item.className = 'list-group-item d-flex justify-content-between align-items-center';

            const label = document.createElement('span');
            const name = document.createElement('strong');
            name.textContent = displayName;
            label.appendChild(name);
            label.appendChild(document.createTextNode(` (${att.content.length} Bytes)`));

            const link = document.createElement('a');
            link.className = 'btn btn-success btn-sm';
            link.href = url;
            link.download = downloadName;
            link.textContent = 'Herunterladen';

            item.appendChild(label);
            item.appendChild(link);
            list.appendChild(item);
        });

        document.getElementById('check-chargeitem-result-heading').textContent =
            matches.length === 1 ? '1 PS7-Datei gefunden:' : `${matches.length} PS7-Dateien gefunden:`;
        document.getElementById('check-chargeitem-result').style.display = 'block';
    }

    // Avoids identical download names when several attachments share a filename.
    function uniqueDownloadName(name, usedNames) {
        const count = (usedNames.get(name) || 0) + 1;
        usedNames.set(name, count);
        if (count === 1) return name;
        const dot = name.lastIndexOf('.');
        return dot > 0 ? `${name.slice(0, dot)} (${count})${name.slice(dot)}` : `${name} (${count})`;
    }

    function hideResult() {
        const el = document.getElementById('check-chargeitem-result');
        if (el) el.style.display = 'none';
    }

    function showError(message) {
        const el = document.getElementById('check-chargeitem-error');
        el.textContent = message;
        el.style.display = 'block';
    }

    function hideError() {
        const el = document.getElementById('check-chargeitem-error');
        if (el) el.style.display = 'none';
    }

    function revokeObjectUrls() {
        currentObjectUrls.forEach((url) => URL.revokeObjectURL(url));
        currentObjectUrls = [];
    }

    function resetState() {
        revokeObjectUrls();
        hideError();
        hideResult();
        const fileInput = document.getElementById('check-chargeitem-file');
        const checkBtn = document.getElementById('check-chargeitem-check-btn');
        if (fileInput) fileInput.value = '';
        if (checkBtn) checkBtn.disabled = true;
    }

    function resolveErrorMessage(err, msg) {
        if (!err) return msg;
        if (typeof err === 'string') return err;
        return err.message || msg;
    }

    function appendAlertSafely(message, type) {
        if (window.appendAlert) {
            window.appendAlert(message, type);
            return;
        }
        alert(message);
    }

    window.openCheckChargeItemOverlay = openCheckChargeItemOverlay;
})();
