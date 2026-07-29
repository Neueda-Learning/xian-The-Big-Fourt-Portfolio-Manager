import { icons } from "./icons.js";

export function AssetAllocationChart(allocation) {
    return `
        <article class="card chart-card allocation-card">
            <div class="card-heading-row">
                <div class="card-title-wrap">
                    <h2>Asset Allocation</h2>
                    <span class="info-icon">${icons.info}</span>
                </div>
            </div>
            <div class="allocation-body">
                <div class="canvas-wrap donut-wrap">
                    <canvas id="allocation-chart" aria-label="Asset allocation donut chart"></canvas>
                </div>
                <ul class="allocation-legend">
                    ${allocation
                        .map(
                            (item) => `
                        <li>
                            <span class="legend-label"><span class="dot" style="background:${item.color}"></span>${item.label}</span>
                            <strong>${item.value.toFixed(1)}%</strong>
                        </li>
                    `
                        )
                        .join("")}
                </ul>
            </div>
        </article>
    `;
}
