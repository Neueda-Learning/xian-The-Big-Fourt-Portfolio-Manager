export function AddAssetModal(isOpen, draftAsset = null) {
    if (!isOpen) {
        return "";
    }

    const isEdit = Boolean(draftAsset);
    const ticker = draftAsset?.ticker || "";
    const currentPrice = draftAsset?.currentPrice || "";

    return `
        <div class="modal-backdrop" id="add-asset-modal" role="dialog" aria-modal="true" aria-labelledby="add-asset-title">
            <div class="modal" role="document">
                <h2 id="add-asset-title">${isEdit ? "Update Current Price" : "New Position"}</h2>
                <form id="add-asset-form" novalidate>
                    ${
                        isEdit
                            ? `
                    <label for="asset-ticker">Asset ticker</label>
                    <input id="asset-ticker" name="ticker" type="text" readonly value="${ticker}">

                    <label for="asset-price">Current price</label>
                    <input id="asset-price" name="price" type="number" min="0.0001" step="0.0001" required value="${currentPrice}">
                    `
                            : `
                    <p>This action moved to Transaction Manager. Use BUY/SELL transactions to change quantity and cash.</p>
                    `
                    }

                    <p class="form-error" id="add-asset-error" aria-live="polite"></p>

                    <div class="modal-actions">
                        <button class="secondary-btn" type="button" id="cancel-add-asset">Cancel</button>
                        <button class="primary-btn" type="submit">${isEdit ? "Update Price" : "Go To Transactions"}</button>
                    </div>
                </form>
            </div>
        </div>
    `;
}
