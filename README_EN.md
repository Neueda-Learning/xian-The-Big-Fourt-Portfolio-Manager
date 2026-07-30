# Investment Portfolio Manager

## 1. Project Introduction and Development Background

This project is a multi-asset investment portfolio management system built on **Spring Boot 4**. It delivers a complete REST API backend and a vanilla-JavaScript single-page dashboard, providing individual investors and small teams with a unified interface for position tracking, performance analysis, and AI-powered financial Q&A via Zhipu GLM large language models.

### Background and Pain Points Addressed

Traditional personal investment management typically relies on spreadsheets, which introduce a range of recurring problems:

- **Fragmented data and version inconsistency**: Holdings, trades, and price data are scattered across separate files, making it difficult to build a consolidated asset view.
- **Drift between records and reality**: BUY/SELL operations lack enforcement against cash balance and position quantity, causing silent data corruption when maintained manually.
- **No transaction immutability**: Without a protection mechanism, historical trade records can be accidentally modified or deleted, breaking the audit trail.
- **Manual performance calculation**: Metrics such as total market value, return rate, and asset allocation must be computed by hand, which is error-prone and hard to reproduce.
- **Weak market data management**: The absence of a unified historical-price and real-time-quote data source undermines the reliability of performance calculations.
- **AI suggestions conflated with live operations**: Users want AI-assisted financial education but need a strict guarantee that the AI never mutates positions, transactions, or any database state.

This system addresses all of the above through a structured relational data model, strict service-layer business rules (cash deduction on BUY, cash addition on SELL, immutable transactions), and a daily snapshot mechanism — improving the accuracy, traceability, and analytical power of investment data.

---

## 2. Core System Features

### 2.1 Portfolio Management

- Create, query, update, and delete investment portfolios.
- Each portfolio maintains its name, description, initial cash amount, and current cash balance.
- A dedicated summary endpoint aggregates total asset value, total gain/loss, cash balance, and daily price change across all holdings.

### 2.2 Holdings Management

- Query holdings by portfolio; view individual holding details.
- Records asset type (`STOCK` / `BOND` / `CASH`), ticker symbol, quantity, weighted-average cost price, current market price, purchase date, and currency.
- **Position quantity changes are only permitted through the trade endpoints** — direct holding edits and deletes are disabled, preserving data integrity.
- Supports a dedicated endpoint for refreshing the current price of a single holding (used for manual quote updates).

### 2.3 Transaction Management

- Create BUY and SELL transaction records through dedicated trade endpoints.
- **Transactions are immutable once created** — update and delete operations are explicitly rejected, preserving the full audit history.
- BUY orders automatically validate sufficient cash balance; SELL orders automatically validate available position quantity; both checks reject the request if the conditions are not met.
- A BUY decreases and a SELL increases the portfolio cash balance; holdings quantity and average cost price are recalculated after every trade.
- A dedicated cash deposit endpoint allows adding cash to a portfolio.
- Transactions can be queried by portfolio ID or by holding ID.

### 2.4 Performance Analysis

- Calculates total market value, total cost basis, total gain/loss (absolute and percentage) at the portfolio level.
- Returns per-holding breakdown including market value, cost, current price, and quantity.
- Current price resolution order: local price history → external Yahoo Finance API → holding cost price as fallback.

### 2.5 Portfolio Daily Snapshot

- Captures an on-demand snapshot of the current day's portfolio state, recording total value, cash balance, and holdings value.
- Historical snapshots can be queried by date range, providing the time-series data source for frontend performance charts.
- A unique constraint on `(portfolio_id, snapshot_date)` ensures only one snapshot per portfolio per day is retained.

### 2.6 Price History Management

- Look up closing prices by ticker and exact date, or query a range of dates in bulk.
- Supports manual insertion and deletion of price records.
- Price history data powers holdings valuation and performance trend calculations.

### 2.7 Real-Time Quote Integration

- Fetches live security quotes via an external Yahoo Finance API proxy.
- Automatically falls back to the most recent locally stored price when the external service is unavailable.

### 2.8 AI Financial Q&A

- Integrates Zhipu AI (GLM series models) to provide educational financial knowledge Q&A.
- Frontend model selection is supported (`glm-4.7-flash`, `glm-4.7`, `glm-5.2`); the available model list is governed by backend configuration.
- **The AI is strictly limited to knowledge Q&A and cannot execute any trades, holding changes, or database mutations** — enforced by a built-in system prompt constraint.

---

## 3. Database Table Descriptions

The database engine is **MySQL**. Table definitions are located in `src/main/resources/schema.sql`; seed data is in `src/main/resources/data.sql`.

