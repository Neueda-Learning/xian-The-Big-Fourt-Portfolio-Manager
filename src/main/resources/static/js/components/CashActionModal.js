import { toInputSafeText } from "../utils/formatters.js";

export function CashActionModal(mode, cashHolding, cashBalance) {
    if (!mode || !cashHolding) {
        return "";
    }

    const title = mode === "remove" ? "Remove Cash" : "Add Cash";
    const buttonLabel = mode === "remove" ? "Remove" : "Add";
    const balance = Number(cashBalance || 0).toFixed(4);

    return `
        <div class="modal-backdrop" id="cash-action-modal" role="dialog" aria-modal="true" aria-labelledby="cash-action-title">
            <div class="modal" role="document">
                <h2 id="cash-action-title">${title}</h2>
                <p>Current cash balance: <strong>${toInputSafeText(balance)}</strong></p>
                <form id="cash-action-form" novalidate>
                    <label for="cash-action-amount">Amount</label>
                    <input id="cash-action-amount" name="amount" type="number" min="0.0001" step="0.0001" required>

                    <p class="form-error" id="cash-action-error" aria-live="polite"></p>

                    <div class="modal-actions">
                        <button class="secondary-btn" type="button" id="cancel-cash-action">Cancel</button>
                        <button class="primary-btn" type="submit">${buttonLabel}</button>
                    </div>
                </form>
            </div>
        </div>
    `;
}
