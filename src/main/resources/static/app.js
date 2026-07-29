import { AppLayout } from "./js/components/AppLayout.js";
import { TopNavbar } from "./js/components/TopNavbar.js";
import { Sidebar } from "./js/components/Sidebar.js";
import { DashboardHeader } from "./js/components/DashboardHeader.js";
import { SummaryCard } from "./js/components/SummaryCard.js";
import { PortfolioPerformanceChart } from "./js/components/PortfolioPerformanceChart.js";
import { AssetAllocationChart } from "./js/components/AssetAllocationChart.js";
import { HoldingsTable } from "./js/components/HoldingsTable.js";
import { RecentTransactions } from "./js/components/RecentTransactions.js";
import { AddAssetModal } from "./js/components/AddAssetModal.js";
import { ConfirmDeleteModal } from "./js/components/ConfirmDeleteModal.js";
import { CashActionModal } from "./js/components/CashActionModal.js";
import { icons } from "./js/components/icons.js";
import { generatePerformanceData } from "./js/data/mockData.js";
import { compactDate, formatCurrency, formatPercent, formatSignedCurrency, toInputSafeText } from "./js/utils/formatters.js";

const appRoot = document.getElementById("app");
const savedTheme = window.localStorage.getItem("pm-theme");

const state = {
    portfolios: [],
    selectedPortfolioId: null,
    holdingsRaw: [],
    holdings: [],
    transactions: [],
    priceHistory: [],
    performance: null,
    performanceData: [],
    allocationData: [],
    loading: true,
    globalError: "",
    searchTerm: "",
    selectedType: "All Types",
    selectedRange: "1Y",
    addModalOpen: false,
    editingAssetId: null,
    deleteTargetId: null,
    cashActionMode: null,
    cashActionHoldingId: null,
    sidebarOpen: false,
    activeNav: "Dashboard",
    darkMode: savedTheme === "dark",
    editingTransactionId: null,
    priceMessage: "",
    transactionMessage: ""
};

let performanceChart;
let allocationChart;
let lastFocusedElement;
let escapeHandlerBound = false;

/*
 * Eren issue: frontend initially used local mock state and chart slicing that masked real backend updates.
 * Fix: migrate to API-driven state loading/refresh and make time-range chart data react to real dates and post-close trades.
 * Reviewer: GitHub Copilot (GPT-5.3-Codex).
 */

async function apiRequest(path, options = {}) {
    const response = await fetch(path, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    const text = await response.text();
    const isJson = (response.headers.get("content-type") || "").includes("application/json");
    const payload = isJson && text ? JSON.parse(text) : text;

    if (!response.ok) {
        const message = typeof payload === "string" ? payload : payload?.message || "Request failed";
        throw new Error(message);
    }

    return payload;
}

function normalizeHolding(rawHolding, detailMap) {
    const detail = detailMap.get(rawHolding.id);
    const typeLabel = String(rawHolding.assetType || "Stock").toLowerCase();
    const type = typeLabel.charAt(0).toUpperCase() + typeLabel.slice(1);
    const ticker = rawHolding.ticker || rawHolding.assetType || `ASSET-${rawHolding.id}`;

    return {
        id: rawHolding.id,
        portfolioId: rawHolding.portfolioId,
        ticker,
        name: ticker,
        type,
        quantity: Number(rawHolding.quantity || 0),
        avgPrice: Number(rawHolding.purchasePrice || 0),
        currentPrice: Number(detail?.currentPrice || rawHolding.purchasePrice || 0),
        currency: rawHolding.currency || "USD",
        purchasedata: rawHolding.purchasedata
    };
}

function normalizeTransaction(rawTx, holdingMap) {
    const holdingTicker = holdingMap.get(rawTx.holdingId)?.ticker || `Holding-${rawTx.holdingId}`;
    const txType = String(rawTx.type || "BUY").toUpperCase();
    const quantity = Number(rawTx.quantity || 0);
    const price = Number(rawTx.price || 0);
    const amount = txType === "SELL" ? quantity * price : -(quantity * price);

    return {
        id: rawTx.id,
        holdingId: rawTx.holdingId,
        date: new Intl.DateTimeFormat("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric"
        }).format(new Date(rawTx.tradeDate)),
        dateRaw: rawTx.tradeDate,
        type: holdingMap.get(rawTx.holdingId)?.type || "Stock",
        asset: holdingTicker,
        action: txType === "SELL" ? "Sell" : "Buy",
        txType,
        quantity,
        price,
        amount
    };
}

function buildFallbackPerformanceSeries(totalValue) {
    const generated = generatePerformanceData(120);
    const baseline = generated[generated.length - 1]?.value || 1;
    const target = totalValue > 0 ? totalValue : 100000;
    return generated.map((item) => ({
        date: item.date,
        value: Number(((item.value / baseline) * target).toFixed(2))
    }));
}

