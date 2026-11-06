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
    let currentToken = '';

    function openHeadlessTokenOverlay() {
        showSpinnerSafely();
        return requestHeadlessToken()
            .then((responseBody) => ensureModalReady().then(() => renderAndShow(responseBody)))
            .catch((err) => {
                appendAlertSafely(resolveErrorMessage(err), 'danger');
            })
            .finally(() => hideSpinnerSafely());
    }

    function requestHeadlessToken() {
        const xsrfToken = decodeURIComponent((window.getXsrfToken && window.getXsrfToken()) || '');
        if (!xsrfToken) {
            return Promise.reject('Missing XSRF token');
        }

        return fetch('/bff/delegations', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': xsrfToken
            },
            body: JSON.stringify({
                scope: ['openid'],
                ttlSeconds: 900
            })
        }).then((response) => {
            if (!response.ok) {
                return response
                    .json()
                    .catch(() => ({}))
                    .then((data) => {
                        const message = (data && data.message) ? data.message : `HTTP ${response.status} ${response.statusText}`;
                        throw new Error(message);
                    });
            }
            return response.json();
        });
    }

    function ensureModalReady() {
        if (modalReadyPromise) return modalReadyPromise;
        modalReadyPromise = fetch('headless_token.html')
            .then((response) => {
                if (!response.ok) throw new Error('Unable to load headless token overlay');
                return response.text();
            })
            .then((html) => {
                if (!document.getElementById('headless-token-modal')) {
                    const wrapper = document.createElement('div');
                    wrapper.innerHTML = html;
                    const modalElement = wrapper.querySelector('#headless-token-modal');
                    document.body.appendChild(modalElement);
                }
                const modalElement = document.getElementById('headless-token-modal');
                modalInstance = new bootstrap.Modal(modalElement);
                const copyBtn = document.getElementById('headless-token-copy-btn');
                if (copyBtn) {
                    copyBtn.addEventListener('click', () => {
                        if (window.copyToClipboard) {
                            window.copyToClipboard(currentToken);
                        } else {
                            navigator.clipboard.writeText(currentToken);
                        }
                    });
                }
            });
        return modalReadyPromise;
    }

    function renderAndShow(responseBody) {
        const tokenValueNode = document.getElementById('headless-token-value');
        const tokenMetaNode = document.getElementById('headless-token-meta');
        const rawContainerNode = document.getElementById('headless-token-raw-container');
        currentToken = (responseBody && responseBody.delegationToken) || '';

        if (!currentToken) {
            throw new Error('No delegationToken in response');
        }

        const formatMinutes = (value) =>
            Number.isInteger(value) ? `${value}` : value.toFixed(1);

        tokenValueNode.textContent = currentToken;
        const requested = (responseBody.requestedExpiresIn ?? 900) / 60;
        const effective = (responseBody.expiresIn ?? requested) / 60;
        tokenMetaNode.textContent = `requested ttl: ${formatMinutes(requested)}min, effective ttl: ${formatMinutes(effective)}min`;

        rawContainerNode.innerHTML = '';
        if (window.innerHtmlForRawDataButton) {
            rawContainerNode.innerHTML = window.innerHtmlForRawDataButton(responseBody);
        } else {
            const pre = document.createElement('pre');
            pre.className = 'mt-2 p-2 border rounded bg-light';
            pre.textContent = JSON.stringify(responseBody, null, 2);
            rawContainerNode.appendChild(pre);
        }

        modalInstance.show();
    }

    function resolveErrorMessage(err) {
        if (!err) return 'Unexpected error while creating headless token';
        if (typeof err === 'string') return err;
        return err.message || 'Unexpected error while creating headless token';
    }

    function appendAlertSafely(message, type) {
        if (window.appendAlert) {
            window.appendAlert(message, type);
            return;
        }
        alert(message);
    }

    function showSpinnerSafely() {
        if (window.showSpinner) window.showSpinner();
    }

    function hideSpinnerSafely() {
        if (window.hideSpinner) window.hideSpinner();
    }

    window.openHeadlessTokenOverlay = openHeadlessTokenOverlay;
})();
