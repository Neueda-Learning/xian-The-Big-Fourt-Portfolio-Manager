package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Transaction getTransactionById(int id) {
        String sql = "select * from `transaction` where id = ?";
        List<Transaction> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp tradeDate = rs.getTimestamp("trade_date");
            return new Transaction(
                    rs.getInt("id"),
                    rs.getInt("holding_id"),
                    rs.getString("type"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("price"),
                    tradeDate == null ? null : tradeDate.toLocalDateTime()
            );
        }, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Transaction> getTransactionsByHoldingId(int holdingId) {
        String sql = "select * from `transaction` where holding_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp tradeDate = rs.getTimestamp("trade_date");
            return new Transaction(
                    rs.getInt("id"),
                    rs.getInt("holding_id"),
                    rs.getString("type"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("price"),
                    tradeDate == null ? null : tradeDate.toLocalDateTime()
            );
        }, holdingId);
    }

    public List<Transaction> getTransactionsByPortfolioId(int portfolioId) {
        String sql = """
                select t.* from `transaction` t
                inner join holding h on h.id = t.holding_id
                where h.portfolio_id = ?
                order by t.trade_date desc, t.id desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp tradeDate = rs.getTimestamp("trade_date");
            return new Transaction(
                    rs.getInt("id"),
                    rs.getInt("holding_id"),
                    rs.getString("type"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("price"),
                    tradeDate == null ? null : tradeDate.toLocalDateTime()
            );
        }, portfolioId);
    }

    public Transaction save(Transaction transaction) {
        String sql = "insert into `transaction` (holding_id, type, quantity, price, trade_date) values (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rows = jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, transaction.getHoldingId());
            ps.setString(2, transaction.getType());
            ps.setBigDecimal(3, transaction.getQuantity());
            ps.setBigDecimal(4, transaction.getPrice());
            ps.setObject(5, transaction.getTradeDate());
            return ps;
        }, keyHolder);
        if (rows == 0) {
            return null;
        }

        // Keep holding market price in sync with the latest transaction price.
        syncHoldingCurrentPrice(transaction.getHoldingId(), transaction.getPrice());

        Number key = keyHolder.getKey();
        return key == null ? transaction : getTransactionById(key.intValue());
    }

    public Transaction update(Transaction transaction) {
        Transaction existing = getTransactionById(transaction.getId());
        String sql = "update `transaction` set holding_id = ?, type = ?, quantity = ?, price = ?, trade_date = ? where id = ?";
        jdbcTemplate.update(
                sql,
                transaction.getHoldingId(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getTradeDate(),
                transaction.getId()
        );

        recalculateHoldingCurrentPrice(transaction.getHoldingId());
        if (existing != null && existing.getHoldingId() != transaction.getHoldingId()) {
            recalculateHoldingCurrentPrice(existing.getHoldingId());
        }

        return getTransactionById(transaction.getId());
    }

    public int deleteById(int id) {
        Transaction existing = getTransactionById(id);
        String sql = "delete from `transaction` where id = ?";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 1 && existing != null) {
            recalculateHoldingCurrentPrice(existing.getHoldingId());
        }
        return rows;
    }

    private void syncHoldingCurrentPrice(int holdingId, BigDecimal price) {
        String sql = "update holding set current_price = ?, updated_at = CURRENT_TIMESTAMP where id = ?";
        jdbcTemplate.update(sql, price, holdingId);
    }

    private void recalculateHoldingCurrentPrice(int holdingId) {
        String sql = "select price from `transaction` where holding_id = ? order by trade_date desc, id desc limit 1";
        List<BigDecimal> prices = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getBigDecimal("price"), holdingId);
        if (!prices.isEmpty()) {
            syncHoldingCurrentPrice(holdingId, prices.get(0));
        }
    }
}