function rebuildAllocationData() {
    const total = state.holdings.reduce((sum, item) => sum + item.quantity * item.currentPrice, 0);
    if (total <= 0) {
        state.allocationData = [];
        return;
    }

    const colorMap = {
        Stock: "#3b82f6",
        Bond: "#22c55e",
        Cash: "#f59e0b"
    };

    const grouped = new Map();
    state.holdings.forEach((item) => {
        const value = item.quantity * item.currentPrice;
        grouped.set(item.type, (grouped.get(item.type) || 0) + value);
    });

    state.allocationData = Array.from(grouped.entries()).map(([label, value]) => ({
        label: `${label}s`,
        value: Number(((value / total) * 100).toFixed(1)),
        color: colorMap[label] || "#8b5cf6"
    }));
}

function buildPerformanceSeries() {
    if (state.priceHistory.length > 1) {
        const totalValue = Number(state.performance?.totalMarketValue || state.holdings.reduce((sum, item) => sum + item.quantity * item.currentPrice, 0));
        const ordered = [...state.priceHistory].sort((a, b) => new Date(a.priceDate).getTime() - new Date(b.priceDate).getTime());
        const lastPriceDate = new Date(`${ordered[ordered.length - 1].priceDate}T23:59:59`);
        const holdingPriceMap = new Map(state.holdings.map((item) => [item.id, Number(item.currentPrice || item.avgPrice || 0)]));
        const postCloseImpact = state.transactions
            .filter((tx) => new Date(tx.dateRaw).getTime() > lastPriceDate.getTime())
            .reduce((sum, tx) => {
                const qty = Number(tx.quantity || 0);
                const tradePrice = Number(tx.price || 0);
                const currentPrice = Number(holdingPriceMap.get(tx.holdingId) || tradePrice);
                if (String(tx.txType).toUpperCase() === "SELL") {
                    return sum + qty * (tradePrice - currentPrice);
                }
                return sum + qty * (currentPrice - tradePrice);
            }, 0);
        const referenceTotal = totalValue - postCloseImpact;
        const lastClose = Number(ordered[ordered.length - 1]?.closeprice || 0);

        state.performanceData = ordered.map((item) => {
            const close = Number(item.closeprice || 0);
            const scaled = lastClose > 0 ? (close / lastClose) * referenceTotal : referenceTotal;
            return {
                date: new Date(`${item.priceDate}T16:00:00`),
                value: Number(scaled.toFixed(2))
            };
        });

        const now = new Date();
        const latestPoint = state.performanceData[state.performanceData.length - 1];
        if (!latestPoint || latestPoint.date.toDateString() !== now.toDateString()) {
            state.performanceData.push({
                date: now,
                value: Number(totalValue.toFixed(2))
            });
        } else {
            latestPoint.value = Number(totalValue.toFixed(2));
        }

        return;
    }

    const totalValue = state.holdings.reduce((sum, item) => sum + item.quantity * item.currentPrice, 0);
    state.performanceData = buildFallbackPerformanceSeries(totalValue);
}

async function refreshPortfolioData() {
    if (!state.selectedPortfolioId) {
        state.holdingsRaw = [];
        state.holdings = [];
        state.transactions = [];
        state.priceHistory = [];
        state.performance = null;
        state.performanceData = buildFallbackPerformanceSeries(0);
        state.allocationData = [];
        return;
    }

    const holdingsRaw = await apiRequest(`/holdings/portfolio/${state.selectedPortfolioId}`);
    state.holdingsRaw = Array.isArray(holdingsRaw) ? holdingsRaw : [];

    state.performance = await apiRequest(`/portfolio/${state.selectedPortfolioId}/performance`);
    const detailMap = new Map((state.performance?.holdingsDetail || []).map((item) => [item.holdingId, item]));
    state.holdings = state.holdingsRaw.map((item) => normalizeHolding(item, detailMap));

    const holdingMap = new Map(state.holdings.map((item) => [item.id, item]));
    const txResult = await Promise.all(
        state.holdings.map(async (holding) => {
            const rows = await apiRequest(`/transactions/holding/${holding.id}`);
            return Array.isArray(rows) ? rows : [];
        })
    );
    const flattened = txResult.flat();
    state.transactions = flattened
        .map((item) => normalizeTransaction(item, holdingMap))
        .sort((a, b) => new Date(b.dateRaw).getTime() - new Date(a.dateRaw).getTime());

    const primaryTicker = state.holdings.find((item) => item.ticker && item.type !== "Cash")?.ticker;
    if (primaryTicker) {
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(endDate.getDate() - 30);
        const format = (date) => date.toISOString().slice(0, 10);
        const query = `startDate=${format(startDate)}&endDate=${format(endDate)}`;
        const priceRows = await apiRequest(`/prices/${encodeURIComponent(primaryTicker)}?${query}`);
        state.priceHistory = Array.isArray(priceRows) ? priceRows : [];
    } else {
        state.priceHistory = [];
    }

    rebuildAllocationData();
    buildPerformanceSeries();
}

async function loadInitialData() {
    state.loading = true;
    state.globalError = "";
    render();

    try {
        const portfolios = await apiRequest("/portfolios");
        state.portfolios = Array.isArray(portfolios) ? portfolios : [];
        state.selectedPortfolioId = state.portfolios[0]?.id || null;
        await refreshPortfolioData();
    } catch (error) {
        state.globalError = error.message;
    } finally {
        state.loading = false;
        render();
    }
}

