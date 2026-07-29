export const initialHoldings = [
    {
        id: "AAPL",
        ticker: "AAPL",
        name: "Apple Inc.",
        type: "Stock",
        quantity: 50,
        avgPrice: 150,
        currentPrice: 195.4,
        currency: "USD"
    },
    {
        id: "MSFT",
        ticker: "MSFT",
        name: "Microsoft Corporation",
        type: "Stock",
        quantity: 40,
        avgPrice: 280,
        currentPrice: 415.32,
        currency: "USD"
    },
    {
        id: "VBTLX",
        ticker: "VBTLX",
        name: "Vanguard Total Bond Market",
        type: "Bond",
        quantity: 80,
        avgPrice: 10.2,
        currentPrice: 10.75,
        currency: "USD"
    },
    {
        id: "CASH",
        ticker: "Cash",
        name: "USD",
        type: "Cash",
        quantity: 1,
        avgPrice: 12315.5,
        currentPrice: 12315.5,
        currency: "USD"
    }
];

export const initialTransactions = [
    {
        id: "txn-1",
        date: "Jun 12, 2024",
        type: "Stock",
        asset: "AAPL",
        action: "Buy",
        quantity: 10,
        price: 194.2,
        amount: -1942
    },
    {
        id: "txn-2",
        date: "Jun 10, 2024",
        type: "Stock",
        asset: "MSFT",
        action: "Buy",
        quantity: 5,
        price: 410.5,
        amount: -2052.5
    },
    {
        id: "txn-3",
        date: "Jun 5, 2024",
        type: "Bond",
        asset: "VBTLX",
        action: "Buy",
        quantity: 20,
        price: 10.68,
        amount: -213.6
    },
    {
        id: "txn-4",
        date: "May 28, 2024",
        type: "Stock",
        asset: "AAPL",
        action: "Sell",
        quantity: 4,
        price: 188.1,
        amount: 752.4
    }
];

export const allocationTarget = [
    { label: "Stocks", value: 68.2, color: "#3b82f6" },
    { label: "Bonds", value: 18.7, color: "#22c55e" },
    { label: "Cash", value: 9.8, color: "#f59e0b" },
    { label: "Other", value: 3.3, color: "#8b5cf6" }
];

export function generatePerformanceData(points = 365) {
    const start = new Date("2023-06-01T00:00:00");
    let value = 70000;
    const data = [];

    for (let i = 0; i < points; i += 1) {
        const date = new Date(start);
        date.setDate(start.getDate() + i);
        const trend = 130;
        const seasonal = Math.sin(i / 16) * 550;
        const noise = ((i * 97) % 13 - 6) * 65;
        value = Math.max(56000, value + trend + seasonal * 0.08 + noise);

        data.push({
            date,
            value: Number(value.toFixed(2))
        });
    }

    return data;
}
