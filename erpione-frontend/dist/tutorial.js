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

(() => {

    class TutorialManager {
        //TODO add a tooltip to the Tutorial button to entice users
        constructor() {
            this.steps = [];
            this.currentIndex = 0;
            this.isStarted = false;
            this.overlay = null;
            this.highlight = null;
            this.bubble = null;
            this.titleEl = null;
            this.bodyEl = null;
            this.closeButton = null;
            this.nextButton = null;
            this.previousButton = null;
            this.resizeObserver = null;
            this.observedTargets = new Set();
            this.attachKeyboard();
        }

        attachKeyboard() {
            document.addEventListener("keydown", (event) => {
                if (!this.isStarted) {
                    return;
                }
                if (event.code === "Space" || event.key === " ") {
                    event.preventDefault();
                    if (!event.repeat) {
                        this.next();
                    }
                }
            });
        }

        setSteps(steps) {
            this.steps = Array.isArray(steps) ? steps.filter(Boolean) : [];
            this.currentIndex = 0;
        }

        toggleOverlay(state){
            this.overlay.classList.toggle("d-none", !state);
            this.overlay.classList.toggle("pe-none", !state);
            this.overlay.classList.toggle("pe-auto", state);
        }

        convertRemToPixels(rem) {
            return rem * parseFloat(getComputedStyle(document.documentElement).fontSize);
        }

        createDom() {
            this.overlay = findElement("tutorial-overlay");
            if (!this.overlay) {
                return;
            }

            if (!this.highlight) {
                const highlight = document.createElement("div");
                highlight.classList.add("position-absolute", "border", "border-3", "border-primary", "rounded-3", "pe-none", "tutorial-highlight");
                this.highlight = highlight;
                this.overlay.appendChild(this.highlight);
            }

            if (!this.bubble) {
                this.bubble = document.createElement("div");
                this.bubble.className = "tutorial-bubble";
                this.bubble.setAttribute("data-placement", "bottom");

                const header = document.createElement("div");
                header.classList.add("d-flex", "align-items-start", "justify-content-between", "gap-3", "mb-2");


                const title = document.createElement("div");
                title.classList.add("fw-bold", "fs-5");
                this.titleEl = title;

                const close = document.createElement("button");
                close.type = "button";
                close.classList.add("btn", "btn-close", "m-1");
                close.setAttribute(
                    "aria-label",
                    "Tutorial schließen"
                );
                this.closeButton = close;
                this.closeButton.addEventListener(
                    "click",
                    () => this.end()
                );

                const body = document.createElement("div");
                body.classList.add("lh-base", "mb-3", "text-body-secondary");
                this.bodyEl = body;

                const actions = document.createElement("div");
                actions.classList.add("d-flex", "justify-content-end");

                this.previousButton = document.createElement("button");
                this.previousButton.type = "button";
                this.previousButton.classList.add("btn", "btn-primary", "fw-semibold", "m-1", "rounded-3", "border-0");
                this.previousButton.textContent = "Zurück";
                this.previousButton.addEventListener(
                    "click",
                    () => this.previous()
                );

                this.nextButton = document.createElement("button");
                this.nextButton.type = "button";
                this.nextButton.classList.add("btn", "btn-primary", "fw-semibold", "m-1", "rounded-3", "border-0");
                this.nextButton.textContent = "Weiter";
                this.nextButton.addEventListener(
                    "click",
                    () => this.next()
                );

                header.appendChild(this.titleEl);
                header.appendChild(this.closeButton);

                actions.appendChild(this.previousButton);
                actions.appendChild(this.nextButton);

                this.bubble.appendChild(header);
                this.bubble.appendChild(body);
                this.bubble.appendChild(actions);
                this.overlay.appendChild(this.bubble);
            }
        }

        start() {
            if (!this.steps.length) {
                return;
            }

            this.createDom();
            if (!this.overlay) {
                return;
            }

            this.isStarted = true;
            this.currentIndex = 0;
            this.toggleOverlay(true);
            this.overlay.setAttribute("aria-hidden", "false");
            this.update();
        }

        end() {
            this.isStarted = false;

            if (this.overlay) {
                this.toggleOverlay(false);
                this.overlay.setAttribute("aria-hidden", "true");
            }
            if (this.resizeObserver) {
                this.resizeObserver.disconnect();
                this.resizeObserver = null;
            }
            this.observedTargets.clear();
        }

        next() {
            if (!this.isStarted) {
                return;
            }
            this.currentIndex += 1;
            this.update();
        }

        previous() {
            if (!this.isStarted) {
                return;
            }
            if (this.currentIndex > 0) {
                this.currentIndex -= 1;
                this.update();
            }
        }

        resolveTarget(step) {
            if (!step || !step.selector) {
                return null;
            }
            const selector = step.selector;
            const directTarget = document.querySelector(selector);
            if (!!directTarget && directTarget.isConnected === true) {
                return directTarget;
            }
            return null;
        }

        isElementInViewport(element) {
            const rect = element.getBoundingClientRect();
            return (
                rect.top >= 0 &&
                rect.left >= 0 &&
                rect.bottom <= window.innerHeight &&
                rect.right <= window.innerWidth
            );
        }

        scrollToElement(element) {
            return new Promise((resolve) => {
                if (this.isElementInViewport(element)) {
                    resolve();
                    return;
                }
                element.scrollIntoView({ behavior: "smooth", block: "center" });
                setTimeout(resolve, 600);
            });
        }

        async update() {
            if (!this.isStarted) {
                return;
            }
            this.createDom();
            if (!this.overlay) {
                return;
            }

            while (this.currentIndex < this.steps.length) {
                const step = this.steps[this.currentIndex];
                const target = this.resolveTarget(step);

                if (!target) {
                    this.currentIndex += 1;
                    continue;
                }

                // Scroll to element if not visible
                await this.scrollToElement(target);

                this.titleEl.textContent =
                    step.title || "Tutorial";

                this.bodyEl.innerHTML =
                    step.text || "";

                this.previousButton.classList.toggle("d-none", this.currentIndex === 0);

                this.nextButton.textContent =
                    step.isLast ? "Fertig" : "Weiter";

                const rect = target.getBoundingClientRect();
                const padding = this.convertRemToPixels(0.5);

                this.highlight.style.left = (rect.left - padding) + "px";
                this.highlight.style.top = (rect.top - padding) + "px";
                this.highlight.style.width = (rect.width + (padding * 2)) + "px";
                this.highlight.style.height = (rect.height + (padding * 2)) + "px";

                this.positionBubble(rect, step.position || "bottom");

                if (!this.resizeObserver) {
                    this.resizeObserver = new ResizeObserver(() => this.update());
                }

                if (!this.observedTargets.has(target)) {
                    this.resizeObserver.observe(target);
                    this.observedTargets.add(target);
                }
                return;
            }
            this.end();
        }

        positionBubble(targetRect, preferredPosition) {

            const bubbleWidth = this.bubble.offsetWidth || 320;
            const bubbleHeight = this.bubble.offsetHeight || 140;

            const spacer = this.convertRemToPixels(1.4);

            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;
            const placements = {
                top: {
                    left: targetRect.left - this.convertRemToPixels(0.5),
                    top: targetRect.top - bubbleHeight - spacer
                },

                bottom: {
                    left: targetRect.left - this.convertRemToPixels(0.5),
                    top: targetRect.bottom + spacer
                },

                left: {
                    left: targetRect.left - bubbleWidth - spacer,
                    top: targetRect.top - this.convertRemToPixels(0.5)
                },

                right: {
                    left: targetRect.right + spacer,
                    top: targetRect.top - this.convertRemToPixels(0.5)
                }
            };

            const order = [
                preferredPosition,
                "bottom",
                "top",
                "right",
                "left"
            ];

            //This is for making sure the whole tooltip is inside the viewport, even if the positioning rules would suggest otherwise
            let chosenPosition = "bottom"; //Fallback position
            let result = null;
            const margin = this.convertRemToPixels(1);
            for (const candidate of order) {
                const candidatePosition = placements[candidate];
                if (!candidatePosition) {
                    continue;
                }

                const boundedLeft = Math.max(margin, candidatePosition.left);
                const boundedTop = Math.max(margin, candidatePosition.top);
                const rightBound = boundedLeft + bubbleWidth;
                const bottomBound = boundedTop + bubbleHeight;

                if (
                    rightBound <= viewportWidth - margin &&
                    bottomBound <= viewportHeight - margin
                ) {
                    result = {left: boundedLeft, top: boundedTop};
                    chosenPosition = candidate;
                    break;
                }
            }

            if (!result) {
                result = {left: margin, top: margin};
            }

            this.bubble.dataset.placement = chosenPosition;
            this.bubble.style.left =
                Math.min(
                    Math.max(result.left, margin),
                    viewportWidth - bubbleWidth - margin
                ) + "px";

            this.bubble.style.top =
                Math.min(
                    Math.max(result.top, margin),
                    viewportHeight - bubbleHeight - margin
                ) + "px";
        }
    }

    const tutorialManager = new TutorialManager();
    window.Tutorial = {
        setSteps(steps) {tutorialManager.setSteps(steps);},
        start() {tutorialManager.start();},
        next() {tutorialManager.next();},
        end() {tutorialManager.end();},
        reset() {
            tutorialManager.setSteps([]);
            tutorialManager.end();
        }
    };

    window.addEventListener("resize", () => {
        if (tutorialManager.isStarted) {
            tutorialManager.update();
        }
    });

    window.addEventListener(
        "scroll",
        () => {
            if (tutorialManager.isStarted) {
                tutorialManager.update();
            }
        },
        true
    );
})();
