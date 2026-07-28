# Yahoo Finance Data Crawling Analysis

## Current Status

### Is the code crawling data from Yahoo?
**Yes, but VERY LIMITED.**

The code currently fetches **only real-time current prices** from Yahoo Finance via an AWS Lambda endpoint (`YahooFinanceService.java`), but:
- Data is **NOT persisted** to the database
- Price is cached only in **memory** (ConcurrentHashMap)
- Cache is cleared when application restarts
- Only used for performance calculations, never stored for historical analysis

## Current Data Flow

```
Yahoo Finance API
    ↓
AWS Lambda (https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/...)
    ↓
YahooFinanceService.getCurrentPrice(ticker)
    ↓
Cached in Memory (ConcurrentHashMap)
    ↓
PerformanceService (used for real-time calculations only)
    ↓
Never saved to database
```

## Current Database Schema vs. Required Capacity

### `price_history` Table (Current)
```sql
CREATE TABLE price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    price_date DATE NOT NULL,
    close_price DECIMAL(18,4) NOT NULL,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Current Capacity:**
- ✅ Ticker
- ✅ Date
- ✅ Close Price
- ✅ Creation Timestamp

**Limitations:**
- ❌ No Open Price (OHLC data)
- ❌ No High Price
- ❌ No Low Price  
- ❌ No Volume data
- ❌ No Adjusted Close Price
- ❌ No Split Adjusted values
- ❌ No Dividend information
- ❌ No Data fetch timestamp (only creation time)

---

## Recommended Tables for Complete Yahoo Data Integration

### Option 1: Enhanced price_history Table (MINIMAL)
Add essential OHLC data:

```sql
ALTER TABLE price_history ADD COLUMN (
    open_price DECIMAL(18,4),
    high_price DECIMAL(18,4),
    low_price DECIMAL(18,4),
    volume BIGINT,
    adjusted_close DECIMAL(18,4),
    data_source VARCHAR(20) DEFAULT 'YAHOO'
);
```

### Option 2: New Comprehensive Tables (RECOMMENDED)

#### 1. Enhanced `price_history` Table
```sql
CREATE TABLE IF NOT EXISTS price_history_v2 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    price_date DATE NOT NULL,
    open_price DECIMAL(18,4),
    high_price DECIMAL(18,4),
    low_price DECIMAL(18,4),
    close_price DECIMAL(18,4) NOT NULL,
    adjusted_close DECIMAL(18,4),
    volume BIGINT,
    data_source VARCHAR(20) DEFAULT 'YAHOO',
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticker_date (ticker, price_date),
    INDEX idx_ticker (ticker),
    INDEX idx_date (price_date)
);
```

#### 2. New `dividend_history` Table
```sql
CREATE TABLE IF NOT EXISTS dividend_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    payment_date DATE,
    ex_dividend_date DATE,
    dividend_amount DECIMAL(18,4) NOT NULL,
    dividend_type VARCHAR(50),
    data_source VARCHAR(20) DEFAULT 'YAHOO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticker_date (ticker, ex_dividend_date),
    INDEX idx_ticker (ticker)
);
```

#### 3. New `stock_split_history` Table
```sql
CREATE TABLE IF NOT EXISTS stock_split_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    split_date DATE NOT NULL,
    split_ratio VARCHAR(20) NOT NULL,
    numerator INT,
    denominator INT,
    data_source VARCHAR(20) DEFAULT 'YAHOO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticker_date (ticker, split_date),
    INDEX idx_ticker (ticker)
);
```

#### 4. New `market_snapshot` Table (for portfolio snapshots)
```sql
CREATE TABLE IF NOT EXISTS market_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    snapshot_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_market_value DECIMAL(18,4),
    total_cost_basis DECIMAL(18,4),
    total_return DECIMAL(18,4),
    return_percentage DECIMAL(8,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE,
    UNIQUE KEY uk_portfolio_time (portfolio_id, snapshot_time),
    INDEX idx_portfolio_date (portfolio_id, snapshot_date)
);
```

#### 5. New `ticker_master` Table (metadata)
```sql
CREATE TABLE IF NOT EXISTS ticker_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL UNIQUE,
    company_name VARCHAR(255),
    sector VARCHAR(100),
    industry VARCHAR(100),
    country VARCHAR(50),
    exchange VARCHAR(20),
    currency VARCHAR(3),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    data_source VARCHAR(20) DEFAULT 'YAHOO',
    INDEX idx_ticker (ticker)
);
```

---

## Compatibility Analysis

### Current Implementation ✅
**Compatible with existing schema:** YES, but limited

The current `price_history` table can store historical price data, BUT:
- **Only close prices** are captured (the most essential field)
- Cannot track OHLC data for technical analysis
- Cannot track volume trends
- Cannot track dividend-adjusted returns
- Cannot store multiple data points per day

### Issues to Address ⚠️

1. **Data Persistence Gap**
   - Yahoo data is fetched but NOT saved to database
   - Real-time prices are cached in memory only
   - No historical price tracking
   - **Action:** Modify `PerformanceService` or `PriceHistoryService` to persist fetched prices

2. **Data Granularity**
   - Only daily close prices supported
   - No intraday data
   - No volume information
   - **Action:** Enhance schema if intraday/volume tracking needed

3. **Data Integrity**
   - No unique constraint on (ticker, price_date)
   - Duplicate entries possible
   - **Action:** Add UNIQUE KEY constraint

4. **Audit Trail**
   - No `fetched_at` timestamp (only `created_at`)
   - Can't track when data was retrieved
   - **Action:** Add separate `fetched_at` column

---

## Recommended Implementation Plan

### Phase 1: Immediate (Minimal Changes)
1. ✅ Add UNIQUE constraint to price_history
2. ✅ Add `fetched_at` column to track data retrieval time
3. ✅ Modify `PriceHistoryService` to auto-save fetched prices
4. ✅ Modify `PerformanceService` to persist prices before use

**SQL:**
```sql
ALTER TABLE price_history 
ADD UNIQUE KEY uk_ticker_date (ticker, price_date),
ADD COLUMN fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

