import { icons } from "./icons.js";

/*
 * Eren issue: header lacked a clear place for portfolio-level controls in API-driven pages.
 * Fix: add extraControls slot so selector controls and action buttons can coexist consistently.
 * Reviewer: GitHub Copilot (GPT-5.3-Codex).
 */

export function DashboardHeader({
    title = "Dashboard",
    subtitle = "Overview of your portfolio performance and holdings",
    showAddAsset = true,
    extraControls = ""
} = {}) {
    return `
        <section class="dashboard-header">
            <div>
                <h1>${title}</h1>
                <p>${subtitle}</p>
            </div>
            <div class="header-controls">
                ${extraControls}
                <button class="secondary-btn" id="add-portfolio-btn" type="button">
                    <span class="btn-icon">${icons.plus}</span>
                    Add Portfolio
                </button>
                <button class="secondary-btn" id="delete-portfolio-btn" type="button">
                    <span class="btn-icon">${icons.trash}</span>
                    Delete
                </button>
                ${
                    showAddAsset
                        ? `<button class="primary-btn" id="add-asset-btn" type="button">
                <span class="btn-icon">${icons.plus}</span>
                Add Asset
            </button>`
                        : ""
                }
            </div>
        </section>
    `;
}
