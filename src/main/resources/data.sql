INSERT INTO portfolio (pro_name, pro_description) VALUES ('稳健增长组合', '以蓝筹股和国债为主，适合长期持有');
INSERT INTO portfolio (pro_name, pro_description) VALUES ('科技先锋组合', '聚焦高成长科技股，风险偏好较高');

INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (1, 'STOCK', 'AAPL', 100.0000, 185.5000, '2026-01-15', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (1, 'STOCK', 'MSFT', 50.0000, 420.3000, '2026-02-01', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (1, 'BOND', 'US10Y', 50.0000, 98.5000, '2026-01-20', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (1, 'CASH', NULL, 50000.0000, 1.0000, '2026-01-10', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (2, 'STOCK', 'TSLA', 30.0000, 280.0000, '2026-04-01', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (2, 'STOCK', 'NVDA', 20.0000, 880.5000, '2026-04-01', 'USD');