function computeMetrics() {
    const totalValue = Number(state.performance?.totalMarketValue || state.holdings.reduce((sum, item) => sum + item.quantity * item.currentPrice, 0));
    const totalCost = state.holdings.reduce((sum, item) => sum + item.quantity * item.avgPrice, 0);
    const totalGain = totalValue - totalCost;
    const totalGainPct = totalCost > 0 ? (totalGain / totalCost) * 100 : 0;
    const cashBalance = getCurrentCashBalance();

    const selectedData = getPerformanceDataByRange(state.selectedRange);
    const lastValue = selectedData[selectedData.length - 1]?.value || 0;
    const prevValue = selectedData[selectedData.length - 2]?.value || lastValue;
    const dayChangeAmount = lastValue - prevValue;
    const dayChangePct = prevValue > 0 ? (dayChangeAmount / prevValue) * 100 : 0;

    return {
        totalValue,
        totalGain,
        totalGainPct,
        dayChangeAmount,
        dayChangePct,
        cashBalance
    };
}

function getPerformanceDataByRange(range) {
    const data = state.performanceData;
    if (!data.length) {
        return [];
    }

    const ordered = [...data].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
    if (range === "ALL") {
        return ordered;
    }

    const daysMap = {
        "1D": 1,
        "1W": 7,
        "1M": 30,
        "3M": 90,
        "1Y": 365
    };

    const days = daysMap[range] || 365;
    const endTime = new Date(ordered[ordered.length - 1].date).getTime();
    const startTime = endTime - days * 24 * 60 * 60 * 1000;
    const filtered = ordered.filter((item) => new Date(item.date).getTime() >= startTime);

    if (filtered.length >= 2) {
        return filtered;
    }
    return ordered.slice(Math.max(0, ordered.length - 2));
}

function buildSparklineSvg() {
    const points = getPerformanceDataByRange("1M").slice(-24).map((item) => item.value);
    if (points.length < 2) {
        return "";
    }

    const min = Math.min(...points);
    const max = Math.max(...points);
    const width = 120;
    const height = 40;
    const mapY = (value) => {
        if (max === min) {
            return height / 2;
        }
        return height - ((value - min) / (max - min)) * height;
    };

    const path = points
        .map((value, index) => {
            const x = (index / (points.length - 1)) * width;
            const y = mapY(value);
            return `${index === 0 ? "M" : "L"}${x.toFixed(2)} ${y.toFixed(2)}`;
        })
        .join(" ");

    return `
        <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true">
            <path d="${path}" fill="none" stroke="#3b82f6" stroke-width="2.4" stroke-linecap="round"></path>
        </svg>
    `;
}

function getEditingAsset() {
    if (!state.editingAssetId) {
        return null;
    }
    return state.holdings.find((holding) => Number(holding.id) === Number(state.editingAssetId)) || null;
}

function getDeleteTargetAsset() {
    if (!state.deleteTargetId) {
        return null;
    }
    return state.holdings.find((holding) => Number(holding.id) === Number(state.deleteTargetId)) || null;
}

function getCashHolding() {
    if (!state.cashActionHoldingId) {
        return null;
    }
    return state.holdingsRaw.find((holding) => Number(holding.id) === Number(state.cashActionHoldingId)) || null;
}

function getPrimaryCashHolding() {
    return state.holdings.find((holding) => holding.type === "Cash") || null;
}

function getCurrentCashBalance() {
    return state.holdings
        .filter((holding) => holding.type === "Cash")
        .reduce((sum, holding) => sum + holding.quantity * holding.currentPrice, 0);
}

function getViewConfig(metrics) {
    const defaults = {
        title: "Dashboard",
        subtitle: "Overview of your portfolio performance and holdings",
        showAddAsset: true,
        showSummary: true,
        showPerformance: true,
        showHoldings: true,
        showTransactions: true,
        holdingsMarkup: null,
        transactionsMarkup: null
    };

    if (state.activeNav === "Holdings") {
        return {
            ...defaults,
            title: "Holdings",
            subtitle: "View, edit, and manage your current positions",
            showSummary: false,
            showPerformance: false,
            showTransactions: false
        };
    }

    if (state.activeNav === "Transactions") {
        return {
            ...defaults,
            title: "Transactions",
            subtitle: "Track your recent buy and sell activity",
            showAddAsset: false,
            showSummary: false,
            showPerformance: false,
            showHoldings: false,
            transactionsMarkup: renderTransactionManager()
        };
    }

    if (state.activeNav === "Performance") {
        return {
            ...defaults,
            title: "Performance",
            subtitle: "Analyze portfolio growth and allocation trends",
            showAddAsset: false,
            showHoldings: false,
            showTransactions: false,
            transactionsMarkup: renderPriceHistoryManager()
        };
    }

    if (state.activeNav === "Reports") {
        return {
            ...defaults,
            title: "Reports",
            subtitle: "Quick analytics generated from your current dataset",
            showAddAsset: false,
            showSummary: false,
            showPerformance: false,
            showTransactions: false,
            holdingsMarkup: renderReportsPanel(metrics)
        };
    }

    if (state.activeNav === "Settings") {
        return {
            ...defaults,
            title: "Settings",
            subtitle: "Adjust interface preferences",
            showAddAsset: false,
            showSummary: false,
            showPerformance: false,
            showTransactions: false,
            holdingsMarkup: renderSettingsPanel()
        };
    }

    return defaults;
}

