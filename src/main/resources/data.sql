INSERT INTO portfolio (pro_name, pro_description) VALUES ('稳健增长组合', '以蓝筹股和国债为主，适合长期持有');
INSERT INTO portfolio (pro_name, pro_description) VALUES ('科技先锋组合', '聚焦高成长科技股，风险偏好较高');

INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (1, 'STOCK', 'AAPL', 100.0000, 185.5000, '2026-01-15', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (1, 'STOCK', 'MSFT', 50.0000, 420.3000, '2026-02-01', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (1, 'BOND', 'US10Y', 50.0000, 98.5000, '2026-01-20', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (1, 'CASH', NULL, 50000.0000, 1.0000, '2026-01-10', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (2, 'STOCK', 'TSLA', 30.0000, 280.0000, '2026-04-01', 'USD');
INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) VALUES (2, 'STOCK', 'NVDA', 20.0000, 880.5000, '2026-04-01', 'USD');
<<<<<<< HEAD

INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date) VALUES (1, 'BUY', 40.0000, 180.2500, '2026-01-15 10:00:00');
INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date) VALUES (1, 'BUY', 60.0000, 188.5000, '2026-02-05 11:15:00');
INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date) VALUES (2, 'BUY', 50.0000, 420.3000, '2026-02-01 09:30:00');
INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date) VALUES (3, 'BUY', 50.0000, 98.5000, '2026-01-20 14:20:00');
INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date) VALUES (5, 'BUY', 30.0000, 280.0000, '2026-04-01 13:10:00');
INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date) VALUES (6, 'BUY', 20.0000, 880.5000, '2026-04-01 13:20:00');

INSERT INTO price_history (ticker, price_date, close_price) VALUES ('AAPL', '2026-07-20', 194.2000);
INSERT INTO price_history (ticker, price_date, close_price) VALUES ('AAPL', '2026-07-21', 195.8000);
INSERT INTO price_history (ticker, price_date, close_price) VALUES ('AAPL', '2026-07-22', 197.1000);
INSERT INTO price_history (ticker, price_date, close_price) VALUES ('MSFT', '2026-07-20', 423.2000);
INSERT INTO price_history (ticker, price_date, close_price) VALUES ('MSFT', '2026-07-21', 425.6000);
INSERT INTO price_history (ticker, price_date, close_price) VALUES ('TSLA', '2026-07-20', 297.3000);
INSERT INTO price_history (ticker, price_date, close_price) VALUES ('NVDA', '2026-07-20', 905.8000);
INSERT INTO price_history (ticker, price_date, close_price) VALUES ('US10Y', '2026-07-20', 99.1500);
=======
>>>>>>> dd09e73e365a435c4c9bed5aa08e68d924147e41
