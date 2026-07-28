DROP TABLE IF EXISTS price_history;
DROP TABLE IF EXISTS `transaction`;
DROP TABLE IF EXISTS pro_transaction;
DROP TABLE IF EXISTS holding;
DROP TABLE IF EXISTS portfolio_snapshot;
DROP TABLE IF EXISTS portfolio;

CREATE TABLE portfolio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pro_name VARCHAR(100) NOT NULL,
    pro_description VARCHAR(500),
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE holding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    asset_type VARCHAR(10) NOT NULL,
    ticker VARCHAR(20),
    quantity DECIMAL(18,4) NOT NULL,
    purchase_price DECIMAL(18,4),
    purchase_date DATE,
    currency VARCHAR(3) DEFAULT 'USD',
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE
);

CREATE TABLE `transaction` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holding_id BIGINT NOT NULL,
    type VARCHAR(4) NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    price DECIMAL(18,4) NOT NULL,
    trade_date TIMESTAMP NOT NULL,
    FOREIGN KEY (holding_id) REFERENCES holding(id) ON DELETE CASCADE
);

CREATE TABLE price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    price_date DATE NOT NULL,
    price_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    open_price DECIMAL(18,4),
    high_price DECIMAL(18,4),
    low_price DECIMAL(18,4),
    close_price DECIMAL(18,4) NOT NULL,
    adjusted_close DECIMAL(18,4),
    volume BIGINT,
    currency VARCHAR(10),
    raw_payload TEXT,
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_price_history_ticker_time (ticker, price_time)
);