function renderReportsPanel(metrics) {
    const totalAssets = state.holdings.length;
    const stockCount = state.holdings.filter((item) => item.type === "Stock").length;
    const bondCount = state.holdings.filter((item) => item.type === "Bond").length;
    const cashCount = state.holdings.filter((item) => item.type === "Cash").length;

    return `
        <article class="card info-panel">
            <h2>Portfolio Snapshot</h2>
            <p>Generated from current in-memory portfolio data.</p>
            <div class="stats-grid">
                <div><strong>${formatCurrency(metrics.totalValue)}</strong><span>Total Market Value</span></div>
                <div><strong>${formatCurrency(metrics.totalGain)}</strong><span>Total Gain / Loss</span></div>
                <div><strong>${totalAssets}</strong><span>Total Assets</span></div>
                <div><strong>${state.transactions.length}</strong><span>Total Transactions</span></div>
                <div><strong>${stockCount}</strong><span>Stock Positions</span></div>
                <div><strong>${bondCount}</strong><span>Bond Positions</span></div>
                <div><strong>${cashCount}</strong><span>Cash Positions</span></div>
            </div>
        </article>
    `;
}

function renderSettingsPanel() {
    return `
        <article class="card info-panel">
            <h2>Display Preferences</h2>
            <p>Switch between light and dark theme for the dashboard.</p>
            <div class="settings-row">
                <span>Theme Mode</span>
                <button class="secondary-btn" id="settings-theme-toggle" type="button">${state.darkMode ? "Switch to Light" : "Switch to Dark"}</button>
            </div>
        </article>
    `;
}

function renderTransactionManager() {
    const holdingOptions = state.holdings
        .map((holding) => `<option value="${holding.id}">${holding.ticker} (Holding #${holding.id})</option>`)
        .join("");

    return `
        <article class="card info-panel">
            <h2>Transaction Manager</h2>
            <p>Create, edit, and delete transaction records for the selected portfolio.</p>
            <form id="transaction-form" class="manager-grid" novalidate>
                <label>Holding</label>
                <select name="holdingId" required>${holdingOptions}</select>

                <label>Type</label>
                <select name="type" required>
                    <option value="BUY">BUY</option>
                    <option value="SELL">SELL</option>
                </select>

                <label>Quantity</label>
                <input name="quantity" type="number" min="0.0001" step="0.0001" required>

                <label>Price</label>
                <input name="price" type="number" min="0.0001" step="0.0001" required>

                <label>Trade Time</label>
                <input name="tradeDate" type="datetime-local" required>

                <div class="manager-actions">
                    <button type="submit" class="primary-btn">Add Transaction</button>
                </div>
            </form>
            <p class="form-error" id="transaction-message">${toInputSafeText(state.transactionMessage)}</p>
            <div class="table-scroller">
                <table>
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Asset</th>
                        <th>Type</th>
                        <th>Qty</th>
                        <th>Price</th>
                        <th>Date</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                        ${state.transactions
                            .slice(0, 20)
                            .map(
                                (item) => `
                            <tr>
                                <td>${item.id}</td>
                                <td>${toInputSafeText(item.asset)}</td>
                                <td>${item.txType}</td>
                                <td>${item.quantity}</td>
                                <td>${formatCurrency(item.price)}</td>
                                <td>${toInputSafeText(item.date)}</td>
                                <td>
                                    <button class="icon-btn small" type="button" data-tx-delete="${item.id}">${icons.trash}</button>
                                </td>
                            </tr>
                        `
                            )
                            .join("")}
                    </tbody>
                </table>
            </div>
        </article>
    `;
}

function renderPriceHistoryManager() {
    return `
        <article class="card info-panel">
            <h2>Price History Manager</h2>
            <p>Manage end-of-day prices used by performance charts.</p>
            <form id="price-form" class="manager-grid" novalidate>
                <label>Ticker</label>
                <input name="ticker" type="text" maxlength="20" required>

                <label>Price Date</label>
                <input name="priceDate" type="date" required>

                <label>Close Price</label>
                <input name="closeprice" type="number" min="0.0001" step="0.0001" required>

                <div class="manager-actions">
                    <button type="submit" class="primary-btn">Add Price</button>
                </div>
            </form>
            <p class="form-error" id="price-message">${toInputSafeText(state.priceMessage)}</p>
            <div class="table-scroller">
                <table>
                    <thead>
                    <tr>
                        <th>Ticker</th>
                        <th>Date</th>
                        <th>Close</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                        ${state.priceHistory
                            .slice(-30)
                            .reverse()
                            .map(
                                (item) => `
                            <tr>
                                <td>${toInputSafeText(item.ticker)}</td>
                                <td>${toInputSafeText(item.priceDate)}</td>
                                <td>${formatCurrency(Number(item.closeprice || 0))}</td>
                                <td>
                                    <button class="icon-btn small" type="button" data-price-delete="${item.ticker}|${item.priceDate}">${icons.trash}</button>
                                </td>
                            </tr>
                        `
                            )
                            .join("")}
                    </tbody>
                </table>
            </div>
        </article>
    `;
}

