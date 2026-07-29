package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

    public Transaction save(Transaction transaction) {
        String sql = "insert into `transaction` (holding_id, type, quantity, price, trade_date) values (?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(
                sql,
                transaction.getHoldingId(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getTradeDate()
        );
        if (rows == 0) {
            return null;
        }

        Integer id = jdbcTemplate.queryForObject("select LAST_INSERT_ID()", Integer.class);
        if (id == null) {
            return transaction;
        }
        return getTransactionById(id);
    }

    public Transaction update(Transaction transaction) {
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
        return getTransactionById(transaction.getId());
    }

    public int deleteById(int id) {
        String sql = "delete from `transaction` where id = ?";
        return jdbcTemplate.update(sql, id);
    }

    public int reassignHoldingId(int fromHoldingId, int toHoldingId) {
        String sql = "update `transaction` set holding_id = ? where holding_id = ?";
        return jdbcTemplate.update(sql, toHoldingId, fromHoldingId);
    }
}
