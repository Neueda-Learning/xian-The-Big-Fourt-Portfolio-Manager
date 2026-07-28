-- ============================================================================
-- YAHOO FINANCE DATA STORAGE - SQL RECOMMENDATIONS
-- ============================================================================
--
-- NOTE: Your current database ALREADY HAS price_history table
-- These scripts show recommended enhancements
-- Execute only what you need based on your requirements
--
-- ============================================================================

-- ============================================================================
-- OPTION 1: MINIMAL ENHANCEMENT (Recommended as first step)
-- Add data integrity and audit columns to existing price_history table
-- ============================================================================

-- This ensures no duplicate prices for same ticker/date combination
ALTER TABLE price_history
ADD CONSTRAINT uk_ticker_date UNIQUE (ticker, price_date),
ADD INDEX idx_ticker (ticker),
ADD INDEX idx_price_date (price_date);

-- Add fetched_at column to track when data was retrieved from Yahoo
ALTER TABLE price_history
ADD COLUMN fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER create_at;

-- Add data_source column to track which API provided the data (useful for multi-source)
ALTER TABLE price_history
ADD COLUMN data_source VARCHAR(20) DEFAULT 'YAHOO' AFTER fetched_at;

-- Result: price_history table schema after these changes:
-- CREATE TABLE price_history (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     ticker VARCHAR(20) NOT NULL,
--     price_date DATE NOT NULL,
--     close_price DECIMAL(18,4) NOT NULL,
--     create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     data_source VARCHAR(20) DEFAULT 'YAHOO',
--     UNIQUE KEY uk_ticker_date (ticker, price_date),
--     KEY idx_ticker (ticker),
--     KEY idx_price_date (price_date)
-- );


-- ============================================================================
-- OPTION 2: ENHANCED PRICE DATA (For OHLC analysis)
-- Add Open, High, Low, Volume, Adjusted Close columns
-- This enables technical analysis and more accurate return calculations
-- ============================================================================

ALTER TABLE price_history
ADD COLUMN (
    open_price DECIMAL(18,4) AFTER close_price,
    high_price DECIMAL(18,4) AFTER open_price,
    low_price DECIMAL(18,4) AFTER high_price,
    adjusted_close DECIMAL(18,4) AFTER low_price,
    volume BIGINT AFTER adjusted_close
);

-- IMPORTANT: Update your entity class priceHistory.java to include these fields


-- ============================================================================
-- OPTION 3: NEW TABLE - dividend_history
-- Track dividend payments for accurate portfolio return calculations
-- ============================================================================

CREATE TABLE IF NOT EXISTS dividend_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    ex_dividend_date DATE NOT NULL,
    payment_date DATE,
    dividend_per_share DECIMAL(18,4) NOT NULL,
    dividend_type VARCHAR(50) COMMENT 'e.g., Regular Cash, Special Cash',
    record_date DATE COMMENT 'Date of record for dividend',
    announcement_date DATE COMMENT 'Date dividend was announced',
    data_source VARCHAR(20) DEFAULT 'YAHOO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_ticker_ex_date (ticker, ex_dividend_date),
    KEY idx_ticker (ticker),
    KEY idx_ex_dividend_date (ex_dividend_date),
    KEY idx_payment_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ============================================================================
-- OPTION 4: NEW TABLE - stock_split_history
-- Track stock splits for adjusting historical prices and holdings
-- ============================================================================