function applyTheme() {
    document.body.classList.toggle("dark-theme", state.darkMode);
    window.localStorage.setItem("pm-theme", state.darkMode ? "dark" : "light");
}

function toggleTheme() {
    state.darkMode = !state.darkMode;
    render();
}

function render() {
    if (state.loading) {
        appRoot.innerHTML = `<div class="loading-wrap">Loading portfolio data...</div>`;
        applyTheme();
        return;
    }

    const metrics = computeMetrics();
    const view = getViewConfig(metrics);

    const summaryCards = [
        SummaryCard({
            id: "sparkline-total-value",
            label: "Total Value",
            value: formatCurrency(metrics.totalValue),
            detail: `${formatSignedCurrency(metrics.dayChangeAmount)} (${formatPercent(metrics.dayChangePct)})`,
            tone: "positive",
            icon: icons.wallet,
            sparkline: buildSparklineSvg()
        }),
        SummaryCard({
            id: "summary-gain",
            label: "Total Gain / Loss",
            value: formatCurrency(metrics.totalGain),
            detail: formatPercent(metrics.totalGainPct),
            tone: metrics.totalGain >= 0 ? "positive" : "negative",
            icon: icons.trend
        }),
        SummaryCard({
            id: "summary-day-change",
            label: "Day's Change",
            value: formatCurrency(metrics.dayChangeAmount),
            detail: formatPercent(metrics.dayChangePct),
            tone: metrics.dayChangeAmount >= 0 ? "positive" : "negative",
            icon: icons.check
        }),
        SummaryCard({
            id: "summary-cash-balance",
            label: "Cash Balance",
            value: formatCurrency(metrics.cashBalance),
            detail: "Available",
            tone: "neutral",
            icon: icons.wallet,
            actions: (() => {
                const cashHolding = getPrimaryCashHolding();
                if (!cashHolding) {
                    return "";
                }
                return `
                    <button class="secondary-btn compact-btn" type="button" data-cash-action="add" data-cash-id="${cashHolding.id}">Add Cash</button>
                    <button class="secondary-btn compact-btn" type="button" data-cash-action="remove" data-cash-id="${cashHolding.id}">Remove Cash</button>
                `;
            })()
        })
    ].join("");

    const charts = view.showPerformance ? `${PortfolioPerformanceChart(state.selectedRange)}${AssetAllocationChart(state.allocationData)}` : "";
    const holdingsMarkup = view.holdingsMarkup || (view.showHoldings ? HoldingsTable(state.holdings, state.searchTerm, state.selectedType) : "");
    const transactionsMarkup = view.transactionsMarkup || (view.showTransactions ? RecentTransactions(state.transactions) : "");
    const portfolioOptions = state.portfolios
        .map(
            (item) =>
                `<option value="${item.id}" ${item.id === state.selectedPortfolioId ? "selected" : ""}>${toInputSafeText(item.name || `Portfolio ${item.id}`)}</option>`
        )
        .join("");
    const globalErrorMarkup = state.globalError
        ? `<article class="card info-panel"><h2>System Message</h2><p class="form-error">${toInputSafeText(state.globalError)}</p></article>`
        : "";

    appRoot.innerHTML = AppLayout({
        topNavbar: TopNavbar(),
        sidebar: Sidebar({
            totalValue: formatCurrency(metrics.totalValue),
            dayChange: `${formatSignedCurrency(metrics.dayChangeAmount)} (${formatPercent(metrics.dayChangePct)})`,
            activeNav: state.activeNav
        }),
        header: DashboardHeader({
            title: view.title,
            subtitle: view.subtitle,
            showAddAsset: view.showAddAsset,
            extraControls: `<label class="header-select-wrap">Portfolio<select id="portfolio-switch" class="header-select">${portfolioOptions}</select></label>`
        }),
        summaryCards: view.showSummary ? summaryCards : globalErrorMarkup,
        charts,
        holdingsTable: holdingsMarkup,
        recentTransactions: transactionsMarkup,
        addAssetModal: AddAssetModal(state.addModalOpen, getEditingAsset()),
        confirmDeleteModal: ConfirmDeleteModal(getDeleteTargetAsset()),
        cashActionModal: CashActionModal(state.cashActionMode, getCashHolding(), getCurrentCashBalance())
    });

    document.body.classList.toggle("sidebar-open", state.sidebarOpen);
    applyTheme();
    if (view.showPerformance) {
        initCharts(metrics.totalValue);
    } else {
        if (performanceChart) {
            performanceChart.destroy();
            performanceChart = null;
        }
        if (allocationChart) {
            allocationChart.destroy();
            allocationChart = null;
        }
    }
    bindEvents();

    if (state.addModalOpen) {
        focusModalField("asset-ticker");
    } else if (state.cashActionMode) {
        focusModalField("cash-action-amount");
    }
}

