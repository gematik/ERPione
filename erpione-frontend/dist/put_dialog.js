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
    let modalReadyPromise = null;
    let modalInstance = null;

    const examplePayload = {
        currency: "EUR",
        total: {
            insurantCost: "42.0",
            totalCost: "99.0",
        }
    };

    function openPutDialogOverlay(erpTask, actor) {
        ensurePutModalReady()
            .then(() => {
                const modalElement = document.getElementById('put-dialog-modal');
                modalElement.dataset.erpTask = JSON.stringify(erpTask);
                modalElement.dataset.actor = actor;
                modalInstance.show();
            }).catch((err) => {
                appendAlertSafely(resolveErrorMessage(err, 'Unexpected error while opening put dialog overlay'), 'danger');
            });
    }

    function ensurePutModalReady() {
        if (modalReadyPromise) return modalReadyPromise;
        modalReadyPromise = fetch('put_dialog.html')
            .then((response) => {
                if (!response.ok) throw new Error('Unable to load put dialog overlay');
                return response.text();
            })
            .then((html) => {
                if (!document.getElementById('put-dialog-modal')) {
                    const wrapper = document.createElement('div');
                    wrapper.innerHTML = html;
                    const modalElement = wrapper.querySelector('#put-dialog-modal');
                    document.body.appendChild(modalElement);
                }
                const modalElement = document.getElementById('put-dialog-modal');
                modalInstance = new bootstrap.Modal(modalElement);
                document.getElementById('put-dialog-textarea').value = JSON.stringify(examplePayload, null, 2);
                const sendBtn = document.getElementById('put-dialog-send-btn');
                if (sendBtn) {
                    sendBtn.addEventListener('click', () => {
                        const erpTask = modalElement.dataset.erpTask ? JSON.parse(modalElement.dataset.erpTask) : null;
                        const actor = modalElement.dataset.actor || null;
                        const newItem = document.getElementById('put-dialog-textarea').value;
                        const chargeAc = document.getElementById('put-dialog-ac').value;
                        sendPutRequest(newItem, chargeAc, erpTask, actor);
                    });
                }
            });
        return modalReadyPromise;
    }

    function sendPutRequest(newItem, chargeAc, erpTask, actor) {
        const taskId = erpTask.taskId
        const csrfToken = getXsrfToken();
        const data = JSON.parse(newItem);

        showSpinner();

        fetch(`/api/erp/chargeitems/${encodeURIComponent(taskId)}?ac=${encodeURIComponent(chargeAc)}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-Token': csrfToken,
                'x-actor': actor
            },
            credentials: "same-origin",
            body: JSON.stringify(data)
        })
            .then(response => checkForLogin(response))
            .then(async response => {
                const data = await response.json();
                return [response.ok, data];
            })
            .then(result => {
                hideSpinner();
                const [ok, data] = result;
                if (!ok) {
                    appendAlertSafely(data?.message || 'Unexpected error while sending put request', 'danger');
                } else {
                    appendAlertSafely('Put request sent successfully', 'success');
                }
            })
            .catch(err => {
                hideSpinner();
                appendAlertSafely(resolveErrorMessage(err, 'Unexpected error while sending put request'), 'danger');
            });
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

    window.openPutDialogOverlay = openPutDialogOverlay;
})();
