import { icons } from "./icons.js";

const navItems = [
    { key: "Dashboard", icon: icons.home },
    { key: "Holdings", icon: icons.holdings },
    { key: "Transactions", icon: icons.transactions },
    { key: "AI Assistant", icon: icons.info },
    { key: "Reports", icon: icons.reports },
    { key: "Settings", icon: icons.settings }
];

export function Sidebar(metrics) {
    const activeNav = metrics.activeNav || "Dashboard";

    return `
        <aside class="sidebar" id="sidebar">
            <nav class="sidebar-nav" aria-label="Primary">
                ${navItems
                    .map(
                        (item) => `
                    <button class="nav-item ${item.key === activeNav ? "active" : ""}" type="button" aria-label="${item.key}" data-nav="${item.key}">
                        <span class="nav-icon">${item.icon}</span>
                        <span>${item.key}</span>
                    </button>
                `
                    )
                    .join("")}
            </nav>

            <div class="portfolio-summary-card">
                <p class="summary-label">Total Portfolio Value</p>
                <p class="summary-value">${metrics.totalValue}</p>
            </div>

            <p class="sidebar-footer">&copy; 2024 Portfolio Manager</p>
        </aside>
    `;
}
