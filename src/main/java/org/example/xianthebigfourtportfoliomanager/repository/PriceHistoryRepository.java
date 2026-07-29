package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Repository
public class PriceHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public PriceHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public priceHistory getPriceByTickerAndDate(String ticker, LocalDate date) {
        String sql = "select * from price_history where ticker = ? and price_date = ?";
        List<priceHistory> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date priceDate = rs.getDate("price_date");
            Timestamp createAt = rs.getTimestamp("create_at");
            return new priceHistory(
                    rs.getInt("id"),
                    rs.getString("ticker"),
                    priceDate == null ? null : priceDate.toLocalDate(),
                    rs.getBigDecimal("close_price"),
                    createAt == null ? null : createAt.toLocalDateTime()
            );
        }, ticker, date);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<priceHistory> getPricesByTickerAndRange(String ticker, LocalDate startDate, LocalDate endDate) {
        String sql = "select * from price_history where ticker = ? and price_date between ? and ? order by price_date";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date priceDate = rs.getDate("price_date");
            Timestamp createAt = rs.getTimestamp("create_at");
            return new priceHistory(
                    rs.getInt("id"),
                    rs.getString("ticker"),
                    priceDate == null ? null : priceDate.toLocalDate(),
                    rs.getBigDecimal("close_price"),
                    createAt == null ? null : createAt.toLocalDateTime()
            );
        }, ticker, startDate, endDate);
    }

    /**
     * Eren issue: when Yahoo quote was unavailable, gain/loss stayed flat because no local latest-price fallback existed.
     * Fix: expose a latest close-price query so valuation can fall back to local price_history before purchase cost.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    public priceHistory getLatestPriceByTicker(String ticker) {
        String sql = "select * from price_history where ticker = ? order by price_date desc limit 1";
        List<priceHistory> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date priceDate = rs.getDate("price_date");
            Timestamp createAt = rs.getTimestamp("create_at");
            return new priceHistory(
                    rs.getInt("id"),
                    rs.getString("ticker"),
                    priceDate == null ? null : priceDate.toLocalDate(),
                    rs.getBigDecimal("close_price"),
                    createAt == null ? null : createAt.toLocalDateTime()
            );
        }, ticker);
        return list.isEmpty() ? null : list.get(0);
    }

    public priceHistory save(priceHistory priceHistory) {
        String sql = "insert into price_history (ticker, price_date, close_price) values (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rows = jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, priceHistory.getTicker());
            ps.setObject(2, priceHistory.getPriceDate());
            ps.setBigDecimal(3, priceHistory.getCloseprice());
            return ps;
        }, keyHolder);
        if (rows == 0) {
            return null;
        }
        return getPriceByTickerAndDate(priceHistory.getTicker(), priceHistory.getPriceDate());
    }

    public int deleteByTickerAndDate(String ticker, LocalDate date) {
        String sql = "delete from price_history where ticker = ? and price_date = ?";
        return jdbcTemplate.update(sql, ticker, date);
    }
}

