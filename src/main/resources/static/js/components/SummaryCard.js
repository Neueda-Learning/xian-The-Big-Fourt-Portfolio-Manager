export function SummaryCard({ id, label, value, detail, tone = "neutral", icon, sparkline = "" }) {
    return `
        <article class="summary-card">
            <div class="summary-top-row">
                <p class="summary-card-label">${label}</p>
                <span class="summary-icon ${tone}">${icon}</span>
            </div>
            <p class="summary-card-value">${value}</p>
            <p class="summary-card-detail ${tone}">${detail}</p>
            ${sparkline ? `<div id="${id}" class="sparkline-wrap">${sparkline}</div>` : ""}
        </article>
    `;
}