function initCharts(totalValue) {
    if (performanceChart) {
        performanceChart.destroy();
    }
    if (allocationChart) {
        allocationChart.destroy();
    }

    const selectedData = getPerformanceDataByRange(state.selectedRange);
    const perfCtx = document.getElementById("performance-chart")?.getContext("2d");
    const allocCtx = document.getElementById("allocation-chart")?.getContext("2d");

    if (perfCtx) {
        const gradient = perfCtx.createLinearGradient(0, 0, 0, 260);
        gradient.addColorStop(0, "rgba(59, 130, 246, 0.24)");
        gradient.addColorStop(1, "rgba(59, 130, 246, 0)");

        performanceChart = new Chart(perfCtx, {
            type: "line",
            data: {
                labels: selectedData.map((item) => compactDate(item.date)),
                datasets: [
                    {
                        data: selectedData.map((item) => item.value),
                        borderColor: "#2563eb",
                        backgroundColor: gradient,
                        pointRadius: 0,
                        pointHoverRadius: 4,
                        fill: true,
                        borderWidth: 2.5,
                        tension: 0.25
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: {
                            color: "#94a3b8",
                            maxTicksLimit: 7,
                            callback: (_, index) => {
                                const total = selectedData.length;
                                const every = Math.max(1, Math.floor(total / 7));
                                return index % every === 0 ? compactDate(selectedData[index].date) : "";
                            }
                        }
                    },
                    y: {
                        grid: { color: "#eef2ff" },
                        ticks: {
                            color: "#94a3b8",
                            callback: (value) => `$${Math.round(value / 1000)}K`
                        }
                    }
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: "#0f172a",
                        titleColor: "#f8fafc",
                        bodyColor: "#e2e8f0",
                        displayColors: false,
                        callbacks: {
                            title: (items) => {
                                const dataIndex = items[0]?.dataIndex || 0;
                                return compactDate(selectedData[dataIndex].date);
                            },
                            label: (item) => formatCurrency(item.raw)
                        }
                    }
                }
            }
        });
    }

    if (allocCtx) {
        const centerLabel = {
            id: "centerLabel",
            afterDraw(chart) {
                const { ctx } = chart;
                const meta = chart.getDatasetMeta(0);
                if (!meta?.data?.length) {
                    return;
                }
                const x = meta.data[0].x;
                const y = meta.data[0].y;
                ctx.save();
                ctx.textAlign = "center";
                ctx.fillStyle = "#111827";
                ctx.font = "700 23px Manrope, sans-serif";
                ctx.fillText(formatCurrency(totalValue), x, y - 2);
                ctx.fillStyle = "#6b7280";
                ctx.font = "500 13px Manrope, sans-serif";
                ctx.fillText("Total", x, y + 20);
                ctx.restore();
            }
        };

        allocationChart = new Chart(allocCtx, {
            type: "doughnut",
            data: {
                labels: state.allocationData.map((item) => item.label),
                datasets: [
                    {
                        data: state.allocationData.map((item) => item.value),
                        backgroundColor: state.allocationData.map((item) => item.color),
                        borderColor: "#ffffff",
                        borderWidth: 2,
                        hoverOffset: 5,
                        cutout: "68%"
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (item) => `${item.label}: ${Number(item.raw).toFixed(1)}%`
                        }
                    }
                }
            },
            plugins: [centerLabel]
        });
    }
}

function bindCashActionModalEvents() {
    const modal = document.getElementById("cash-action-modal");
    if (!modal) {
        return;
    }

    const form = document.getElementById("cash-action-form");
    const cancelButton = document.getElementById("cancel-cash-action");
    const error = document.getElementById("cash-action-error");
    const cashHolding = getCashHolding();

    cancelButton?.addEventListener("click", () => closeCashActionModal());

    modal.addEventListener("click", (event) => {
        if (event.target.id === "cash-action-modal") {
            closeCashActionModal();
        }
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!cashHolding) {
            error.textContent = "Cash holding not found.";
            return;
        }

        const amount = Number(new FormData(form).get("amount"));
        if (!(amount > 0)) {
            error.textContent = "Amount must be greater than 0.";
            return;
        }

        const currentBalance = getCurrentCashBalance();
        const nextBalance = state.cashActionMode === "remove" ? currentBalance - amount : currentBalance + amount;
        if (nextBalance < 0) {
            error.textContent = "Amount exceeds available cash balance.";
            return;
        }

        const payload = {
            portfolioId: cashHolding.portfolioId,
            assetType: cashHolding.assetType,
            ticker: cashHolding.ticker || "CASH",
            quantity: Number(nextBalance.toFixed(4)),
            purchasePrice: Number(cashHolding.purchasePrice || 1),
            purchasedata: cashHolding.purchasedata || new Date().toISOString().slice(0, 10),
            currency: cashHolding.currency || "USD"
        };

        try {
            await apiRequest(`/holding/${cashHolding.id}`, {
                method: "PATCH",
                body: JSON.stringify(payload)
            });
            closeCashActionModal(false);
            state.loading = true;
            render();
            await refreshPortfolioData();
        } catch (submitError) {
            error.textContent = submitError.message;
            return;
        }

        state.loading = false;
        render();
        restoreFocus();
    });
}

