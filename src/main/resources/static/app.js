(function () {
    const toastEl = document.getElementById("toast");
    let overviewState = null;

    function showToast(message) {
        toastEl.textContent = message;
        toastEl.classList.add("show");
        setTimeout(() => toastEl.classList.remove("show"), 2200);
    }

    async function api(url, options) {
        const response = await fetch(url, options);
        const contentType = response.headers.get("content-type") || "";
        let body;
        if (contentType.includes("application/json")) {
            body = await response.json();
        } else {
            body = await response.text();
        }
        if (!response.ok) {
            throw new Error(typeof body === "string" ? body : JSON.stringify(body));
        }
        return body;
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/\"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function isIsoDateTime(value) {
        return typeof value === "string" && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(value);
    }

    function isIsoDate(value) {
        return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value);
    }

    function formatNumber(value) {
        if (value === null || value === undefined || value === "") {
            return "";
        }
        const numeric = Number(value);
        if (!Number.isFinite(numeric)) {
            return String(value);
        }
        return numeric.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 });
    }

    function formatMoney(value) {
        if (value === null || value === undefined || value === "") {
            return "$0.00";
        }
        const numeric = Number(value);
        if (!Number.isFinite(numeric)) {
            return String(value);
        }
        return numeric.toLocaleString(undefined, { style: "currency", currency: "USD", minimumFractionDigits: 2, maximumFractionDigits: 4 });
    }

    function formatDisplayValue(value) {
        if (value === null || value === undefined) {
            return "";
        }
        if (typeof value === "number") {
            return formatNumber(value);
        }
        if (typeof value === "boolean") {
            return value ? "Yes" : "No";
        }
        if (isIsoDateTime(value)) {
            return value.replace("T", " ");
        }
        if (Array.isArray(value) || typeof value === "object") {
            return escapeHtml(JSON.stringify(value));
        }
        return escapeHtml(value);
    }

    function renderTable(containerId, items, preferredKeys) {
        const container = document.getElementById(containerId);
        if (!Array.isArray(items) || items.length === 0) {
            container.innerHTML = "<p>No data found.</p>";
            return;
        }

        const discoveredKeys = Array.from(items.reduce((set, item) => {
            Object.keys(item || {}).forEach((key) => set.add(key));
            return set;
        }, new Set()));

        const orderedKeys = Array.isArray(preferredKeys) && preferredKeys.length > 0
            ? [...preferredKeys.filter((key) => discoveredKeys.includes(key)), ...discoveredKeys.filter((key) => !preferredKeys.includes(key))]
            : discoveredKeys;

        const header = orderedKeys.map((key) => `<th>${escapeHtml(key)}</th>`).join("");
        const rows = items.map((item) => {
            const cells = orderedKeys.map((key) => `<td>${formatDisplayValue(item?.[key])}</td>`).join("");
            return `<tr>${cells}</tr>`;
        }).join("");

        container.innerHTML = `<table><thead><tr>${header}</tr></thead><tbody>${rows}</tbody></table>`;
    }

    function bindTabs() {
        const tabs = document.querySelectorAll(".tab");
        const panels = document.querySelectorAll(".panel");
        tabs.forEach((tab) => {
            tab.addEventListener("click", () => {
                tabs.forEach((t) => t.classList.remove("active"));
                panels.forEach((p) => p.classList.remove("active"));
                tab.classList.add("active");
                document.getElementById(`panel-${tab.dataset.tab}`).classList.add("active");
            });
        });
    }

    function renderSummaryCards(summary) {
        const container = document.getElementById("summary-cards");
        if (!summary) {
            container.innerHTML = "<p>No summary available.</p>";
            return;
        }

        const cards = [
            { label: "Total Assets", value: formatMoney(summary.totalAssets), tone: "primary" },
            { label: "Cash", value: formatMoney(summary.cashAssets), tone: "neutral" },
            { label: "Invested in Stocks/Bonds", value: formatMoney(summary.investedAmount), tone: "neutral" },
            { label: "Stocks Profit/Loss", value: formatMoney(summary.stockProfitLoss), tone: Number(summary.stockProfitLoss) >= 0 ? "gain" : "loss" },
            { label: "Bonds Profit/Loss", value: formatMoney(summary.bondProfitLoss), tone: Number(summary.bondProfitLoss) >= 0 ? "gain" : "loss" },
            { label: "Stock + Bond Profit/Loss", value: formatMoney(summary.stockBondProfitLoss), tone: Number(summary.stockBondProfitLoss) >= 0 ? "gain" : "loss" },
            { label: "Stock Buy Cost", value: formatMoney(summary.stockSpent), tone: "neutral" },
            { label: "Bond Buy Cost", value: formatMoney(summary.bondSpent), tone: "neutral" },
            { label: "Portfolios / Holdings / Transactions", value: `${summary.portfolioCount} / ${summary.holdingCount} / ${summary.transactionCount}`, tone: "neutral" }
        ];

        container.innerHTML = cards.map((card) => `
            <article class="summary-card ${card.tone}">
                <span class="summary-label">${escapeHtml(card.label)}</span>
                <strong class="summary-value">${escapeHtml(card.value)}</strong>
            </article>
        `).join("");
    }

    function renderBreakdown(summary) {
        const container = document.getElementById("summary-breakdown");
        if (!summary) {
            container.innerHTML = "<p>No data found.</p>";
            return;
        }

        container.innerHTML = `
            <div class="formula-box">
                <div><strong>Total Assets</strong> = Cash + Stock/Bond Profit or Loss + Money Spent on Buying Stocks/Bonds</div>
                <div class="formula-line">${formatMoney(summary.totalAssets)} = ${formatMoney(summary.cashAssets)} + ${formatMoney(summary.stockBondProfitLoss)} + ${formatMoney(summary.investedAmount)}</div>
            </div>
            <table>
                <tbody>
                    <tr><th>Cash</th><td>${escapeHtml(formatMoney(summary.cashAssets))}</td></tr>
                    <tr><th>Stock Buy Cost</th><td>${escapeHtml(formatMoney(summary.stockSpent))}</td></tr>
                    <tr><th>Bond Buy Cost</th><td>${escapeHtml(formatMoney(summary.bondSpent))}</td></tr>
                    <tr><th>Stock Profit/Loss</th><td>${escapeHtml(formatMoney(summary.stockProfitLoss))}</td></tr>
                    <tr><th>Bond Profit/Loss</th><td>${escapeHtml(formatMoney(summary.bondProfitLoss))}</td></tr>
                </tbody>
            </table>
        `;
    }

    function renderYahooStatus(status) {
        const container = document.getElementById("yahoo-status");
        if (!status) {
            container.innerHTML = "<p>No Yahoo sync data.</p>";
            return;
        }

        const items = Array.isArray(status.items) ? status.items : [];
        const preview = items.slice(0, 8).map((item) => `
            <tr>
                <td>${escapeHtml(item.ticker)}</td>
                <td>${item.saved ? "Saved" : "Skipped"}</td>
                <td>${item.closePrice === null || item.closePrice === undefined ? "" : escapeHtml(formatNumber(item.closePrice))}</td>
            </tr>
        `).join("");

        container.innerHTML = `
            <div class="status-list">
                <div><strong>Completed:</strong> ${status.completed ? "Yes" : "No"}</div>
                <div><strong>Bootstrap Time:</strong> ${escapeHtml(status.bootstrapAt ? String(status.bootstrapAt).replace("T", " ") : "")}</div>
                <div><strong>Tickers:</strong> ${escapeHtml(status.tickerCount ?? 0)}</div>
                <div><strong>Saved Rows:</strong> ${escapeHtml(status.savedCount ?? 0)}</div>
            </div>
            <div class="table-wrap compact-table">
                <table>
                    <thead><tr><th>Ticker</th><th>Status</th><th>Close Price</th></tr></thead>
                    <tbody>${preview || '<tr><td colspan="3">No startup items.</td></tr>'}</tbody>
                </table>
            </div>
        `;
    }

    function renderTrendChart(points) {
        const legend = document.getElementById("trend-legend");
        const container = document.getElementById("trend-chart");

        if (!Array.isArray(points) || points.length === 0) {
            legend.innerHTML = "";
            container.innerHTML = "<p>No trend data found.</p>";
            return;
        }

        const series = [
            { key: "cash", label: "Cash", color: "#2563eb" },
            { key: "stock", label: "Stocks", color: "#16a34a" },
            { key: "bond", label: "Bonds", color: "#d97706" },
            { key: "totalAssets", label: "Total Assets", color: "#7c3aed" }
        ];

        legend.innerHTML = series.map((item) => `
            <span class="legend-item"><span class="legend-dot" style="background:${item.color}"></span>${escapeHtml(item.label)}</span>
        `).join("");

        const width = 900;
        const height = 320;
        const padding = 42;
        const maxValue = Math.max(1, ...points.flatMap((point) => series.map((item) => Number(point[item.key] || 0))));
        const xStep = points.length === 1 ? 0 : (width - padding * 2) / (points.length - 1);

        const axisLabels = points.map((point, index) => {
            if (points.length > 8 && index % Math.ceil(points.length / 8) !== 0 && index !== points.length - 1) {
                return "";
            }
            const x = padding + index * xStep;
            return `<text x="${x}" y="${height - 8}" text-anchor="middle" font-size="10" fill="#475569">${escapeHtml(String(point.date).slice(5))}</text>`;
        }).join("");

        const yGuides = [0, 0.25, 0.5, 0.75, 1].map((ratio) => {
            const y = height - padding - ratio * (height - padding * 2);
            const value = maxValue * ratio;
            return `
                <line x1="${padding}" y1="${y}" x2="${width - padding}" y2="${y}" stroke="#e2e8f0" stroke-width="1" />
                <text x="${padding - 8}" y="${y + 4}" text-anchor="end" font-size="10" fill="#64748b">${escapeHtml(formatNumber(value))}</text>
            `;
        }).join("");

        const lines = series.map((item) => {
            const polyline = points.map((point, index) => {
                const x = padding + index * xStep;
                const y = height - padding - ((Number(point[item.key] || 0) / maxValue) * (height - padding * 2));
                return `${x},${y}`;
            }).join(" ");
            return `<polyline fill="none" stroke="${item.color}" stroke-width="3" points="${polyline}" />`;
        }).join("");

        container.innerHTML = `
            <svg viewBox="0 0 ${width} ${height}" class="chart-svg" role="img" aria-label="Cash stock bond and total assets trend chart">
                ${yGuides}
                <line x1="${padding}" y1="${height - padding}" x2="${width - padding}" y2="${height - padding}" stroke="#94a3b8" stroke-width="1.5" />
                <line x1="${padding}" y1="${padding}" x2="${padding}" y2="${height - padding}" stroke="#94a3b8" stroke-width="1.5" />
                ${lines}
                ${axisLabels}
            </svg>
        `;
    }

    function renderOverview(data) {
        overviewState = data;
        renderSummaryCards(data.summary);
        renderBreakdown(data.summary);
        renderTrendChart(data.assetTrend);
        renderTable("trend-table", data.assetTrend, ["date", "cash", "stock", "bond", "totalAssets"]);
        renderTable("holding-list", data.holdings, ["id", "portfolioId", "assetType", "ticker", "quantity", "purchasePrice", "purchasedata", "currency"]);
        renderTable("transaction-list", data.transactions, ["id", "holdingId", "type", "quantity", "price", "tradeDate"]);
    }

    async function loadOverview() {
        try {
            const data = await api("/dashboard/overview");
            renderOverview(data);
        } catch (e) {
            showToast(e.message);
        }
    }

    async function loadYahooSection(syncFirst) {
        try {
            let status;
            if (syncFirst) {
                status = await api("/yahoo/sync/all", { method: "POST" });
                showToast(`Yahoo sync saved ${status.savedCount ?? 0}/${status.tickerCount ?? 0}`);
            } else {
                status = await api("/yahoo/bootstrap/status");
            }

            renderYahooStatus(status);
            const latestRows = await api("/prices/latest");
            renderTable("latest-prices-table", latestRows, ["ticker", "priceDate", "pricetime", "openprice", "highprice", "lowprice", "closeprice", "adjustedclose", "volume", "currency"]);
        } catch (e) {
            showToast(e.message);
        }
    }

    function buildHoldingPayload(prefix) {
        const assetType = document.getElementById(`${prefix}asset-type`).value;
        const rawTicker = document.getElementById(`${prefix}ticker`).value.trim().toUpperCase();
        return {
            portfolioId: Number(document.getElementById(`${prefix}portfolio-id`).value),
            assetType,
            ticker: assetType === "CASH" ? null : (rawTicker || null),
            quantity: Number(document.getElementById(`${prefix}quantity`).value),
            purchasePrice: Number(document.getElementById(`${prefix}purchase-price`).value),
            purchasedata: document.getElementById(`${prefix}purchase-date`).value,
            currency: document.getElementById(`${prefix}currency`).value.trim().toUpperCase()
        };
    }

    function bindHolding() {
        document.getElementById("holding-create-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = buildHoldingPayload("holding-");
            const msg = await api("/saveholding", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
            await loadOverview();
        });

        document.getElementById("holding-find-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("holding-find-id").value;
            const data = await api(`/holding/${id}`);
            document.getElementById("holding-find-result").textContent = JSON.stringify(data, null, 2);
        });

        document.getElementById("holding-by-portfolio-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const portfolioId = document.getElementById("holding-list-portfolio-id").value;
            const data = await api(`/holdings/portfolio/${portfolioId}`);
            renderTable("holding-list", data, ["id", "portfolioId", "assetType", "ticker", "quantity", "purchasePrice", "purchasedata", "currency"]);
        });

        document.getElementById("holding-update-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("holding-update-id").value;
            const payload = buildHoldingPayload("holding-update-");
            const msg = await api(`/holding/${id}`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
            await loadOverview();
        });

        document.getElementById("holding-delete-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("holding-delete-id").value;
            const msg = await api(`/delete/holding/${id}`, { method: "DELETE" });
            showToast(msg);
            await loadOverview();
        });
    }

    function bindRefreshButtons() {
        document.getElementById("dashboard-refresh").addEventListener("click", loadOverview);
        document.getElementById("trend-refresh").addEventListener("click", loadOverview);
        document.getElementById("holding-refresh").addEventListener("click", loadOverview);
        document.getElementById("transaction-refresh").addEventListener("click", loadOverview);
        document.getElementById("yahoo-refresh").addEventListener("click", async () => {
            await loadYahooSection(true);
            await loadOverview();
        });
    }

    function bindGlobalErrorHandler() {
        window.addEventListener("unhandledrejection", (event) => {
            showToast(event.reason?.message || "Request failed");
        });
    }

    bindTabs();
    bindHolding();
    bindRefreshButtons();
    bindGlobalErrorHandler();
    loadOverview();
    loadYahooSection(false);
})();
