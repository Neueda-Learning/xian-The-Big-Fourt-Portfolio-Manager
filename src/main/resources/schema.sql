SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `transaction`;
DROP TABLE IF EXISTS portfolio_snapshot;
DROP TABLE IF EXISTS holding;
DROP TABLE IF EXISTS price_history;
DROP TABLE IF EXISTS portfolio;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS portfolio (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pro_name VARCHAR(100) NOT NULL,
    pro_description VARCHAR(500),
    initial_cash DECIMAL(18,4) NOT NULL DEFAULT 0,
    cash_balance DECIMAL(18,4) NOT NULL DEFAULT 0,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS holding (
    id INT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id INT NOT NULL,
    asset_type VARCHAR(10) NOT NULL,
    ticker VARCHAR(20),
    quantity DECIMAL(18,4) NOT NULL,
    average_price DECIMAL(18,4),
    current_price DECIMAL(18,4),
    purchase_date DATE,
    currency VARCHAR(3) DEFAULT 'USD',
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `transaction` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id INT NOT NULL,
    holding_id INT,
    type VARCHAR(4) NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    price DECIMAL(18,4) NOT NULL,
    trade_date TIMESTAMP NOT NULL,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE,
    FOREIGN KEY (holding_id) REFERENCES holding(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS portfolio_snapshot (
    id INT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id INT NOT NULL,
    snapshot_date DATE NOT NULL,
    total_value DECIMAL(18,4) NOT NULL,
    cash_balance DECIMAL(18,4) NOT NULL,
    holdings_value DECIMAL(18,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_portfolio_snapshot_date (portfolio_id, snapshot_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS price_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    price_date DATE NOT NULL,
    close_price DECIMAL(18,4) NOT NULL,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