### Phase 2: Enhanced (Recommended)
1. Create `price_history_v2` with OHLC support
2. Migrate data from `price_history` to `price_history_v2`
3. Update repository queries to use new table
4. Add OHLC data extraction from Yahoo API response

### Phase 3: Advanced (Future)
1. Create dividend_history table
2. Create stock_split_history table  
3. Create market_snapshot table for portfolio tracking
4. Add ticker_master for company metadata
5. Implement dividend adjustment calculations
6. Track split adjustments automatically

---

## Current Code Compatibility Status

| Component | Current Status | Compatible | Notes |
|-----------|---|---|---|
| YahooFinanceService | Fetches current price only | ⚠️ Limited | Only extracts close price, no OHLC data |
| priceHistory Entity | Only 5 fields | ⚠️ Limited | No open, high, low, volume fields |
| PriceHistoryRepository | Basic CRUD | ✅ Yes | Works with current schema |
| price_history Table | 5 columns | ⚠️ Limited | Missing essential fields for analysis |
| PerformanceService | Real-time only | ⚠️ Limited | Doesn't use historical price data |

---

## Recommendations Summary

### DO NOT NEED ❌
- Create entirely new database
- Drop existing tables
- Rewrite core business logic

### SHOULD ADD ✅
1. **Immediate:** Add UNIQUE constraint to prevent duplicates
2. **Immediate:** Add `fetched_at` timestamp for audit trail
3. **Important:** Enhance schema to include OHLC data
4. **Important:** Implement automatic price persistence in `PriceHistoryService`
5. **Nice-to-have:** Add dividend and split history tables

### Current Database is Compatible ✅
The existing `price_history` table **IS compatible** with Yahoo data, but it's too minimal for comprehensive portfolio analysis. It can store close prices, which is the most essential data point.

---

## Next Steps

1. **Verify Yahoo API Response Format**
   - Check what fields the AWS Lambda actually returns
   - Parse and extract OHLC data if available

2. **Decide on Enhancement Level**
   - Minimal: Just add UNIQUE constraint and audit columns
   - Recommended: Add OHLC columns to existing table
   - Advanced: Create comprehensive schema with dividend/split tracking

3. **Implement Price Persistence**
   - Current: Prices NOT saved to database
   - Suggested: Auto-save fetched prices to `price_history` table
   - Add `PriceHistoryService` to handle persistence

4. **Add Data Validation**
   - Prevent duplicate entries
   - Validate price ranges
   - Track data quality


