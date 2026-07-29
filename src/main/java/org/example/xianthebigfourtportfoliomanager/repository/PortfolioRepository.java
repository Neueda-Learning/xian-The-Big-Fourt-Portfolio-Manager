package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class PortfolioRepository {

    private final JdbcTemplate jdbcTemplate;

    public PortfolioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public portfolio getPortfolioById(int id) {
        String sql = "select * from portfolio where id = ?";
        List<portfolio> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp createAt = rs.getTimestamp("create_at");
            Timestamp updateAt = rs.getTimestamp("update_at");
            return new portfolio(
                    rs.getInt("id"),
                    rs.getString("pro_name"),
                    rs.getString("pro_description"),
                    rs.getBigDecimal("initial_cash"),
                    rs.getBigDecimal("cash_balance"),
                    createAt == null ? null : createAt.toLocalDateTime(),
                    updateAt == null ? null : updateAt.toLocalDateTime()
            );
        }, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<portfolio> getAllPortfolios() {
        String sql = "select * from portfolio";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp createAt = rs.getTimestamp("create_at");
            Timestamp updateAt = rs.getTimestamp("update_at");
            return new portfolio(
                    rs.getInt("id"),
                    rs.getString("pro_name"),
                    rs.getString("pro_description"),
                    rs.getBigDecimal("initial_cash"),
                    rs.getBigDecimal("cash_balance"),
                    createAt == null ? null : createAt.toLocalDateTime(),
                    updateAt == null ? null : updateAt.toLocalDateTime()
            );
        });
    }

    public portfolio save(portfolio portf) {
        BigDecimal initialCash = portf.getInitialCash() == null ? BigDecimal.ZERO : portf.getInitialCash();
        BigDecimal cashBalance = initialCash;
        String sql = "insert into portfolio (pro_name, pro_description, initial_cash, cash_balance) values (?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql, portf.getName(), portf.getDescription(), initialCash, cashBalance);
        if (rows == 0) {
            return null;
        }

        Integer id = jdbcTemplate.queryForObject("select LAST_INSERT_ID()", Integer.class);
        if (id == null) {
            return portf;
        }
        return getPortfolioById(id);
    }

    public portfolio update(portfolio portf) {
        String sql = "update portfolio set pro_name = ?, pro_description = ?, update_at = CURRENT_TIMESTAMP where id = ?";
        jdbcTemplate.update(sql, portf.getName(), portf.getDescription(), portf.getId());
        return getPortfolioById(portf.getId());
    }

    public portfolio updateCashBalance(int id, BigDecimal cashBalance) {
        String sql = "update portfolio set cash_balance = ?, update_at = CURRENT_TIMESTAMP where id = ?";
        jdbcTemplate.update(sql, cashBalance, id);
        return getPortfolioById(id);
    }

    public portfolio updateInitialCashAndBalance(int id, BigDecimal initialCash, BigDecimal cashBalance) {
        String sql = "update portfolio set initial_cash = ?, cash_balance = ?, update_at = CURRENT_TIMESTAMP where id = ?";
        jdbcTemplate.update(sql, initialCash, cashBalance, id);
        return getPortfolioById(id);
    }

    public int deleteById(int id) {
        String sql = "delete from portfolio where id = ?";
        return jdbcTemplate.update(sql, id);
    }

    public boolean existsById(int id) {
        String sql = "select count(*) from portfolio where id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count.intValue() > 0;
    }
}

