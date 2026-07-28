import { formatCurrency, formatSignedCurrency } from "../utils/formatters.js";

function transactionBadge(action) {
    return `badge ${action.toLowerCase()}`;
}

export function RecentTransactions(transactions) {
    return `
        <article class="card table-card">
            <div class="card-heading-row">
                <h2>Recent Transactions</h2>
                <button type="button" class="text-action">View All</button>
            </div>
            <div class="table-scroller">
                <table>
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Type</th>
                            <th>Asset</th>
                            <th>Action</th>
                            <th>Quantity</th>
                            <th>Price</th>
                            <th>Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${transactions
                            .slice(0, 6)
                            .map(
                                (transaction) => `
                            <tr>
                                <td>${transaction.date}</td>
                                <td><span class="badge ${transaction.type.toLowerCase()}">${transaction.type}</span></td>
                                <td>${transaction.asset}</td>
                                <td><span class="${transactionBadge(transaction.action)}">${transaction.action}</span></td>
                                <td>${transaction.quantity}</td>
                                <td>${formatCurrency(transaction.price)}</td>
                                <td class="${transaction.amount >= 0 ? "positive" : "negative"}">${formatSignedCurrency(transaction.amount)}</td>
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
