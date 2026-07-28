import { icons } from "./icons.js";

const ranges = ["1D", "1W", "1M", "3M", "1Y", "ALL"];

export function PortfolioPerformanceChart(activeRange) {
    return `
        <article class="card chart-card performance-card">
            <div class="card-heading-row">
                <div class="card-title-wrap">
                    <h2>Portfolio Performance</h2>
                    <span class="info-icon">${icons.info}</span>
                </div>
                <div class="range-controls" role="tablist" aria-label="Performance range controls">
                    ${ranges
                        .map(
                            (range) => `
                        <button class="range-btn ${range === activeRange ? "active" : ""}" type="button" data-range="${range}">${range}</button>
                    `
                        )
                        .join("")}
                </div>
            </div>
            <div class="canvas-wrap performance-canvas-wrap">
                <canvas id="performance-chart" aria-label="Portfolio performance line chart"></canvas>
            </div>
        </article>
    `;
}