CREATE TABLE IF NOT EXISTS stock_split_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    split_date DATE NOT NULL,
    split_ratio VARCHAR(20) NOT NULL COMMENT 'e.g., 2:1, 10:3',
    numerator INT COMMENT 'e.g., 2 for 2:1 split',
    denominator INT COMMENT 'e.g., 1 for 2:1 split',
    data_source VARCHAR(20) DEFAULT 'YAHOO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_ticker_date (ticker, split_date),
    KEY idx_ticker (ticker),
    KEY idx_split_date (split_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ============================================================================
-- OPTION 5: NEW TABLE - market_snapshot
-- Track daily portfolio snapshots for performance analysis
-- Links to existing portfolio table to store historical portfolio values
-- ============================================================================

CREATE TABLE IF NOT EXISTS market_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    snapshot_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Market values
    total_market_value DECIMAL(18,4) COMMENT 'Total current value of all holdings',
    total_cost_basis DECIMAL(18,4) COMMENT 'Total purchase cost of all holdings',
    total_return DECIMAL(18,4) COMMENT 'Total gain/loss in currency',
    return_percentage DECIMAL(8,4) COMMENT 'Return as percentage',

    -- Metadata
    currency VARCHAR(3) DEFAULT 'USD',
    data_source VARCHAR(20) DEFAULT 'YAHOO_REALTIME',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE,
    UNIQUE KEY uk_portfolio_snapshot (portfolio_id, snapshot_date),
    KEY idx_portfolio_id (portfolio_id),
    KEY idx_snapshot_date (snapshot_date),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ============================================================================
-- OPTION 6: NEW TABLE - ticker_master
-- Metadata about tickers (company info, sector, exchange, etc.)
-- Useful for categorization and filtering
-- ============================================================================

CREATE TABLE IF NOT EXISTS ticker_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL UNIQUE,

    -- Company Info
    company_name VARCHAR(255),
    sector VARCHAR(100),
    industry VARCHAR(100),
    country VARCHAR(50) DEFAULT 'US',

    -- Trading Info
    exchange VARCHAR(20) COMMENT 'e.g., NASDAQ, NYSE',
    currency VARCHAR(3) DEFAULT 'USD',
    shares_outstanding BIGINT,

    -- Financial Info
    market_cap DECIMAL(18,2),
    dividend_yield DECIMAL(8,4),
    pe_ratio DECIMAL(10,2),

    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    last_price DECIMAL(18,4),
    last_price_update TIMESTAMP,

    -- Metadata
    data_source VARCHAR(20) DEFAULT 'YAHOO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_ticker (ticker),
    KEY idx_sector (sector),
    KEY idx_exchange (exchange),
    KEY idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ============================================================================
-- VIEWS FOR EASY QUERYING
-- ============================================================================

-- View: Latest prices for all tickers
CREATE OR REPLACE VIEW v_latest_prices AS
SELECT
    ticker,
    MAX(price_date) as latest_date,
    (SELECT close_price FROM price_history p2
     WHERE p2.ticker = p1.ticker
     ORDER BY price_date DESC
     LIMIT 1) as latest_close_price
FROM price_history p1
GROUP BY ticker;


-- View: Price change (yesterday vs today)
CREATE OR REPLACE VIEW v_price_changes AS
SELECT
    ticker,
    price_date,
    close_price,
    LAG(close_price) OVER (PARTITION BY ticker ORDER BY price_date) as prev_close,
    close_price - LAG(close_price) OVER (PARTITION BY ticker ORDER BY price_date) as price_change,
    ROUND(((close_price - LAG(close_price) OVER (PARTITION BY ticker ORDER BY price_date))
        / LAG(close_price) OVER (PARTITION BY ticker ORDER BY price_date)) * 100, 4) as change_percent
FROM price_history
ORDER BY price_date DESC;


-- View: Holdings with latest prices
CREATE OR REPLACE VIEW v_holdings_with_prices AS
SELECT
    h.id,
    h.portfolio_id,
    h.ticker,
    h.asset_type,
    h.quantity,
    h.purchase_price,
    h.purchase_date,
    (SELECT close_price FROM price_history p
     WHERE p.ticker = h.ticker
     ORDER BY price_date DESC
     LIMIT 1) as current_price,
    h.quantity * (SELECT close_price FROM price_history p
     WHERE p.ticker = h.ticker
     ORDER BY price_date DESC
     LIMIT 1) as current_value,
    h.quantity * COALESCE(h.purchase_price, 0) as cost_basis
FROM holding h;


-- ============================================================================
-- STORED PROCEDURES FOR COMMON OPERATIONS
-- ============================================================================

-- Procedure: Insert price and avoid duplicates
DELIMITER $$

CREATE PROCEDURE sp_insert_price_if_not_exists(
    IN p_ticker VARCHAR(20),
    IN p_date DATE,
    IN p_close DECIMAL(18,4),
    IN p_open DECIMAL(18,4),
    IN p_high DECIMAL(18,4),
    IN p_low DECIMAL(18,4),
    IN p_volume BIGINT
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = p_ticker AND price_date = p_date) THEN
        INSERT INTO price_history (
            ticker, price_date, close_price, open_price, high_price, low_price, volume, data_source, fetched_at
        ) VALUES (
            p_ticker, p_date, p_close, p_open, p_high, p_low, p_volume, 'YAHOO', NOW()
        );
    END IF;
END$$

DELIMITER ;


-- ============================================================================
-- RECOMMENDATIONS SUMMARY
-- ============================================================================
--
-- YOUR CURRENT SETUP:
-- ✅ price_history table EXISTS and works
-- ✅ Can store close prices
-- ✅ Basic CRUD operations work
--
-- CURRENT LIMITATIONS:
-- ❌ No unique constraint (duplicates possible)
-- ❌ No fetched_at timestamp
-- ❌ No OHLC data (Open, High, Low)
-- ❌ No volume tracking
-- ❌ No adjusted close prices
-- ❌ No dividend tracking
-- ❌ No stock split tracking
--
-- RECOMMENDED IMPLEMENTATION:
-- 1. START: Apply OPTION 1 (Minimal Enhancement) - adds constraints and audit columns
-- 2. LATER: Apply OPTION 2 (Enhanced Price Data) - adds OHLC support
-- 3. FUTURE: Apply OPTION 3-6 (New Tables) - adds dividend, split, and snapshot tracking
--
-- DATABASE IS COMPATIBLE: YES ✅
-- Current schema can store Yahoo historical prices without modification
-- Enhancements only needed if you want advanced analytics capabilities
--
-- ============================================================================

