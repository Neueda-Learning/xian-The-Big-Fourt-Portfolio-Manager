import { formatCurrency, formatSignedCurrency, toInputSafeText } from "../utils/formatters.js";
import { icons } from "./icons.js";

function badgeClass(type) {
    return `badge ${type.toLowerCase()}`;
}

function gainForAsset(holding) {
    const marketValue = holding.quantity * holding.currentPrice;
    const costValue = holding.quantity * holding.avgPrice;
    const realizedPnl = Number(holding.realizedPnl ?? 0);
    const unrealizedPnl = Number(holding.unrealizedPnl ?? (marketValue - costValue));
    const totalPnl = Number(holding.totalPnl ?? (realizedPnl + unrealizedPnl));
    return { marketValue, realizedPnl, unrealizedPnl, totalPnl };
}

export function HoldingsTable(holdings, searchTerm, selectedType) {
    const filtered = holdings.filter((holding) => {
        const matchesSearch = `${holding.ticker} ${holding.name}`
            .toLowerCase()
            .includes(searchTerm.toLowerCase());
        const matchesType = selectedType === "All Types" || holding.type === selectedType;
        return matchesSearch && matchesType;
    });

    return `
        <article class="card table-card">
            <div class="card-heading-row holdings-header-row">
                <h2>Holdings</h2>
                <div class="table-controls">
                    <label class="search-wrap">
                        <span class="search-icon">${icons.search}</span>
                        <input id="holding-search" value="${toInputSafeText(searchTerm)}" type="search" placeholder="Search holdings..." aria-label="Search holdings">
                    </label>
                    <label>
                        <select id="holding-type-filter" aria-label="Filter asset type">
                            ${["All Types", "Stock", "Bond", "Cash"]
                                .map(
                                    (type) => `<option value="${type}" ${type === selectedType ? "selected" : ""}>${type}</option>`
                                )
                                .join("")}
                        </select>
                    </label>
                </div>
            </div>
            <div class="table-scroller">
                <table>
                    <thead>
                        <tr>
                            <th>Asset</th>
                            <th>Type</th>
                            <th>Quantity</th>
                            <th>Avg. Price</th>
                            <th>Current Price</th>
                            <th>Market Value</th>
                            <th>Realized P/L</th>
                            <th>Unrealized P/L</th>
                            <th>Total P/L</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${filtered
                            .map((holding) => {
                                const { marketValue, realizedPnl, unrealizedPnl, totalPnl } = gainForAsset(holding);
                                const realizedPositive = realizedPnl >= 0;
                                const unrealizedPositive = unrealizedPnl >= 0;
                                const totalPositive = totalPnl >= 0;
                                const cashRow = holding.type === "Cash";
                                return `
                                <tr>
                                    <td>
                                        <div class="asset-cell">
                                            <span class="asset-avatar ${holding.type.toLowerCase()}">${holding.ticker.slice(0, 1)}</span>
                                            <div>
                                                <strong>${holding.ticker}</strong>
                                                <p>${holding.name}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td><span class="${badgeClass(holding.type)}">${holding.type}</span></td>
                                    <td>${holding.quantity}</td>
                                    <td>${formatCurrency(holding.avgPrice)}</td>
                                    <td>${formatCurrency(holding.currentPrice)}</td>
                                    <td>${formatCurrency(marketValue)}</td>
                                    <td class="${realizedPositive ? "positive" : "negative"}">${formatSignedCurrency(realizedPnl)}</td>
                                    <td class="${unrealizedPositive ? "positive" : "negative"}">${formatSignedCurrency(unrealizedPnl)}</td>
                                    <td class="${totalPositive ? "positive" : "negative"}">${formatSignedCurrency(totalPnl)}</td>
                                    <td>
                                        <div class="row-actions">
                                            ${cashRow ? "<span>-</span>" : `
                                                <button class="icon-btn small" aria-label="Buy more ${holding.ticker}" data-buy-id="${holding.id}" title="Buy More">${icons.plus}</button>
                                                <button class="icon-btn small" aria-label="Sell ${holding.ticker}" data-sell-id="${holding.id}" title="Sell">${icons.transactions}</button>
                                                <button class="icon-btn small" aria-label="Update price for ${holding.ticker}" data-edit-id="${holding.id}">${icons.edit}</button>
                                            `}
                                        </div>
                                    </td>
                                </tr>
                            `;
                            })
                            .join("")}
                    </tbody>
                </table>
            </div>
        </article>
    `;
}
