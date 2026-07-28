export function ConfirmDeleteModal(targetHolding) {
    if (!targetHolding) {
        return "";
    }

    return `
        <div class="modal-backdrop" id="confirm-delete-modal" role="dialog" aria-modal="true" aria-labelledby="confirm-delete-title">
            <div class="modal" role="document">
                <h2 id="confirm-delete-title">Delete Asset</h2>
                <p>Are you sure you want to delete <strong>${targetHolding.ticker}</strong> from holdings?</p>
                <div class="modal-actions">
                    <button class="secondary-btn" type="button" id="cancel-delete-asset">Cancel</button>
                    <button class="danger-btn" type="button" id="confirm-delete-asset">Delete</button>
                </div>
            </div>
        </div>
    `;
}
