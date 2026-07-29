export function AddAssetModal(isOpen, draftAsset = null) {
    if (!isOpen) {
        return "";
    }

    const isEdit = Boolean(draftAsset);
    const ticker = draftAsset?.ticker || "";
    const name = draftAsset?.name || "";
    const type = draftAsset?.type || "Stock";
    const quantity = draftAsset?.quantity || "";
    const price = draftAsset?.avgPrice || "";

    return `
        <div class="modal-backdrop" id="add-asset-modal" role="dialog" aria-modal="true" aria-labelledby="add-asset-title">
            <div class="modal" role="document">
                <h2 id="add-asset-title">${isEdit ? "Edit Asset" : "Add Asset"}</h2>
                <form id="add-asset-form" novalidate>
                    <label for="asset-ticker">Asset ticker</label>
                    <input id="asset-ticker" name="ticker" type="text" required maxlength="10" value="${ticker}">

                    <label for="asset-name">Asset name</label>
                    <input id="asset-name" name="name" type="text" required maxlength="80" value="${name}">

                    <label for="asset-type">Asset type</label>
                    <select id="asset-type" name="type" required>
                        <option value="Stock" ${type === "Stock" ? "selected" : ""}>Stock</option>
                        <option value="Bond" ${type === "Bond" ? "selected" : ""}>Bond</option>
                        <option value="Cash" ${type === "Cash" ? "selected" : ""}>Cash</option>
                    </select>

                    <label for="asset-quantity">Quantity</label>
                    <input id="asset-quantity" name="quantity" type="number" min="0.0001" step="0.0001" required value="${quantity}">

                    <label for="asset-price">Purchase price</label>
                    <input id="asset-price" name="price" type="number" min="0" step="0.01" required value="${price}">

                    <p class="form-error" id="add-asset-error" aria-live="polite"></p>

                    <div class="modal-actions">
                        <button class="secondary-btn" type="button" id="cancel-add-asset">Cancel</button>
                        <button class="primary-btn" type="submit">${isEdit ? "Save Changes" : "Add Asset"}</button>
                    </div>
                </form>
            </div>
        </div>
    `;
}