### 3.1 `portfolio` — Portfolio Master Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT AUTO_INCREMENT | Primary key |
| `pro_name` | VARCHAR(100) | Portfolio name |
| `pro_description` | VARCHAR(500) | Portfolio description |
| `initial_cash` | DECIMAL(18,4) | Initial cash amount funded into the portfolio |
| `cash_balance` | DECIMAL(18,4) | Current available cash balance |
| `create_at` | TIMESTAMP | Record creation timestamp |
| `update_at` | TIMESTAMP | Last update timestamp |

**Purpose**: Defines the core identity and cash baseline for each investment portfolio. This is the root entity; all holdings, transactions, and snapshots reference it via foreign key with `ON DELETE CASCADE`.

---

### 3.2 `holding` — Holdings Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT AUTO_INCREMENT | Primary key |
| `portfolio_id` | INT | Owning portfolio ID (FK, cascade delete) |
| `asset_type` | VARCHAR(10) | Asset class (`STOCK` / `BOND` / `CASH`) |
| `ticker` | VARCHAR(20) | Ticker symbol (nullable for cash positions) |
| `quantity` | DECIMAL(18,4) | Current position quantity |
| `average_price` | DECIMAL(18,4) | Weighted-average cost price |
| `current_price` | DECIMAL(18,4) | Current market price |
| `purchase_date` | DATE | Initial purchase date |
| `currency` | VARCHAR(3) | Currency code, default `USD` |
| `create_at` | TIMESTAMP | Record creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |

**Purpose**: Represents the current position and pricing state of each asset held within a portfolio. Quantity and average cost are recalculated automatically by the transaction service layer after every BUY or SELL.

---

### 3.3 `transaction` — Transaction Ledger Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT AUTO_INCREMENT | Primary key |
| `portfolio_id` | INT | Owning portfolio ID (FK, cascade delete) |
| `holding_id` | INT | Associated holding ID (FK, nullable, cascade delete) |
| `type` | VARCHAR(4) | Transaction type (`BUY` / `SELL`) |
| `quantity` | DECIMAL(18,4) | Number of units traded |
| `price` | DECIMAL(18,4) | Execution price per unit |
| `trade_date` | TIMESTAMP | Trade execution timestamp |

**Purpose**: Provides an immutable, append-only record of every trade. This table is the sole authoritative source for position quantity changes. No UPDATE or DELETE operations are permitted, ensuring a complete and tamper-proof audit chain.

---

### 3.4 `portfolio_snapshot` — Daily Portfolio Snapshot Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT AUTO_INCREMENT | Primary key |
| `portfolio_id` | INT | Owning portfolio ID (FK, cascade delete) |
| `snapshot_date` | DATE | Date of the snapshot |
| `total_value` | DECIMAL(18,4) | Total portfolio value on the snapshot date |
| `cash_balance` | DECIMAL(18,4) | Cash balance on the snapshot date |
| `holdings_value` | DECIMAL(18,4) | Total holdings market value on the snapshot date |
| `created_at` | TIMESTAMP | Record creation timestamp |

**Constraint**: `(portfolio_id, snapshot_date)` is unique — at most one snapshot per portfolio per day.

**Purpose**: Stores daily cross-sections of portfolio value, enabling the frontend performance chart to render a time-series view of growth, supporting day-over-day comparisons and long-term trend analysis.

---

### 3.5 `price_history` — Security Price History Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT AUTO_INCREMENT | Primary key |
| `ticker` | VARCHAR(20) | Ticker symbol |
| `price_date` | DATE | Date of the price record |
| `close_price` | DECIMAL(18,4) | Closing price on that date |
| `create_at` | TIMESTAMP | Record creation timestamp |

**Purpose**: Stores the historical closing price series for each security. This data powers holdings valuation, performance calculations, and chart rendering. It also serves as the local fallback price source when the external real-time quote service is unavailable.

---

## Technology Stack

| Layer | Technology |
|-------|------------|
| Backend Framework | Spring Boot 4.1 |
| Data Access | Spring JDBC / JdbcTemplate |
| Database | MySQL 8 (production) / H2 (test) |
| Frontend | Vanilla JavaScript (ES Modules) + Chart.js |
| AI Service | Zhipu AI (GLM series models) |
| Build Tool | Maven |
| Runtime | Java 17+ |

## Quick Start

**Prerequisites**: MySQL must be installed and running locally. The database `portfolio_db` is created automatically via `createDatabaseIfNotExist=true` in the connection URL.

```powershell
# Build and run (default port 9005)
.\mvnw.cmd spring-boot:run
```

Open in browser: [http://localhost:9005](http://localhost:9005)

**Configure AI features** (optional): Add the following to the Environment Variables in your IntelliJ IDEA Run Configuration:

```
ZHIPU_API_KEY=your_zhipu_api_key
```

## Running Tests

```powershell
.\mvnw.cmd clean test
```

