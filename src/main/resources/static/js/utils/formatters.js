export function formatCurrency(value) {
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(value);
}

export function formatPercent(value, digits = 2) {
    return `${value >= 0 ? "+" : ""}${value.toFixed(digits)}%`;
}

export function formatSignedCurrency(value) {
    const formatted = formatCurrency(Math.abs(value));
    return `${value >= 0 ? "+" : "-"}${formatted}`;
}

export function compactDate(date) {
    return new Intl.DateTimeFormat("en-US", {
        month: "short",
        year: "numeric"
    }).format(date);
}

export function toInputSafeText(value) {
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