function bindEvents() {
    const menuBtn = document.getElementById("menu-toggle");
    if (menuBtn) {
        menuBtn.addEventListener("click", () => {
            state.sidebarOpen = !state.sidebarOpen;
            document.body.classList.toggle("sidebar-open", state.sidebarOpen);
        });
    }

    const themeToggleBtn = document.getElementById("theme-toggle-btn");
    if (themeToggleBtn) {
        themeToggleBtn.addEventListener("click", () => {
            toggleTheme();
        });
    }

    const portfolioSwitch = document.getElementById("portfolio-switch");
    if (portfolioSwitch) {
        portfolioSwitch.addEventListener("change", async (event) => {
            state.selectedPortfolioId = Number(event.target.value);
            state.loading = true;
            render();
            try {
                await refreshPortfolioData();
            } catch (error) {
                state.globalError = error.message;
            } finally {
                state.loading = false;
                render();
            }
        });
    }

    const settingsThemeBtn = document.getElementById("settings-theme-toggle");
    if (settingsThemeBtn) {
        settingsThemeBtn.addEventListener("click", () => {
            toggleTheme();
        });
    }

    document.querySelectorAll("[data-nav]").forEach((button) => {
        button.addEventListener("click", () => {
            const target = button.dataset.nav;
            if (!target || target === state.activeNav) {
                return;
            }
            state.activeNav = target;
            state.sidebarOpen = false;
            render();
        });
    });

    const transactionForm = document.getElementById("transaction-form");
    if (transactionForm) {
        transactionForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const formData = new FormData(transactionForm);
            try {
                const payload = {
                    holdingId: Number(formData.get("holdingId")),
                    type: String(formData.get("type") || "BUY").toUpperCase(),
                    quantity: Number(formData.get("quantity")),
                    price: Number(formData.get("price")),
                    tradeDate: new Date(String(formData.get("tradeDate"))).toISOString().slice(0, 19)
                };
                await apiRequest("/savetransaction", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
                state.transactionMessage = "Transaction saved.";
                await refreshPortfolioData();
                render();
            } catch (error) {
                state.transactionMessage = error.message;
                render();
            }
        });
    }

    document.querySelectorAll("[data-tx-delete]").forEach((button) => {
        button.addEventListener("click", async () => {
            try {
                await apiRequest(`/delete/transaction/${button.dataset.txDelete}`, { method: "DELETE" });
                state.transactionMessage = "Transaction deleted.";
                await refreshPortfolioData();
                render();
            } catch (error) {
                state.transactionMessage = error.message;
                render();
            }
        });
    });

    const priceForm = document.getElementById("price-form");
    if (priceForm) {
        priceForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const formData = new FormData(priceForm);
            try {
                const payload = {
                    ticker: String(formData.get("ticker") || "").trim().toUpperCase(),
                    priceDate: String(formData.get("priceDate") || "").trim(),
                    closeprice: Number(formData.get("closeprice"))
                };
                await apiRequest("/saveprice", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
                state.priceMessage = "Price record saved.";
                await refreshPortfolioData();
                render();
            } catch (error) {
                state.priceMessage = error.message;
                render();
            }
        });
    }

    document.querySelectorAll("[data-price-delete]").forEach((button) => {
        button.addEventListener("click", async () => {
            const [ticker, date] = String(button.dataset.priceDelete || "").split("|");
            if (!ticker || !date) {
                return;
            }
            try {
                const query = `ticker=${encodeURIComponent(ticker)}&date=${encodeURIComponent(date)}`;
                await apiRequest(`/delete/price?${query}`, { method: "DELETE" });
                state.priceMessage = "Price record deleted.";
                await refreshPortfolioData();
                render();
            } catch (error) {
                state.priceMessage = error.message;
                render();
            }
        });
    });

    document.querySelectorAll(".range-btn").forEach((button) => {
        button.addEventListener("click", () => {
            state.selectedRange = button.dataset.range;
            render();
        });
    });

    const addAssetBtn = document.getElementById("add-asset-btn");
    if (addAssetBtn) {
        addAssetBtn.addEventListener("click", () => {
            lastFocusedElement = document.activeElement;
            state.addModalOpen = true;
            state.editingAssetId = null;
            render();
        });
    }

    const searchInput = document.getElementById("holding-search");
    if (searchInput) {
        searchInput.addEventListener("input", (event) => {
            state.searchTerm = event.target.value;
            render();
        });
    }

    const filterSelect = document.getElementById("holding-type-filter");
    if (filterSelect) {
        filterSelect.addEventListener("change", (event) => {
            state.selectedType = event.target.value;
            render();
        });
    }

    document.querySelectorAll("[data-delete-id]").forEach((button) => {
        button.addEventListener("click", () => {
            lastFocusedElement = document.activeElement;
            state.deleteTargetId = Number(button.dataset.deleteId);
            render();
        });
    });

    document.querySelectorAll("[data-edit-id]").forEach((button) => {
        button.addEventListener("click", () => {
            lastFocusedElement = document.activeElement;
            state.editingAssetId = Number(button.dataset.editId);
            state.addModalOpen = true;
            render();
        });
    });

    document.querySelectorAll("[data-cash-action]").forEach((button) => {
        button.addEventListener("click", () => {
            lastFocusedElement = document.activeElement;
            state.cashActionMode = button.dataset.cashAction;
            state.cashActionHoldingId = Number(button.dataset.cashId);
            render();
        });
    });

    bindAddAssetModalEvents();
    bindDeleteModalEvents();
    bindCashActionModalEvents();
    bindModalKeyboardEscape();
}

