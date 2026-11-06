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

async function updateFdStatusBar() {
    const fdStatusBar = document.getElementById("fd-status");

    if (!fdStatusBar || !fdStatusBar.isConnected) {
        return;
    }
    fdStatusBar.textContent = "⚪ FD Status-Anfrage...";
    fdStatusBar.classList.toggle("text-info-emphasis", true);
    fdStatusBar.classList.toggle("bg-light", true);
    const availabilityScore = await checkFdAvailability();
    toggleFdStatusBar(availabilityScore);
}

function toggleFdStatusBar(availability) {
    const fdStatusBar = document.getElementById("fd-status");

    fdStatusBar.classList.toggle("bg-success", false);
    fdStatusBar.classList.toggle("text-success-emphasis", false);

    fdStatusBar.classList.toggle("bg-warning", false);
    fdStatusBar.classList.toggle("text-warning-emphasis", false);

    fdStatusBar.classList.toggle("bg-primary-subtle", false);
    fdStatusBar.classList.toggle("text-primary-emphasis", false);

    fdStatusBar.classList.toggle("bg-danger-subtle", false);
    fdStatusBar.classList.toggle("text-danger-emphasis", false);

    fdStatusBar.classList.toggle("bg-light", false);
    fdStatusBar.classList.toggle("text-info-emphasis", false);

    switch (availability) {
        case 2:
            fdStatusBar.textContent = "🟢 FD erreichbar";
            fdStatusBar.classList.toggle("text-success-emphasis", true);
            fdStatusBar.classList.toggle("bg-success", true);
            break;
        case 1:
            fdStatusBar.textContent = "🟡 FD instabil";
            fdStatusBar.classList.toggle("text-warning-emphasis", true);
            fdStatusBar.classList.toggle("bg-warning", true);
            break;
        case 0:
            fdStatusBar.textContent = "🔵 FD Status unbekannt";
            fdStatusBar.classList.toggle("bg-primary-subtle", true);
            fdStatusBar.classList.toggle("text-primary-emphasis", true);
            break;
        case -1:
            fdStatusBar.textContent = "🔴 FD unerreichbar";
            fdStatusBar.classList.toggle("text-danger-emphasis", true);
            fdStatusBar.classList.toggle("bg-danger-subtle", true);
            break;
        default:
            fdStatusBar.textContent = "⚪ FD Status-Anfrage...";
            fdStatusBar.classList.toggle("text-info-emphasis", true);
            fdStatusBar.classList.toggle("bg-light", true);
    }
}

async function checkFdAvailability() {
    try {
        const [ capabilityResult,
                healthResult] = await Promise.allSettled([
            fetch(`/api/erp/health`, {
                cache: 'no-store',
                credentials: 'same-origin',
                headers: {
                    'x-actor': 'machaon'
                }
            }).then(response => checkForLogin(response)),
            fetch('/api/erp/meta', {
                cache: 'no-store',
                credentials: 'same-origin',
                headers: {
                    'x-actor': 'machaon'
                }
            }).then(response => checkForLogin(response))
        ]);
        let fdCapability = capabilityResult.status === 'fulfilled' && capabilityResult.value.ok ? 1 : 0;
        let fdHealth = healthResult.status === 'fulfilled' && healthResult.value.ok ? 1 : 0;
        console.log("FD-Health: " + fdHealth + ", FD-Capability: " + fdCapability);
        return ((fdCapability + fdHealth) === 0) ? -1 : fdCapability + fdHealth;
    } catch (error) {
        console.log("error during FD check:", error);
        handleUnexpectedFetchError(error);
        return 0;
    }
}

/**
 * this function is used whenever an issue related to user data and/or cookies is detected.
 * It clears local storage and can optionally clear the CSRF cookie as well.
 */
function clearUserData(clearXsrfCookie = true) {
    localStorage.removeItem('userData');
    if (clearXsrfCookie) {
        document.cookie = 'XSRF-TOKEN=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    }
}

function fetchUserData() {
    fetch('/bff/user')
        .then(response => checkForLogin(response))
        .then(response => response.json())
        .then(data => {
            applyUserData(data);
            localStorage.setItem('userData', JSON.stringify(data));
        })
        .catch(error => {
            if (error && error.message === 'redirecting to login') {
                return;
            }
            handleUnexpectedFetchError(error);
        });
}

function applyUserData(data) {
    const profileContainer = findElement('profile-container');
    const profilePic = findElement('profile-pic');
    const profileUsernameItem = findElement('profile-username-item');

    if (!data || !data.username) {
        if (profileContainer) profileContainer.style.display = 'none';
        return;
    }

    if (profilePic) {
        if (data.picture) {
            profilePic.src = data.picture;
        } else {
            profilePic.src = "img/default-profile.png";
        }
    }

    if (data.username && profileUsernameItem) profileUsernameItem.textContent = data.username;
    if (profileContainer) profileContainer.style.display = 'flex';
}

function openKeycloakAccountManagement() {
    console.log("Opening Keycloak account management in a new tab");
    window.open('/kc/realms/erpione/account', '_blank');
}

async function triggerLogout() {
    const xsrfToken = getXsrfToken();
    if (!xsrfToken) {
        appendAlert("Missing XSRF token for logout", "danger");
        return;
    }

    try {
        const response = await fetch('/logout', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'X-XSRF-TOKEN': xsrfToken
            }
        });

        if (!response.ok && !response.redirected) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        clearUserData();
        window.location.replace(response.redirected && response.url ? response.url : '/');
    } catch (error) {
        appendAlert(`Logout failed: ${error}`, 'danger');
    }
}

function ensureHeadlessTokenModule() {
    if (window.openHeadlessTokenOverlay) return Promise.resolve();
    return new Promise((resolve, reject) => {
        const existing = document.getElementById('headless-token-js');
        if (existing) {
            existing.addEventListener('load', () => resolve());
            existing.addEventListener('error', () => reject('script load failed'));
            return;
        }
        const script = document.createElement('script');
        script.id = 'headless-token-js';
        script.src = 'headless_token.js';
        script.onload = () => resolve();
        script.onerror = () => reject('script load failed');
        document.head.appendChild(script);
    });
}

function openTokenDialog() {
    ensureHeadlessTokenModule()
        .then(() => window.openHeadlessTokenOverlay())
        .catch((err) => appendAlert(`Failed to load Headless Token UI: ${err}`, 'danger'));
}

// I will burn in hell for that (Dont worry, its cold in hell)
window.addEventListener('DOMContentLoaded', async function() {
    const navContainer = document.createElement('div');

    navContainer.innerHTML = await fetch('nav.html').then(r => r.text());
    document.body.insertBefore(navContainer, document.body.firstChild);
    document.title = `Erpione ${window.ERPIONE_ENV}`;

    findElement('env-highlight').textContent = window.ERPIONE_ENV;
    const fdStatusBar = findElement("fd-status");
    const intervalId = setInterval(() => {
        if (!fdStatusBar || !fdStatusBar.isConnected) {
            clearInterval(intervalId);
            return;
        }
        // periodic task here
        updateFdStatusBar();
    }, 300000);
    updateFdStatusBar(); // initial call to set the status bar immediately

    let userData = localStorage.getItem('userData');
    if (!userData) {
        fetchUserData(); //user-data is applied automatically after fetching
    } else {
        applyUserData(JSON.parse(userData));
    }
});
