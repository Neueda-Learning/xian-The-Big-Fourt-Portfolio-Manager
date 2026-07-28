import { icons } from "./icons.js";

export function DashboardHeader({ title = "Dashboard", subtitle = "Overview of your portfolio performance and holdings", showAddAsset = true } = {}) {
    return `
        <section class="dashboard-header">
            <div>
                <h1>${title}</h1>
                <p>${subtitle}</p>
            </div>
            ${
                showAddAsset
                    ? `<button class="primary-btn" id="add-asset-btn" type="button">
                <span class="btn-icon">${icons.plus}</span>
                Add Asset
            </button>`
                    : ""
            }
        </section>
    `;
}
