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

        version: 3,
        text: "Vielen Dank für Ihre Bestellung!",
        transactionID: "ABCD-EFGH-IJKL-MNOP",
        communicationType: "text"
    };

    function openComReplyOverlay(erpTask, actor) {
        ensureModalReady()
            .then(() => {
                document.getElementById('com-reply-textarea').value = '';
                const modalElement = document.getElementById('com-reply-modal');
                modalElement.dataset.erpTask = JSON.stringify(erpTask);
                modalElement.dataset.actor = actor;
                modalInstance.show();
            })
            .catch((err) => {
                appendAlertSafely(resolveErrorMessage(err, 'Unexpected error while opening communication reply overlay'), 'danger');
            });
    }

    function ensureModalReady() {
        if (modalReadyPromise) return modalReadyPromise;
        modalReadyPromise = fetch('com_reply.html')
            .then((response) => {
                if (!response.ok) throw new Error('Unable to load communication reply overlay');
                return response.text();
            })
            .then((html) => {
                if (!document.getElementById('com-reply-modal')) {
                    const wrapper = document.createElement('div');
                    wrapper.innerHTML = html;
                    const modalElement = wrapper.querySelector('#com-reply-modal');
                    document.body.appendChild(modalElement);
                }
                const modalElement = document.getElementById('com-reply-modal');
                modalInstance = new bootstrap.Modal(modalElement);
                document.getElementById('com-reply-textarea').placeholder = JSON.stringify(examplePayload, null, 2);
                const sendBtn = document.getElementById('com-reply-send-btn');
                if (sendBtn) {
                    sendBtn.addEventListener('click', () => {
                        const erpTask = modalElement.dataset.erpTask ? JSON.parse(modalElement.dataset.erpTask) : null;
                        const actor = modalElement.dataset.actor || null;
                        const message = document.getElementById('com-reply-textarea').value;
                        sendComReply(message, erpTask, actor);
                    });
                }
            });
        return modalReadyPromise;
    }

    function sendComReply(message, erpTask, actor) {
        const taskId = erpTask.taskId
        const csrfToken = getXsrfToken()
        const data = JSON.parse(message);
        const body = {
            message: data
        }

        showSpinner()
        fetch(`/api/erp/communications/${taskId}/reply`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-Token': csrfToken,
                'x-actor': actor
            },
            credentials: "same-origin",
            body: JSON.stringify(body)
        })
        .then(response => checkForLogin(response))
        .then(async response => {
            const data = await response.json();
            return [response.ok, data];
        })
        .then(result => {
            hideSpinner()
            const [ok, data] = result;
            const codeBox = getHighlightedCodeBox(data)

            if (!ok) {
                appendAlert(`Could not send communication reply for task ${taskId}`, 'danger');
            } else {
                appendAlert(
                    `Successfully sent communication reply for task ${taskId}`, 'success');
            }
            codeBox.prepend(createEditPrescriptionButton(data))
            document.getElementById('response-container-overlay').prepend(codeBox);
        })
        .catch(handleUnexpectedFetchError);
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

    window.openComReplyOverlay = openComReplyOverlay;
})();
