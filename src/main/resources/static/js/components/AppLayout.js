export function AppLayout({ topNavbar, sidebar, header, summaryCards, charts, holdingsTable, recentTransactions, addAssetModal, confirmDeleteModal }) {
    return `
        <div class="dashboard-shell">
            ${topNavbar}
            <div class="dashboard-body">
                ${sidebar}
                <main class="dashboard-main">
                    <section id="section-header">${header}</section>
                    <section id="section-summary" class="summary-grid">${summaryCards}</section>
                    <section id="section-performance" class="charts-grid">${charts}</section>
                    <section id="section-holdings">${holdingsTable}</section>
                    <section id="section-transactions">${recentTransactions}</section>
                </main>
            </div>
        </div>
        ${addAssetModal}
        ${confirmDeleteModal}
    `;
}