function bindAddAssetModalEvents() {
    const modal = document.getElementById("add-asset-modal");
    if (!modal) {
        return;
    }

    const cancelButton = document.getElementById("cancel-add-asset");
    const form = document.getElementById("add-asset-form");
    const error = document.getElementById("add-asset-error");

    cancelButton?.addEventListener("click", () => closeAddAssetModal());

    modal.addEventListener("click", (event) => {
        if (event.target.id === "add-asset-modal") {
            closeAddAssetModal();
        }
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const formData = new FormData(form);
        const ticker = String(formData.get("ticker") || "").trim().toUpperCase();
        const name = String(formData.get("name") || "").trim();
        const type = String(formData.get("type") || "Stock");
        const quantity = Number(formData.get("quantity"));
        const price = Number(formData.get("price"));

        if (!ticker || !name) {
            error.textContent = "Ticker and asset name are required.";
            return;
        }
        if (!(quantity > 0) || !(price >= 0)) {
            error.textContent = "Quantity must be positive and price cannot be negative.";
            return;
        }

        const enumType = type.toUpperCase();
        const payload = {
            portfolioId: state.selectedPortfolioId,
            assetType: enumType,
            ticker: enumType === "CASH" ? "CASH" : ticker,
            quantity,
            purchasePrice: price,
            purchasedata: new Date().toISOString().slice(0, 10),
            currency: "USD"
        };

        try {
            if (state.editingAssetId) {
                await apiRequest(`/holding/${state.editingAssetId}`, {
                    method: "PATCH",
                    body: JSON.stringify(payload)
                });
            } else {
                await apiRequest("/saveholding", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
            }

            state.globalError = "";
            closeAddAssetModal();
            state.loading = true;
            render();
            await refreshPortfolioData();
        } catch (submitError) {
            error.textContent = submitError.message;
            return;
        } finally {
            state.loading = false;
            render();
        }
    });
}

function bindDeleteModalEvents() {
    const modal = document.getElementById("confirm-delete-modal");
    if (!modal) {
        return;
    }

    const cancelButton = document.getElementById("cancel-delete-asset");
    const confirmButton = document.getElementById("confirm-delete-asset");

    cancelButton?.addEventListener("click", () => closeDeleteModal());

    confirmButton?.addEventListener("click", async () => {
        if (!state.deleteTargetId) {
            return;
        }
        try {
            await apiRequest(`/delete/holding/${state.deleteTargetId}`, { method: "DELETE" });
            state.deleteTargetId = null;
            state.loading = true;
            render();
            await refreshPortfolioData();
            state.loading = false;
            render();
            restoreFocus();
        } catch (error) {
            state.globalError = error.message;
            state.deleteTargetId = null;
            render();
        }
    });

    modal.addEventListener("click", (event) => {
        if (event.target.id === "confirm-delete-modal") {
            closeDeleteModal();
        }
    });
}

function bindModalKeyboardEscape() {
    if (escapeHandlerBound) {
        return;
    }

    document.addEventListener("click", (event) => {
        const deleteBtn = event.target.closest("[data-delete-id]");
        if (deleteBtn) {
            lastFocusedElement = document.activeElement;
            state.deleteTargetId = Number(deleteBtn.dataset.deleteId);
            render();
            return;
        }

        const editBtn = event.target.closest("[data-edit-id]");
        if (editBtn) {
            lastFocusedElement = document.activeElement;
            state.editingAssetId = Number(editBtn.dataset.editId);
            state.addModalOpen = true;
            render();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            if (state.addModalOpen) {
                closeAddAssetModal();
            } else if (state.cashActionMode) {
                closeCashActionModal();
            } else if (state.deleteTargetId) {
                closeDeleteModal();
            }
        }
    });

    escapeHandlerBound = true;
}

function closeAddAssetModal() {
    state.addModalOpen = false;
    state.editingAssetId = null;
    render();
    restoreFocus();
}

function closeCashActionModal(shouldRender = true) {
    state.cashActionMode = null;
    state.cashActionHoldingId = null;
    if (shouldRender) {
        render();
        restoreFocus();
    }
}

function closeDeleteModal() {
    state.deleteTargetId = null;
    render();
    restoreFocus();
}

function focusModalField(fieldId) {
    const field = document.getElementById(fieldId);
    if (field) {
        field.focus();
    }
}

function restoreFocus() {
    if (lastFocusedElement && typeof lastFocusedElement.focus === "function") {
        lastFocusedElement.focus();
    }
}

loadInitialData();
