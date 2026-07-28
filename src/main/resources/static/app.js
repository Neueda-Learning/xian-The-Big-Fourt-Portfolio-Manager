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
import { icons } from "./js/components/icons.js";
import { allocationTarget, generatePerformanceData, initialHoldings, initialTransactions } from "./js/data/mockData.js";
import { compactDate, formatCurrency, formatPercent, formatSignedCurrency } from "./js/utils/formatters.js";

const appRoot = document.getElementById("app");
const savedTheme = window.localStorage.getItem("pm-theme");

const state = {
    holdings: [...initialHoldings],
    transactions: [...initialTransactions],
    performanceData: generatePerformanceData(380),
    allocationData: allocationTarget,
    searchTerm: "",
    selectedType: "All Types",
    selectedRange: "1Y",
    addModalOpen: false,
    editingAssetId: null,
    deleteTargetId: null,
    sidebarOpen: false,
    activeNav: "Dashboard",
    darkMode: savedTheme === "dark"
};

let performanceChart;
let allocationChart;
let lastFocusedElement;
let escapeHandlerBound = false;

function computeMetrics() {
    const totalValue = state.holdings.reduce((sum, item) => sum + item.quantity * item.currentPrice, 0);
    const totalCost = state.holdings.reduce((sum, item) => sum + item.quantity * item.avgPrice, 0);
    const totalGain = totalValue - totalCost;
    const totalGainPct = totalCost > 0 ? (totalGain / totalCost) * 100 : 0;
    const cashBalance = state.holdings
        .filter((item) => item.type === "Cash")
        .reduce((sum, item) => sum + item.quantity * item.currentPrice, 0);

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
    const rangeMap = {
        "1D": 1,
        "1W": 7,
        "1M": 30,
        "3M": 90,
        "1Y": 365,
        ALL: data.length
    };

    const count = rangeMap[range] || 365;
    return data.slice(Math.max(0, data.length - count));
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
    return state.holdings.find((holding) => holding.id === state.editingAssetId) || null;
}

function getDeleteTargetAsset() {
    if (!state.deleteTargetId) {
        return null;
    }
    return state.holdings.find((holding) => holding.id === state.deleteTargetId) || null;
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
            showHoldings: false
        };
    }

    if (state.activeNav === "Performance") {
        return {
            ...defaults,
            title: "Performance",
            subtitle: "Analyze portfolio growth and allocation trends",
            showAddAsset: false,
            showHoldings: false,
            showTransactions: false
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

function applyTheme() {
    document.body.classList.toggle("dark-theme", state.darkMode);
    window.localStorage.setItem("pm-theme", state.darkMode ? "dark" : "light");
}

function toggleTheme() {
    state.darkMode = !state.darkMode;
    render();
}

function render() {
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
            icon: icons.wallet
        })
    ].join("");

    const charts = view.showPerformance ? `${PortfolioPerformanceChart(state.selectedRange)}${AssetAllocationChart(state.allocationData)}` : "";
    const holdingsMarkup = view.holdingsMarkup || (view.showHoldings ? HoldingsTable(state.holdings, state.searchTerm, state.selectedType) : "");
    const transactionsMarkup = view.transactionsMarkup || (view.showTransactions ? RecentTransactions(state.transactions) : "");

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
            showAddAsset: view.showAddAsset
        }),
        summaryCards: view.showSummary ? summaryCards : "",
        charts,
        holdingsTable: holdingsMarkup,
        recentTransactions: transactionsMarkup,
        addAssetModal: AddAssetModal(state.addModalOpen, getEditingAsset()),
        confirmDeleteModal: ConfirmDeleteModal(getDeleteTargetAsset())
    });

    document.body.classList.toggle("sidebar-open", state.sidebarOpen);
    applyTheme();
    initCharts(metrics.totalValue);
    bindEvents();

    if (state.addModalOpen) {
        focusModalField("asset-ticker");
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
            state.deleteTargetId = button.dataset.deleteId;
            render();
        });
    });

    document.querySelectorAll("[data-edit-id]").forEach((button) => {
        button.addEventListener("click", () => {
            lastFocusedElement = document.activeElement;
            state.editingAssetId = button.dataset.editId;
            state.addModalOpen = true;
            render();
        });
    });

    bindAddAssetModalEvents();
    bindDeleteModalEvents();
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

    form?.addEventListener("submit", (event) => {
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

        const currentPrice = type === "Cash" ? price : Number((price * 1.04).toFixed(2));

        if (state.editingAssetId) {
            state.holdings = state.holdings.map((holding) => {
                if (holding.id !== state.editingAssetId) {
                    return holding;
                }
                return {
                    ...holding,
                    id: ticker,
                    ticker,
                    name,
                    type,
                    quantity,
                    avgPrice: price,
                    currentPrice
                };
            });
        } else {
            state.holdings.unshift({
                id: ticker,
                ticker,
                name,
                type,
                quantity,
                avgPrice: price,
                currentPrice,
                currency: "USD"
            });

            state.transactions.unshift({
                id: `txn-${Date.now()}`,
                date: new Intl.DateTimeFormat("en-US", {
                    month: "short",
                    day: "numeric",
                    year: "numeric"
                }).format(new Date()),
                type,
                asset: ticker,
                action: "Buy",
                quantity,
                price,
                amount: -(quantity * price)
            });
        }

        closeAddAssetModal();
        render();
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

    confirmButton?.addEventListener("click", () => {
        if (!state.deleteTargetId) {
            return;
        }
        state.holdings = state.holdings.filter((holding) => holding.id !== state.deleteTargetId);
        state.deleteTargetId = null;
        render();
        restoreFocus();
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

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            if (state.addModalOpen) {
                closeAddAssetModal();
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

render();
