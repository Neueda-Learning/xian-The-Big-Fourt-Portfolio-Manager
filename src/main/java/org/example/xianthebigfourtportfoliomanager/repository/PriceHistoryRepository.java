package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
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

    public priceHistory save(priceHistory priceHistory) {
        String sql = "insert into price_history (ticker, price_date, close_price) values (?, ?, ?)";
        int rows = jdbcTemplate.update(sql, priceHistory.getTicker(), priceHistory.getPriceDate(), priceHistory.getCloseprice());
        if (rows == 0) {
            return null;
        }

        Integer id = jdbcTemplate.queryForObject("select LAST_INSERT_ID()", Integer.class);
        if (id == null) {
            return priceHistory;
        }
        return getPriceByTickerAndDate(priceHistory.getTicker(), priceHistory.getPriceDate());
    }

    public int deleteByTickerAndDate(String ticker, LocalDate date) {
        String sql = "delete from price_history where ticker = ? and price_date = ?";
        return jdbcTemplate.update(sql, ticker, date);
    }
}

