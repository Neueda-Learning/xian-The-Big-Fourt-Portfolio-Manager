(function () {
    const toastEl = document.getElementById("toast");

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

    function renderTable(containerId, items) {
        const container = document.getElementById(containerId);
        if (!Array.isArray(items) || items.length === 0) {
            container.innerHTML = "<p>No data found.</p>";
            return;
        }
        const keys = Object.keys(items[0]);
        const header = keys.map((k) => `<th>${k}</th>`).join("");
        const rows = items
            .map((item) => `<tr>${keys.map((k) => `<td>${item[k] ?? ""}</td>`).join("")}</tr>`)
            .join("");
        container.innerHTML = `<table><thead><tr>${header}</tr></thead><tbody>${rows}</tbody></table>`;
    }

    function formatDateTimeLocal(value) {
        return value.replace("T", " ") + ":00";
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

    function bindPortfolio() {
        document.getElementById("portfolio-refresh").addEventListener("click", loadPortfolios);

        document.getElementById("portfolio-create-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = {
                name: document.getElementById("portfolio-name").value,
                description: document.getElementById("portfolio-description").value
            };
            const msg = await api("/saveportfolio", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
            await loadPortfolios();
        });

        document.getElementById("portfolio-find-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("portfolio-find-id").value;
            const data = await api(`/portfolio/${id}`);
            document.getElementById("portfolio-find-result").textContent = JSON.stringify(data, null, 2);
        });

        document.getElementById("portfolio-update-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("portfolio-update-id").value;
            const payload = {
                name: document.getElementById("portfolio-update-name").value,
                description: document.getElementById("portfolio-update-description").value
            };
            const msg = await api(`/portfolio/${id}`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
            await loadPortfolios();
        });

        document.getElementById("portfolio-delete-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("portfolio-delete-id").value;
            const msg = await api(`/delete/portfolio/${id}`, { method: "DELETE" });
            showToast(msg);
            await loadPortfolios();
        });
    }

    async function loadPortfolios() {
        try {
            const data = await api("/portfolios");
            renderTable("portfolio-list", data);
        } catch (e) {
            showToast(e.message);
        }
    }

    function bindHolding() {
        document.getElementById("holding-create-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = {
                portfolioId: Number(document.getElementById("holding-portfolio-id").value),
                assetType: document.getElementById("holding-asset-type").value,
                ticker: document.getElementById("holding-ticker").value,
                quantity: Number(document.getElementById("holding-quantity").value),
                purchasePrice: Number(document.getElementById("holding-purchase-price").value),
                purchasedata: document.getElementById("holding-purchase-date").value,
                currency: document.getElementById("holding-currency").value
            };
            const msg = await api("/saveholding", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
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
            renderTable("holding-list", data);
        });

        document.getElementById("holding-update-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("holding-update-id").value;
            const payload = {
                portfolioId: Number(document.getElementById("holding-update-portfolio-id").value),
                assetType: document.getElementById("holding-update-asset-type").value,
                ticker: document.getElementById("holding-update-ticker").value,
                quantity: Number(document.getElementById("holding-update-quantity").value),
                purchasePrice: Number(document.getElementById("holding-update-purchase-price").value),
                purchasedata: document.getElementById("holding-update-purchase-date").value,
                currency: document.getElementById("holding-update-currency").value
            };
            const msg = await api(`/holding/${id}`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
        });

        document.getElementById("holding-delete-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("holding-delete-id").value;
            const msg = await api(`/delete/holding/${id}`, { method: "DELETE" });
            showToast(msg);
        });
    }

    function bindTransaction() {
        document.getElementById("transaction-create-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = {
                holdingId: Number(document.getElementById("transaction-holding-id").value),
                type: document.getElementById("transaction-type").value,
                quantity: Number(document.getElementById("transaction-quantity").value),
                price: Number(document.getElementById("transaction-price").value),
                tradeDate: formatDateTimeLocal(document.getElementById("transaction-trade-date").value)
            };
            const msg = await api("/savetransaction", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
        });

        document.getElementById("transaction-find-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("transaction-find-id").value;
            const data = await api(`/transaction/${id}`);
            document.getElementById("transaction-find-result").textContent = JSON.stringify(data, null, 2);
        });

        document.getElementById("transaction-by-holding-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const holdingId = document.getElementById("transaction-list-holding-id").value;
            const data = await api(`/transactions/holding/${holdingId}`);
            renderTable("transaction-list", data);
        });

        document.getElementById("transaction-update-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("transaction-update-id").value;
            const payload = {
                holdingId: Number(document.getElementById("transaction-update-holding-id").value),
                type: document.getElementById("transaction-update-type").value,
                quantity: Number(document.getElementById("transaction-update-quantity").value),
                price: Number(document.getElementById("transaction-update-price").value),
                tradeDate: formatDateTimeLocal(document.getElementById("transaction-update-trade-date").value)
            };
            const msg = await api(`/transaction/${id}`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
        });

        document.getElementById("transaction-delete-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("transaction-delete-id").value;
            const msg = await api(`/delete/transaction/${id}`, { method: "DELETE" });
            showToast(msg);
        });
    }

    function bindPrice() {
        document.getElementById("price-create-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = {
                ticker: document.getElementById("price-ticker").value,
                priceDate: document.getElementById("price-date").value,
                closeprice: Number(document.getElementById("price-close").value)
            };
            const msg = await api("/saveprice", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showToast(msg);
        });

        document.getElementById("price-find-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const ticker = document.getElementById("price-find-ticker").value;
            const date = document.getElementById("price-find-date").value;
            const data = await api(`/price/${encodeURIComponent(ticker)}/${date}`);
            document.getElementById("price-find-result").textContent = JSON.stringify(data, null, 2);
        });

        document.getElementById("price-range-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const ticker = document.getElementById("price-range-ticker").value;
            const startDate = document.getElementById("price-range-start").value;
            const endDate = document.getElementById("price-range-end").value;
            const data = await api(`/prices/${encodeURIComponent(ticker)}?startDate=${startDate}&endDate=${endDate}`);
            renderTable("price-list", data);
        });

        document.getElementById("price-delete-form").addEventListener("submit", async (e) => {
            e.preventDefault();
            const ticker = document.getElementById("price-delete-ticker").value;
            const date = document.getElementById("price-delete-date").value;
            const msg = await api(`/delete/price?ticker=${encodeURIComponent(ticker)}&date=${date}`, { method: "DELETE" });
            showToast(msg);
        });
    }

    function bindYahooSync() {
        document.getElementById("yahoo-sync-all-btn").addEventListener("click", async () => {
            const data = await api("/yahoo/sync/all", { method: "POST" });
            document.getElementById("yahoo-sync-all-result").textContent = JSON.stringify(data, null, 2);
            const savedRows = await api("/prices/all");
            renderTable("price-list", savedRows);
            showToast(`Yahoo sync saved ${data.savedCount}/${data.tickerCount}`);
        });
    }

    function bindGlobalErrorHandler() {
        window.addEventListener("unhandledrejection", (event) => {
            showToast(event.reason?.message || "Request failed");
        });
    }

    bindTabs();
    bindPortfolio();
    bindHolding();
    bindTransaction();
    bindPrice();
    bindYahooSync();
    bindGlobalErrorHandler();
    loadPortfolios();
})();
