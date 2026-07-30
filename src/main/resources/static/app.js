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
import { AiAssistantPanel } from "./js/components/AiAssistantPanel.js";
import { icons } from "./js/components/icons.js";
import { compactDate, formatCurrency, formatPercent, formatSignedCurrency, toInputSafeText } from "./js/utils/formatters.js";

const appRoot = document.getElementById("app");
if (!appRoot) {
    throw new Error("App root element '#app' was not found.");
}
const savedTheme = window.localStorage.getItem("pm-theme");

const state = {
    portfolios: [],
    selectedPortfolioId: null,
    holdingsRaw: [],
    holdings: [],
    transactions: [],
    snapshots: [],
    priceHistory: [],
    performance: null,
    summary: null,
    performanceData: [],
    allocationData: [],
    loading: true,
    globalError: "",
    searchTerm: "",
    selectedType: "All Types",
    selectedRange: "1Y",
    addModalOpen: false,
    editingAssetId: null,
    sidebarOpen: false,
    activeNav: "Dashboard",
    darkMode: savedTheme === "dark",
    editingTransactionId: null,
    tradeDraft: null,
    priceMessage: "",
    transactionMessage: "",
    aiApiKeyStatus: {
        userKeyConfigured: false,
        systemKeyConfigured: false,
        activeKeySource: "none"
    },
    aiApiKeyStatusLoaded: false,
    aiApiKeyBusy: false,
    aiApiKeyFeedback: "",
    aiAssistantLoading: false,
    aiAssistantBusy: false,
    aiAssistantModelsLoaded: false,
    aiAssistantModels: [],
    aiAssistantProvider: "",
    aiAssistantSelectedModel: "",
    aiAssistantPrompt: "",
    aiAssistantResponse: "",
    aiAssistantError: ""
};

let performanceChart;
let allocationChart;
let lastFocusedElement;
let escapeHandlerBound = false;

