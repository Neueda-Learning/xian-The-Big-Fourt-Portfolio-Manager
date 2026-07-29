-- H2 Database schema (compatible with MySQL when using MySQL driver)
CREATE TABLE IF NOT EXISTS portfolio (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pro_name VARCHAR(100) NOT NULL,
    pro_description VARCHAR(500),
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS holding (
    id INT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id INT NOT NULL,
    asset_type VARCHAR(10) NOT NULL,
    ticker VARCHAR(20),
    quantity DECIMAL(18,4) NOT NULL,
    purchase_price DECIMAL(18,4),
    purchase_date DATE,
    currency VARCHAR(3) DEFAULT 'USD',
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    holding_id INT NOT NULL,
    type VARCHAR(4) NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    price DECIMAL(18,4) NOT NULL,
    trade_date TIMESTAMP NOT NULL,
    FOREIGN KEY (holding_id) REFERENCES holding(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS price_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    price_date DATE NOT NULL,
    close_price DECIMAL(18,4) NOT NULL,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
