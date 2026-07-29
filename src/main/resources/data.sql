UPDATE `transaction` tx
JOIN holding h_dup ON tx.holding_id = h_dup.id
JOIN holding h_keep ON h_keep.id = (
    SELECT MIN(h2.id)
    FROM holding h2
    WHERE h2.portfolio_id = h_dup.portfolio_id
      AND h2.asset_type = h_dup.asset_type
      AND h2.ticker <=> h_dup.ticker
      AND h2.quantity <=> h_dup.quantity
      AND h2.purchase_price <=> h_dup.purchase_price
      AND h2.purchase_date <=> h_dup.purchase_date
      AND h2.currency <=> h_dup.currency
)
SET tx.holding_id = h_keep.id
WHERE h_dup.id <> h_keep.id;

DELETE tx_dup FROM `transaction` tx_dup
JOIN `transaction` tx_keep
  ON tx_dup.id > tx_keep.id
 AND tx_dup.holding_id = tx_keep.holding_id
 AND tx_dup.type = tx_keep.type
 AND tx_dup.quantity <=> tx_keep.quantity
 AND tx_dup.price <=> tx_keep.price
 AND tx_dup.trade_date <=> tx_keep.trade_date;

DELETE h_dup FROM holding h_dup
JOIN holding h_keep
  ON h_dup.id > h_keep.id
 AND h_dup.portfolio_id = h_keep.portfolio_id
 AND h_dup.asset_type = h_keep.asset_type
 AND h_dup.ticker <=> h_keep.ticker
 AND h_dup.quantity <=> h_keep.quantity
 AND h_dup.purchase_price <=> h_keep.purchase_price
 AND h_dup.purchase_date <=> h_keep.purchase_date
 AND h_dup.currency <=> h_keep.currency;

UPDATE holding h_keep
JOIN (
    SELECT MIN(id) AS keep_id, SUM(quantity) AS total_quantity
    FROM holding
    WHERE asset_type = 'CASH' OR ticker = 'CASH'
) cash ON h_keep.id = cash.keep_id
SET h_keep.asset_type = 'CASH',
    h_keep.ticker = 'CASH',
    h_keep.quantity = cash.total_quantity,
    h_keep.purchase_price = 1.0000,
    h_keep.currency = COALESCE(h_keep.currency, 'USD');

UPDATE `transaction` tx
JOIN holding h_cash ON tx.holding_id = h_cash.id
JOIN (
    SELECT MIN(id) AS keep_id
    FROM holding
    WHERE asset_type = 'CASH' OR ticker = 'CASH'
) cash ON 1 = 1
SET tx.holding_id = cash.keep_id
WHERE (h_cash.asset_type = 'CASH' OR h_cash.ticker = 'CASH')
  AND tx.holding_id <> cash.keep_id;

DELETE h_dup FROM holding h_dup
JOIN (
    SELECT MIN(id) AS keep_id
    FROM holding
    WHERE asset_type = 'CASH' OR ticker = 'CASH'
) cash ON 1 = 1
WHERE (h_dup.asset_type = 'CASH' OR h_dup.ticker = 'CASH')
  AND h_dup.id <> cash.keep_id;

INSERT INTO portfolio (pro_name, pro_description)
SELECT '稳健增长组合', '以蓝筹股和国债为主，适合长期持有'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM portfolio
    WHERE pro_name = '稳健增长组合'
      AND pro_description = '以蓝筹股和国债为主，适合长期持有'
);

INSERT INTO portfolio (pro_name, pro_description)
SELECT '科技先锋组合', '聚焦高成长科技股，风险偏好较高'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM portfolio
    WHERE pro_name = '科技先锋组合'
      AND pro_description = '聚焦高成长科技股，风险偏好较高'
);

INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency)
SELECT p.id, 'STOCK', 'AAPL', 100.0000, 185.5000, '2026-01-15', 'USD'
FROM (SELECT MIN(id) AS id FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有') p
WHERE p.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM holding h
      WHERE h.portfolio_id = p.id
        AND h.asset_type = 'STOCK'
        AND h.ticker = 'AAPL'
        AND h.quantity = 100.0000
        AND h.purchase_price = 185.5000
        AND h.purchase_date = '2026-01-15'
        AND h.currency = 'USD'
  );

INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency)
SELECT p.id, 'STOCK', 'MSFT', 50.0000, 420.3000, '2026-02-01', 'USD'
FROM (SELECT MIN(id) AS id FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有') p
WHERE p.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM holding h
      WHERE h.portfolio_id = p.id
        AND h.asset_type = 'STOCK'
        AND h.ticker = 'MSFT'
        AND h.quantity = 50.0000
        AND h.purchase_price = 420.3000
        AND h.purchase_date = '2026-02-01'
        AND h.currency = 'USD'
  );

INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency)
SELECT p.id, 'BOND', 'US10Y', 50.0000, 98.5000, '2026-01-20', 'USD'
FROM (SELECT MIN(id) AS id FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有') p
WHERE p.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM holding h
      WHERE h.portfolio_id = p.id
        AND h.asset_type = 'BOND'
        AND h.ticker = 'US10Y'
        AND h.quantity = 50.0000
        AND h.purchase_price = 98.5000
        AND h.purchase_date = '2026-01-20'
        AND h.currency = 'USD'
  );

INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency)
SELECT p.id, 'CASH', 'CASH', 50000.0000, 1.0000, '2026-01-10', 'USD'
FROM (SELECT MIN(id) AS id FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有') p
WHERE p.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM holding h
      WHERE h.portfolio_id = p.id
        AND h.asset_type = 'CASH'
        AND h.ticker <=> 'CASH'
        AND h.quantity = 50000.0000
        AND h.purchase_price = 1.0000
        AND h.purchase_date = '2026-01-10'
        AND h.currency = 'USD'
  );

INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date)
SELECT h.id, 'BUY', 50000.0000, 1.0000, '2026-01-10 00:00:00'
FROM holding h
JOIN portfolio p ON p.id = h.portfolio_id
WHERE p.id = (SELECT MIN(id) FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有')
  AND h.asset_type = 'CASH'
  AND h.ticker = 'CASH'
  AND h.quantity = 50000.0000
  AND h.purchase_price = 1.0000
  AND h.purchase_date = '2026-01-10'
  AND h.currency = 'USD'
  AND NOT EXISTS (
      SELECT 1 FROM `transaction` tx
      WHERE tx.holding_id = h.id
        AND tx.type = 'BUY'
        AND tx.quantity = 50000.0000
        AND tx.price = 1.0000
        AND tx.trade_date = '2026-01-10 00:00:00'
  );

INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency)
SELECT p.id, 'STOCK', 'TSLA', 30.0000, 280.0000, '2026-04-01', 'USD'
FROM (SELECT MIN(id) AS id FROM portfolio WHERE pro_name = '科技先锋组合' AND pro_description = '聚焦高成长科技股，风险偏好较高') p
WHERE p.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM holding h
      WHERE h.portfolio_id = p.id
        AND h.asset_type = 'STOCK'
        AND h.ticker = 'TSLA'
        AND h.quantity = 30.0000
        AND h.purchase_price = 280.0000
        AND h.purchase_date = '2026-04-01'
        AND h.currency = 'USD'
  );

INSERT INTO holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency)
SELECT p.id, 'STOCK', 'NVDA', 20.0000, 880.5000, '2026-04-01', 'USD'
FROM (SELECT MIN(id) AS id FROM portfolio WHERE pro_name = '科技先锋组合' AND pro_description = '聚焦高成长科技股，风险偏好较高') p
WHERE p.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM holding h
      WHERE h.portfolio_id = p.id
        AND h.asset_type = 'STOCK'
        AND h.ticker = 'NVDA'
        AND h.quantity = 20.0000
        AND h.purchase_price = 880.5000
        AND h.purchase_date = '2026-04-01'
        AND h.currency = 'USD'
  );

INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date)
SELECT h.id, 'BUY', 40.0000, 180.2500, '2026-01-15 10:00:00'
FROM holding h
JOIN portfolio p ON p.id = h.portfolio_id
WHERE p.id = (SELECT MIN(id) FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有')
  AND h.asset_type = 'STOCK'
  AND h.ticker = 'AAPL'
  AND h.quantity = 100.0000
  AND h.purchase_price = 185.5000
  AND h.purchase_date = '2026-01-15'
  AND h.currency = 'USD'
  AND NOT EXISTS (
      SELECT 1 FROM `transaction` tx
      WHERE tx.holding_id = h.id
        AND tx.type = 'BUY'
        AND tx.quantity = 40.0000
        AND tx.price = 180.2500
        AND tx.trade_date = '2026-01-15 10:00:00'
  );

INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date)
SELECT h.id, 'BUY', 60.0000, 188.5000, '2026-02-05 11:15:00'
FROM holding h
JOIN portfolio p ON p.id = h.portfolio_id
WHERE p.id = (SELECT MIN(id) FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有')
  AND h.asset_type = 'STOCK'
  AND h.ticker = 'AAPL'
  AND h.quantity = 100.0000
  AND h.purchase_price = 185.5000
  AND h.purchase_date = '2026-01-15'
  AND h.currency = 'USD'
  AND NOT EXISTS (
      SELECT 1 FROM `transaction` tx
      WHERE tx.holding_id = h.id
        AND tx.type = 'BUY'
        AND tx.quantity = 60.0000
        AND tx.price = 188.5000
        AND tx.trade_date = '2026-02-05 11:15:00'
  );

INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date)
SELECT h.id, 'BUY', 50.0000, 420.3000, '2026-02-01 09:30:00'
FROM holding h
JOIN portfolio p ON p.id = h.portfolio_id
WHERE p.id = (SELECT MIN(id) FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有')
  AND h.asset_type = 'STOCK'
  AND h.ticker = 'MSFT'
  AND h.quantity = 50.0000
  AND h.purchase_price = 420.3000
  AND h.purchase_date = '2026-02-01'
  AND h.currency = 'USD'
  AND NOT EXISTS (
      SELECT 1 FROM `transaction` tx
      WHERE tx.holding_id = h.id
        AND tx.type = 'BUY'
        AND tx.quantity = 50.0000
        AND tx.price = 420.3000
        AND tx.trade_date = '2026-02-01 09:30:00'
  );

INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date)
SELECT h.id, 'BUY', 50.0000, 98.5000, '2026-01-20 14:20:00'
FROM holding h
JOIN portfolio p ON p.id = h.portfolio_id
WHERE p.id = (SELECT MIN(id) FROM portfolio WHERE pro_name = '稳健增长组合' AND pro_description = '以蓝筹股和国债为主，适合长期持有')
  AND h.asset_type = 'BOND'
  AND h.ticker = 'US10Y'
  AND h.quantity = 50.0000
  AND h.purchase_price = 98.5000
  AND h.purchase_date = '2026-01-20'
  AND h.currency = 'USD'
  AND NOT EXISTS (
      SELECT 1 FROM `transaction` tx
      WHERE tx.holding_id = h.id
        AND tx.type = 'BUY'
        AND tx.quantity = 50.0000
        AND tx.price = 98.5000
        AND tx.trade_date = '2026-01-20 14:20:00'
  );

INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date)
SELECT h.id, 'BUY', 30.0000, 280.0000, '2026-04-01 13:10:00'
FROM holding h
JOIN portfolio p ON p.id = h.portfolio_id
WHERE p.id = (SELECT MIN(id) FROM portfolio WHERE pro_name = '科技先锋组合' AND pro_description = '聚焦高成长科技股，风险偏好较高')
  AND h.asset_type = 'STOCK'
  AND h.ticker = 'TSLA'
  AND h.quantity = 30.0000
  AND h.purchase_price = 280.0000
  AND h.purchase_date = '2026-04-01'
  AND h.currency = 'USD'
  AND NOT EXISTS (
      SELECT 1 FROM `transaction` tx
      WHERE tx.holding_id = h.id
        AND tx.type = 'BUY'
        AND tx.quantity = 30.0000
        AND tx.price = 280.0000
        AND tx.trade_date = '2026-04-01 13:10:00'
  );

INSERT INTO `transaction` (holding_id, type, quantity, price, trade_date)
SELECT h.id, 'BUY', 20.0000, 880.5000, '2026-04-01 13:20:00'
FROM holding h
JOIN portfolio p ON p.id = h.portfolio_id
WHERE p.id = (SELECT MIN(id) FROM portfolio WHERE pro_name = '科技先锋组合' AND pro_description = '聚焦高成长科技股，风险偏好较高')
  AND h.asset_type = 'STOCK'
  AND h.ticker = 'NVDA'
  AND h.quantity = 20.0000
  AND h.purchase_price = 880.5000
  AND h.purchase_date = '2026-04-01'
  AND h.currency = 'USD'
  AND NOT EXISTS (
      SELECT 1 FROM `transaction` tx
      WHERE tx.holding_id = h.id
        AND tx.type = 'BUY'
        AND tx.quantity = 20.0000
        AND tx.price = 880.5000
        AND tx.trade_date = '2026-04-01 13:20:00'
  );

INSERT INTO price_history (ticker, price_date, close_price)
SELECT 'AAPL', '2026-07-20', 194.2000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = 'AAPL' AND price_date = '2026-07-20' AND close_price = 194.2000);

INSERT INTO price_history (ticker, price_date, close_price)
SELECT 'AAPL', '2026-07-21', 195.8000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = 'AAPL' AND price_date = '2026-07-21' AND close_price = 195.8000);

INSERT INTO price_history (ticker, price_date, close_price)
SELECT 'AAPL', '2026-07-22', 197.1000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = 'AAPL' AND price_date = '2026-07-22' AND close_price = 197.1000);

INSERT INTO price_history (ticker, price_date, close_price)
SELECT 'MSFT', '2026-07-20', 423.2000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = 'MSFT' AND price_date = '2026-07-20' AND close_price = 423.2000);

INSERT INTO price_history (ticker, price_date, close_price)
SELECT 'MSFT', '2026-07-21', 425.6000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = 'MSFT' AND price_date = '2026-07-21' AND close_price = 425.6000);

INSERT INTO price_history (ticker, price_date, close_price)
SELECT 'TSLA', '2026-07-20', 297.3000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = 'TSLA' AND price_date = '2026-07-20' AND close_price = 297.3000);

INSERT INTO price_history (ticker, price_date, close_price)
SELECT 'NVDA', '2026-07-20', 905.8000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = 'NVDA' AND price_date = '2026-07-20' AND close_price = 905.8000);

INSERT INTO price_history (ticker, price_date, close_price)
SELECT 'US10Y', '2026-07-20', 99.1500
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM price_history WHERE ticker = 'US10Y' AND price_date = '2026-07-20' AND close_price = 99.1500);