/*
 * Eren issue: performance curve looked accurate but was inferred from single-ticker history or mock fallback.
 * Fix: only render curve from real portfolio-level points; keep single-point display until snapshot history exists.
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

function getAiApiKeyStatusMessage() {
    const source = String(state.aiApiKeyStatus?.activeKeySource || "none").toLowerCase();
    if (source === "user") {
        return "Using personal API key";
    }
    if (source === "system") {
        return "Using system default API key";
    }
    return "No API key configured";
}

function syncAiApiSettingsUi() {
    const statusEl = document.getElementById("ai-api-key-status-text");
    const feedbackEl = document.getElementById("ai-api-key-feedback");
    const saveBtn = document.getElementById("save-ai-api-key-btn");
    const clearBtn = document.getElementById("clear-ai-api-key-btn");

    if (statusEl) {
        // 中文注释：状态文案统一通过 textContent 写入，避免拼接 HTML。
        statusEl.textContent = getAiApiKeyStatusMessage();
    }
    if (feedbackEl) {
        feedbackEl.textContent = state.aiApiKeyFeedback || "";
    }
    if (saveBtn) {
        saveBtn.disabled = state.aiApiKeyBusy;
    }
    if (clearBtn) {
        clearBtn.disabled = state.aiApiKeyBusy;
    }
}

function normalizeAiApiKeyStatus(payload) {
    const source = String(payload?.activeKeySource || "none").toLowerCase();
    const activeKeySource = source === "user" || source === "system" ? source : "none";
    return {
        userKeyConfigured: Boolean(payload?.userKeyConfigured),
        systemKeyConfigured: Boolean(payload?.systemKeyConfigured),
        activeKeySource
    };
}

async function fetchAiApiKeyStatus() {
    const response = await apiRequest("/api/ai/settings/api-key");
    state.aiApiKeyStatus = normalizeAiApiKeyStatus(response);
    state.aiApiKeyStatusLoaded = true;
}

async function loadAiApiKeyStatus() {
    if (state.aiApiKeyBusy) {
        return;
    }
    state.aiApiKeyBusy = true;
    state.aiApiKeyFeedback = "";
    syncAiApiSettingsUi();
    try {
        await fetchAiApiKeyStatus();
    } catch (error) {
        state.aiApiKeyFeedback = error.message;
    } finally {
        state.aiApiKeyBusy = false;
        syncAiApiSettingsUi();
    }
}

function normalizeAiModelOptions(payload) {
    const models = Array.isArray(payload?.models)
        ? payload.models
            .map((model) => String(model || "").trim())
            .filter(Boolean)
        : [];
    const defaultModel = String(payload?.defaultModel || "").trim();
    const selectedDefault = models.includes(defaultModel) ? defaultModel : (models[0] || "");
    return {
        provider: String(payload?.provider || "").trim().toLowerCase(),
        models,
        defaultModel: selectedDefault
    };
}

async function fetchAiModelOptions() {
    const response = await apiRequest("/api/ai/models");
    const normalized = normalizeAiModelOptions(response);
    state.aiAssistantProvider = normalized.provider;
    state.aiAssistantModels = normalized.models;
    state.aiAssistantModelsLoaded = true;
    if (!state.aiAssistantSelectedModel || !normalized.models.includes(state.aiAssistantSelectedModel)) {
        state.aiAssistantSelectedModel = normalized.defaultModel;
    }
}

function getAiAssistantStatusMessage() {
    if (state.aiApiKeyStatusLoaded) {
        return `${getAiApiKeyStatusMessage()}. Manage API keys in Settings.`;
    }
    return "Manage API keys in Settings if the assistant cannot answer.";
}

async function loadAiAssistantData() {
    state.aiAssistantError = "";
    if (state.aiAssistantLoading) {
        return;
    }

    try {
        if (!state.aiAssistantModelsLoaded) {
            state.aiAssistantLoading = true;
            render();
            await fetchAiModelOptions();
        }
        if (!state.aiApiKeyStatusLoaded) {
            try {
                await fetchAiApiKeyStatus();
            } catch (error) {
                // Keep the chat available even if the status hint cannot be loaded.
            }
        }
    } catch (error) {
        state.aiAssistantError = error.message;
    } finally {
        state.aiAssistantLoading = false;
        render();
    }
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
        avgPrice: Number(rawHolding.averagePrice || 0),
        currentPrice: Number(detail?.currentPrice || rawHolding.currentPrice || 0),
        realizedPnl: Number(detail?.realizedPnl || 0),
        unrealizedPnl: Number(detail?.unrealizedPnl || 0),
        totalPnl: Number(detail?.totalPnl || 0),
        currency: rawHolding.currency || "USD",
        purchasedata: rawHolding.purchasedata
    };
}

function normalizeTransaction(rawTx, holdingMap) {
    const isCashTx = rawTx.holdingId == null;
    const holdingTicker = isCashTx ? "CASH" : (holdingMap.get(rawTx.holdingId)?.ticker || `Holding-${rawTx.holdingId}`);
    const txType = String(rawTx.type || "BUY").toUpperCase();
    const quantity = Number(rawTx.quantity || 0);
    const price = Number(rawTx.price || 0);
    const amount = txType === "SELL" || txType === "IN" ? quantity * price : -(quantity * price);
    const action = txType === "SELL" ? "Sell" : txType === "IN" ? "Deposit" : "Buy";

    return {
        id: rawTx.id,
        holdingId: rawTx.holdingId,
        date: new Intl.DateTimeFormat("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric"
        }).format(new Date(rawTx.tradeDate)),
        dateRaw: rawTx.tradeDate,
        type: isCashTx ? "Cash" : (holdingMap.get(rawTx.holdingId)?.type || "Stock"),
        asset: holdingTicker,
        action,
        txType,
        quantity,
        price,
        amount
    };
}

function rebuildAllocationData() {
    const allocation = state.summary?.allocation;
    if (!Array.isArray(allocation) || allocation.length === 0) {
        state.allocationData = [];
        return;
    }

    const colorMap = {
        STOCK: "#1d4ed8",
        BOND: "#059669",
        CASH: "#d97706"
    };

    state.allocationData = allocation.map((item) => {
        const key = String(item.assetType || "OTHER").toUpperCase();
        const label = key.charAt(0) + key.slice(1).toLowerCase();
        return {
            label: `${label}s`,
            value: Number(item.percentage || 0),
            color: colorMap[key] || "#6b7280"
        };
    });
}

function buildPerformanceSeries() {
    const orderedSnapshots = [...state.snapshots]
        .sort((a, b) => new Date(a.snapshotDate).getTime() - new Date(b.snapshotDate).getTime());

    if (orderedSnapshots.length > 0) {
        state.performanceData = orderedSnapshots.map((item) => ({
            date: new Date(`${item.snapshotDate}T16:00:00`),
            value: Number(item.totalValue || 0)
        }));

        const totalValue = Number(state.performance?.totalMarketValue || 0);
        const lastPoint = state.performanceData[state.performanceData.length - 1];
        const today = new Date().toDateString();
        if (!lastPoint || new Date(lastPoint.date).toDateString() !== today) {
            state.performanceData.push({
                date: new Date(),
                value: Number(totalValue.toFixed(2))
            });
        } else {
            lastPoint.value = Number(totalValue.toFixed(2));
        }
        return;
    }

    const totalValue = Number(state.performance?.totalMarketValue || state.holdings.reduce((sum, item) => sum + item.quantity * item.currentPrice, 0));
    state.performanceData = [
        {
            date: new Date(),
            value: Number(totalValue.toFixed(2))
        }
    ];
}

async function refreshPortfolioData() {
    if (!state.selectedPortfolioId) {
        state.holdingsRaw = [];
        state.holdings = [];
        state.transactions = [];
        state.snapshots = [];
        state.priceHistory = [];
        state.performance = null;
        state.summary = null;
        state.performanceData = [];
        state.allocationData = [];
        return;
    }

    const holdingsRaw = await apiRequest(`/holdings/portfolio/${state.selectedPortfolioId}`);
    state.holdingsRaw = Array.isArray(holdingsRaw) ? holdingsRaw : [];

    state.performance = await apiRequest(`/portfolio/${state.selectedPortfolioId}/performance`);
    state.summary = await apiRequest(`/portfolios/${state.selectedPortfolioId}/summary`);
    const detailMap = new Map((state.performance?.holdingsDetail || []).map((item) => [item.holdingId, item]));
    const mappedHoldings = state.holdingsRaw.map((item) => normalizeHolding(item, detailMap));
    state.holdings = mappedHoldings.filter((item) => item.type !== "Cash");

    const holdingMap = new Map(mappedHoldings.map((item) => [item.id, item]));
    const txResult = await apiRequest(`/portfolios/${state.selectedPortfolioId}/transactions`);
    const flattened = Array.isArray(txResult) ? txResult : [];
    state.transactions = flattened
        .map((item) => normalizeTransaction(item, holdingMap))
        .sort((a, b) => new Date(b.dateRaw).getTime() - new Date(a.dateRaw).getTime());

    const snapshotsResult = await apiRequest(`/portfolios/${state.selectedPortfolioId}/snapshots`);
    state.snapshots = Array.isArray(snapshotsResult) ? snapshotsResult : [];

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
    const totalValue = state.summary?.totalValue ?? state.performance?.totalMarketValue ?? null;
    const totalGain = state.summary?.totalGain ?? state.performance?.totalReturn ?? null;
    const totalGainPct = state.summary?.totalGainPercentage ?? state.performance?.returnRate ?? null;
    const cashBalance = Number(state.summary?.cashBalance ?? state.performance?.cashBalance ?? 0);
    const dayChangeReliable = Boolean(state.summary?.dayChangeReliable);

    let dayChangeAmount = state.summary?.dayChangeAmount ?? null;
    let dayChangePct = state.summary?.dayChangePercentage ?? null;

    if (!dayChangeReliable && totalValue !== null) {
        const selectedData = getPerformanceDataByRange(state.selectedRange);
        const lastValue = selectedData[selectedData.length - 1]?.value || 0;
        const prevValue = selectedData[selectedData.length - 2]?.value || lastValue;
        dayChangeAmount = lastValue - prevValue;
        dayChangePct = prevValue > 0 ? (dayChangeAmount / prevValue) * 100 : 0;
    }

    return {
        totalValue,
        totalGain,
        totalGainPct,
        dayChangeAmount,
        dayChangePct,
        cashBalance,
        dayChangeReliable
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
            subtitle: "Track portfolio value over time and allocation by asset class",
            showAddAsset: false,
            showSummary: false,
            showHoldings: false,
            showTransactions: false
        };
    }

    if (state.activeNav === "AI Assistant") {
        return {
            ...defaults,
            title: "AI Assistant",
            subtitle: "Ask the backend assistant questions about portfolio and market knowledge",
            showAddAsset: false,
            showSummary: false,
            showPerformance: false,
            showHoldings: false,
            showTransactions: false,
            holdingsMarkup: AiAssistantPanel({
                loading: state.aiAssistantLoading,
                busy: state.aiAssistantBusy,
                provider: state.aiAssistantProvider,
                models: state.aiAssistantModels,
                selectedModel: state.aiAssistantSelectedModel,
                draftMessage: state.aiAssistantPrompt,
                responseText: state.aiAssistantResponse,
                errorMessage: state.aiAssistantError,
                apiKeyStatus: getAiAssistantStatusMessage()
            })
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
    const allHoldings = Array.isArray(state.holdingsRaw) ? state.holdingsRaw : [];
    const totalAssets = allHoldings.length;
    const stockCount = allHoldings.filter((item) => String(item.assetType || "").toUpperCase() === "STOCK").length;
    const bondCount = allHoldings.filter((item) => String(item.assetType || "").toUpperCase() === "BOND").length;
    const cashCount = allHoldings.filter((item) => String(item.assetType || "").toUpperCase() === "CASH").length;

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
        <article class="card info-panel">
            <h2>AI API Settings</h2>
            <p>Manage your personal AI API key for the current session.</p>
            <div class="settings-stack">
                <label for="ai-api-key-input">API Key</label>
                <input type="password" id="ai-api-key-input" maxlength="500" autocomplete="off">
                <div class="settings-actions">
                    <button class="primary-btn" id="save-ai-api-key-btn" type="button">Save API Key</button>
                    <button class="secondary-btn" id="clear-ai-api-key-btn" type="button">Clear API Key</button>
                </div>
                <p class="settings-status" id="ai-api-key-status-text"></p>
                <p class="settings-note">Your API key is stored only for the current session and will not be displayed after saving.</p>
                <p class="form-error" id="ai-api-key-feedback"></p>
            </div>
        </article>
    `;
}

function toIsoDateTimeSeconds(rawValue, fieldName) {
    const parsed = new Date(String(rawValue || "").trim());
    if (Number.isNaN(parsed.getTime())) {
        throw new Error(`${fieldName} is invalid.`);
    }
    return parsed.toISOString().slice(0, 19);
}

function renderTransactionManager() {
    const selectedHoldingId = state.tradeDraft?.holdingId;
    const selectedType = state.tradeDraft?.type || "BUY";
    const selectedHolding = state.holdings.find((item) => item.id === selectedHoldingId);
    const draftTicker = state.tradeDraft?.ticker || selectedHolding?.ticker || "";
    const draftAssetType = state.tradeDraft?.assetType || selectedHolding?.type || "Stock";
    const draftCurrency = state.tradeDraft?.currency || selectedHolding?.currency || "USD";
    const holdingOptions = state.holdings
        .filter((holding) => holding.type !== "Cash")
        .map((holding) => `<option value="${holding.id}" ${selectedHoldingId === holding.id ? "selected" : ""}>${holding.ticker} (Holding #${holding.id})</option>`)
        .join("");

    return `
        <article class="card info-panel">
            <h2>Transaction Manager</h2>
            <p>Create BUY/SELL transaction records for the selected portfolio. Cash deposit is available on Dashboard.</p>
            <form id="transaction-form" class="manager-grid" novalidate>
                <label>Holding</label>
                <select name="holdingId">
                    <option value="">Auto (BUY uses ticker/assetType)</option>
                    ${holdingOptions}
                </select>

                <label>Ticker (for new BUY)</label>
                <input name="ticker" type="text" maxlength="20" value="${toInputSafeText(draftTicker)}" placeholder="e.g. GOOGL">

                <label>Asset Type</label>
                <select name="assetType">
                    <option value="STOCK" ${String(draftAssetType).toUpperCase() === "STOCK" ? "selected" : ""}>STOCK</option>
                    <option value="BOND" ${String(draftAssetType).toUpperCase() === "BOND" ? "selected" : ""}>BOND</option>
                </select>

                <label>Currency</label>
                <input name="currency" type="text" maxlength="3" value="${toInputSafeText(draftCurrency)}" placeholder="USD">

                <label>Type</label>
                <select name="type" required>
                    <option value="BUY" ${selectedType === "BUY" ? "selected" : ""}>BUY</option>
                    <option value="SELL" ${selectedType === "SELL" ? "selected" : ""}>SELL</option>
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

function renderCashDepositPanel() {
    return `
        <article class="card info-panel">
            <h2>Cash Deposit</h2>
            <p>Add funds directly from Dashboard. This updates cash and records a transaction.</p>
            <form id="cash-deposit-form" class="manager-grid" novalidate>
                <label>Deposit Amount</label>
                <input name="amount" type="number" min="0.01" step="0.01" required>

                <label>Deposit Time</label>
                <input name="tradeDate" type="datetime-local" required>

                <div class="manager-actions">
                    <button type="submit" class="primary-btn">Deposit Cash</button>
                </div>
            </form>
            <p class="form-error" id="cash-deposit-message">${toInputSafeText(state.transactionMessage)}</p>
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
            value: metrics.totalValue !== null ? formatCurrency(metrics.totalValue) : "—",
            detail: metrics.totalValue !== null ? `${formatSignedCurrency(metrics.dayChangeAmount)} (${formatPercent(metrics.dayChangePct)})` : "No holdings",
            tone: "positive",
            icon: icons.wallet,
            sparkline: metrics.totalValue !== null ? buildSparklineSvg() : ""
        }),
        SummaryCard({
            id: "summary-gain",
            label: "Total Gain / Loss",
            value: metrics.totalGain !== null ? formatCurrency(metrics.totalGain) : "—",
            detail: metrics.totalGainPct !== null ? formatPercent(metrics.totalGainPct) : "—",
            tone: metrics.totalGain !== null && metrics.totalGain >= 0 ? "positive" : "negative",
            icon: icons.trend
        }),
        SummaryCard({
            id: "summary-day-change",
            label: "Day's Change",
            value: metrics.dayChangeAmount !== null ? formatCurrency(metrics.dayChangeAmount) : "—",
            detail: metrics.dayChangeAmount !== null ? (metrics.dayChangeReliable ? formatPercent(metrics.dayChangePct) : "Awaiting yesterday snapshot") : "—",
            tone: metrics.dayChangeAmount !== null && metrics.dayChangeAmount >= 0 ? "positive" : "negative",
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
    const holdingsMarkup = view.holdingsMarkup || (view.showHoldings
        ? `${state.activeNav === "Dashboard" ? renderCashDepositPanel() : ""}${HoldingsTable(state.holdings, state.searchTerm, state.selectedType)}`
        : "");
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
    const summarySectionMarkup = `${globalErrorMarkup}${view.showSummary ? summaryCards : ""}`;

    appRoot.innerHTML = AppLayout({
        topNavbar: TopNavbar(),
        sidebar: Sidebar({
            totalValue: metrics.totalValue !== null ? formatCurrency(metrics.totalValue) : "—",
            dayChange: metrics.dayChangeAmount !== null ? `${formatSignedCurrency(metrics.dayChangeAmount)} (${formatPercent(metrics.dayChangePct)})` : "—",
            dayChangeTone: metrics.dayChangeAmount !== null && metrics.dayChangeAmount < 0 ? "negative" : "positive",
            activeNav: state.activeNav
        }),
        header: DashboardHeader({
            title: view.title,
            subtitle: view.subtitle,
            showAddAsset: view.showAddAsset,
            extraControls: `<label class="header-select-wrap">Portfolio<select id="portfolio-switch" class="header-select">${portfolioOptions}</select></label>`
        }),
        summaryCards: summarySectionMarkup,
        charts,
        holdingsTable: holdingsMarkup,
        recentTransactions: transactionsMarkup,
        addAssetModal: AddAssetModal(state.addModalOpen, getEditingAsset()),
        confirmDeleteModal: ""
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

    const aiModelSelect = document.getElementById("ai-model-select");
    if (aiModelSelect) {
        aiModelSelect.addEventListener("change", (event) => {
            state.aiAssistantSelectedModel = String(event.target.value || "").trim();
        });
    }

    const aiChatInput = document.getElementById("ai-chat-input");
    if (aiChatInput) {
        aiChatInput.addEventListener("input", (event) => {
            state.aiAssistantPrompt = event.target.value;
        });
    }

    const aiAssistantForm = document.getElementById("ai-assistant-form");
    if (aiAssistantForm) {
        aiAssistantForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (state.aiAssistantBusy) {
                return;
            }

            const selectedModel = String(document.getElementById("ai-model-select")?.value || state.aiAssistantSelectedModel || "").trim();
            const prompt = String(document.getElementById("ai-chat-input")?.value || state.aiAssistantPrompt || "");
            const trimmedPrompt = prompt.trim();

            if (!selectedModel) {
                state.aiAssistantError = "Please select an AI model.";
                render();
                return;
            }
            if (!trimmedPrompt) {
                state.aiAssistantError = "Question must not be empty.";
                render();
                return;
            }
            if (trimmedPrompt.length > 2000) {
                state.aiAssistantError = "Question must not exceed 2000 characters.";
                render();
                return;
            }

            state.aiAssistantSelectedModel = selectedModel;
            state.aiAssistantPrompt = prompt;
            state.aiAssistantBusy = true;
            state.aiAssistantError = "";
            state.aiAssistantResponse = "";
            render();

            try {
                const response = await apiRequest("/api/ai/chat", {
                    method: "POST",
                    body: JSON.stringify({
                        model: selectedModel,
                        message: trimmedPrompt
                    })
                });
                state.aiAssistantProvider = String(response?.provider || state.aiAssistantProvider || "").trim().toLowerCase();
                state.aiAssistantSelectedModel = String(response?.model || selectedModel).trim();
                state.aiAssistantResponse = String(response?.answer || "").trim();
            } catch (error) {
                state.aiAssistantError = error.message;
            } finally {
                state.aiAssistantBusy = false;
                render();
            }
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

    const saveAiApiKeyBtn = document.getElementById("save-ai-api-key-btn");
    const clearAiApiKeyBtn = document.getElementById("clear-ai-api-key-btn");
    const aiApiKeyInput = document.getElementById("ai-api-key-input");

    if (saveAiApiKeyBtn) {
        saveAiApiKeyBtn.addEventListener("click", async () => {
            if (state.aiApiKeyBusy) {
                return;
            }
            const raw = String(aiApiKeyInput?.value || "").trim();
            if (!raw) {
                state.aiApiKeyFeedback = "API key must not be empty.";
                syncAiApiSettingsUi();
                return;
            }
            if (raw.length > 500) {
                state.aiApiKeyFeedback = "API key must be at most 500 characters.";
                syncAiApiSettingsUi();
                return;
            }

            state.aiApiKeyBusy = true;
            state.aiApiKeyFeedback = "";
            syncAiApiSettingsUi();
            try {
                // 中文注释：前端只提交一次明文，保存后立即清空输入框，避免界面残留。
                const response = await apiRequest("/api/ai/settings/api-key", {
                    method: "POST",
                    body: JSON.stringify({ apiKey: raw })
                });
                if (aiApiKeyInput) {
                    aiApiKeyInput.value = "";
                }
                await fetchAiApiKeyStatus();
                state.aiApiKeyFeedback = response?.message || "API key saved.";
            } catch (error) {
                state.aiApiKeyFeedback = error.message;
            } finally {
                state.aiApiKeyBusy = false;
                syncAiApiSettingsUi();
            }
        });
    }

    if (clearAiApiKeyBtn) {
        clearAiApiKeyBtn.addEventListener("click", async () => {
            if (state.aiApiKeyBusy) {
                return;
            }
            state.aiApiKeyBusy = true;
            state.aiApiKeyFeedback = "";
            syncAiApiSettingsUi();
            try {
                const response = await apiRequest("/api/ai/settings/api-key", { method: "DELETE" });
                if (aiApiKeyInput) {
                    aiApiKeyInput.value = "";
                }
                await fetchAiApiKeyStatus();
                state.aiApiKeyFeedback = response?.message || "API key cleared.";
            } catch (error) {
                state.aiApiKeyFeedback = error.message;
            } finally {
                state.aiApiKeyBusy = false;
                syncAiApiSettingsUi();
            }
        });
    }

    if (state.activeNav === "Settings") {
        syncAiApiSettingsUi();
        if (!state.aiApiKeyStatusLoaded && !state.aiApiKeyBusy) {
            void loadAiApiKeyStatus();
        }
    } else if (state.activeNav === "AI Assistant" && (!state.aiAssistantModelsLoaded || !state.aiApiKeyStatusLoaded) && !state.aiAssistantLoading) {
        void loadAiAssistantData();
    }

    document.querySelectorAll("[data-nav]").forEach((button) => {
        button.addEventListener("click", () => {
            const target = button.dataset.nav;
            if (!target || target === state.activeNav) {
                return;
            }
            if (target === "Settings") {
                state.aiApiKeyStatusLoaded = false;
            }
            state.activeNav = target;
            state.sidebarOpen = false;
            render();
            if (target === "AI Assistant") {
                void loadAiAssistantData();
            }
        });
    });

    const transactionForm = document.getElementById("transaction-form");
    if (transactionForm) {
        transactionForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const formData = new FormData(transactionForm);
            try {
                const type = String(formData.get("type") || "BUY").toUpperCase();
                const holdingIdRaw = String(formData.get("holdingId") || "").trim();
                const holdingId = holdingIdRaw ? Number(holdingIdRaw) : null;
                const ticker = String(formData.get("ticker") || "").trim().toUpperCase();
                const assetType = String(formData.get("assetType") || "STOCK").trim().toUpperCase();
                const currency = String(formData.get("currency") || "USD").trim().toUpperCase();

                if (type === "SELL" && !holdingId) {
                    throw new Error("SELL requires an existing holding.");
                }

                if (type === "BUY" && !holdingId && !ticker) {
                    throw new Error("For a new BUY, provide ticker (or choose an existing holding).");
                }

                const payload = {
                    holdingId,
                    ticker,
                    assetType,
                    currency,
                    type,
                    quantity: Number(formData.get("quantity")),
                    price: Number(formData.get("price")),
                    tradeDate: toIsoDateTimeSeconds(formData.get("tradeDate"), "Trade Time")
                };
                const tradePath = payload.type === "SELL"
                    ? `/portfolios/${state.selectedPortfolioId}/trades/sell`
                    : `/portfolios/${state.selectedPortfolioId}/trades/buy`;
                await apiRequest(tradePath, {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
                state.tradeDraft = null;
                state.transactionMessage = "Transaction saved.";
                await refreshPortfolioData();
                render();
            } catch (error) {
                state.transactionMessage = error.message;
                render();
            }
        });
    }

    const cashDepositForm = document.getElementById("cash-deposit-form");
    if (cashDepositForm) {
        cashDepositForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const formData = new FormData(cashDepositForm);
            try {
                const amount = Number(formData.get("amount"));
                if (!(amount > 0)) {
                    throw new Error("Deposit amount must be greater than 0.");
                }

                const payload = {
                    amount,
                    tradeDate: toIsoDateTimeSeconds(formData.get("tradeDate"), "Deposit Time")
                };

                await apiRequest(`/portfolios/${state.selectedPortfolioId}/cash/deposit`, {
                    method: "POST",
                    body: JSON.stringify(payload)
                });

                state.transactionMessage = "Cash deposited and recorded.";
                await refreshPortfolioData();
                render();
            } catch (error) {
                state.transactionMessage = error.message;
                render();
            }
        });
    }

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
            state.activeNav = "Transactions";
            state.transactionMessage = "Use BUY/SELL transactions. New BUY can auto-create holding by ticker.";
            render();
        });
    }

    const addPortfolioBtn = document.getElementById("add-portfolio-btn");
    if (addPortfolioBtn) {
        addPortfolioBtn.addEventListener("click", async () => {
            const name = prompt("Enter portfolio name:");
            if (!name) return;
            const description = prompt("Enter description (optional):");
            try {
                await apiRequest("/saveportfolio", {
                    method: "POST",
                    body: JSON.stringify({ name, description: description || "", initialCash: 0 })
                });
                state.portfolios = await apiRequest("/portfolios");
                state.selectedPortfolioId = state.portfolios[state.portfolios.length - 1]?.id || null;
                state.loading = true;
                render();
                await refreshPortfolioData();
                state.loading = false;
                render();
            } catch (error) {
                state.globalError = error.message;
                render();
            }
        });
    }

    const deletePortfolioBtn = document.getElementById("delete-portfolio-btn");
    if (deletePortfolioBtn) {
        deletePortfolioBtn.addEventListener("click", async () => {
            if (!state.selectedPortfolioId) {
                state.globalError = "No portfolio selected";
                render();
                return;
            }
            if (state.portfolios.length <= 1) {
                state.globalError = "Cannot delete the last portfolio";
                render();
                return;
            }
            if (confirm("Are you sure you want to delete this portfolio?")) {
                try {
                    await apiRequest(`/delete/portfolio/${state.selectedPortfolioId}`, { method: "DELETE" });
                    state.portfolios = state.portfolios.filter(p => p.id !== state.selectedPortfolioId);
                    state.selectedPortfolioId = state.portfolios[0]?.id || null;
                    state.loading = true;
                    render();
                    await refreshPortfolioData();
                    state.loading = false;
                    render();
                } catch (error) {
                    state.globalError = error.message;
                    render();
                }
            }
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


    document.querySelectorAll("[data-buy-id]").forEach((button) => {
        button.addEventListener("click", () => {
            const holdingId = Number(button.dataset.buyId);
            const holding = state.holdings.find((item) => Number(item.id) === holdingId);
            state.tradeDraft = {
                holdingId,
                type: "BUY",
                ticker: holding?.ticker,
                assetType: holding?.type,
                currency: holding?.currency
            };
            state.activeNav = "Transactions";
            state.transactionMessage = "Create BUY transaction (Buy More).";
            render();
        });
    });

    document.querySelectorAll("[data-sell-id]").forEach((button) => {
        button.addEventListener("click", () => {
            const holdingId = Number(button.dataset.sellId);
            const holding = state.holdings.find((item) => Number(item.id) === holdingId);
            state.tradeDraft = {
                holdingId,
                type: "SELL",
                ticker: holding?.ticker,
                assetType: holding?.type,
                currency: holding?.currency
            };
            state.activeNav = "Transactions";
            state.transactionMessage = "Create SELL transaction.";
            render();
        });
    });

    bindAddAssetModalEvents();
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
    const fetchLatestButton = document.getElementById("fetch-latest-price");
    const priceInput = document.getElementById("asset-price");

    cancelButton?.addEventListener("click", () => closeAddAssetModal());

    modal.addEventListener("click", (event) => {
        if (event.target.id === "add-asset-modal") {
            closeAddAssetModal();
        }
    });

    fetchLatestButton?.addEventListener("click", async () => {
        const ticker = String(fetchLatestButton.dataset.ticker || "").trim().toUpperCase();
        if (!ticker) {
            error.textContent = "Ticker is required to fetch latest quote.";
            return;
        }

        error.textContent = "Fetching latest Yahoo quote...";
        try {
            const quote = await apiRequest(`/quotes/latest/${encodeURIComponent(ticker)}`);
            const fetched = Number(quote?.price);
            if (!(fetched > 0)) {
                throw new Error("Yahoo did not return a valid quote.");
            }
            if (priceInput) {
                priceInput.value = fetched.toFixed(4);
            }
            const source = String(quote?.source || "UNKNOWN");
            error.textContent = `Latest price loaded (${source}): ${formatCurrency(fetched)}. Click Update Price to confirm.`;
        } catch (fetchError) {
            error.textContent = fetchError.message;
        }
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!state.editingAssetId) {
            closeAddAssetModal();
            state.activeNav = "Transactions";
            state.transactionMessage = "Use BUY/SELL in Transaction Manager to change positions.";
            render();
            return;
        }

        const formData = new FormData(form);
        const price = Number(formData.get("price"));
        if (!(price > 0)) {
            error.textContent = "Current price must be greater than 0.";
            return;
        }

        try {
            const query = `currentPrice=${encodeURIComponent(price.toFixed(4))}`;
            await apiRequest(`/holding/${state.editingAssetId}/price?${query}`, {
                method: "PATCH"
            });

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

function bindModalKeyboardEscape() {
    if (escapeHandlerBound) {
        return;
    }

    document.addEventListener("click", (event) => {
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
