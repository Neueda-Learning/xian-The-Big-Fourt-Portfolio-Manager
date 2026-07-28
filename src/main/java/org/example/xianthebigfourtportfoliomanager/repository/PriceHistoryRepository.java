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
        String sql = "select * from price_history where ticker = ? and price_date = ? order by price_time desc";
        List<priceHistory> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date priceDate = rs.getDate("price_date");
            Timestamp priceTime = rs.getTimestamp("price_time");
            Timestamp fetchedAt = rs.getTimestamp("fetched_at");
            Timestamp createAt = rs.getTimestamp("create_at");
            return new priceHistory(
                    rs.getInt("id"),
                    rs.getString("ticker"),
                    priceDate == null ? null : priceDate.toLocalDate(),
                    priceTime == null ? null : priceTime.toLocalDateTime(),
                    rs.getBigDecimal("open_price"),
                    rs.getBigDecimal("high_price"),
                    rs.getBigDecimal("low_price"),
                    rs.getBigDecimal("close_price"),
                    rs.getBigDecimal("adjusted_close"),
                    rs.getObject("volume", Long.class),
                    rs.getString("currency"),
                    rs.getString("raw_payload"),
                    fetchedAt == null ? null : fetchedAt.toLocalDateTime(),
                    createAt == null ? null : createAt.toLocalDateTime()
            );
        }, ticker, date);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<priceHistory> getAllPrices() {
        String sql = "select * from price_history order by price_time desc, ticker asc";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date priceDate = rs.getDate("price_date");
            Timestamp priceTime = rs.getTimestamp("price_time");
            Timestamp fetchedAt = rs.getTimestamp("fetched_at");
            Timestamp createAt = rs.getTimestamp("create_at");
            return new priceHistory(
                    rs.getInt("id"),
                    rs.getString("ticker"),
                    priceDate == null ? null : priceDate.toLocalDate(),
                    priceTime == null ? null : priceTime.toLocalDateTime(),
                    rs.getBigDecimal("open_price"),
                    rs.getBigDecimal("high_price"),
                    rs.getBigDecimal("low_price"),
                    rs.getBigDecimal("close_price"),
                    rs.getBigDecimal("adjusted_close"),
                    rs.getObject("volume", Long.class),
                    rs.getString("currency"),
                    rs.getString("raw_payload"),
                    fetchedAt == null ? null : fetchedAt.toLocalDateTime(),
                    createAt == null ? null : createAt.toLocalDateTime()
            );
        });
    }

    public List<priceHistory> getPricesByTickerAndRange(String ticker, LocalDate startDate, LocalDate endDate) {
        String sql = "select * from price_history where ticker = ? and price_date between ? and ? order by price_time";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date priceDate = rs.getDate("price_date");
            Timestamp priceTime = rs.getTimestamp("price_time");
            Timestamp fetchedAt = rs.getTimestamp("fetched_at");
            Timestamp createAt = rs.getTimestamp("create_at");
            return new priceHistory(
                    rs.getInt("id"),
                    rs.getString("ticker"),
                    priceDate == null ? null : priceDate.toLocalDate(),
                    priceTime == null ? null : priceTime.toLocalDateTime(),
                    rs.getBigDecimal("open_price"),
                    rs.getBigDecimal("high_price"),
                    rs.getBigDecimal("low_price"),
                    rs.getBigDecimal("close_price"),
                    rs.getBigDecimal("adjusted_close"),
                    rs.getObject("volume", Long.class),
                    rs.getString("currency"),
                    rs.getString("raw_payload"),
                    fetchedAt == null ? null : fetchedAt.toLocalDateTime(),
                    createAt == null ? null : createAt.toLocalDateTime()
            );
        }, ticker, startDate, endDate);
    }

    public priceHistory save(priceHistory priceHistory) {
        String sql = "insert into price_history (ticker, price_date, price_time, open_price, high_price, low_price, close_price, adjusted_close, volume, currency, raw_payload, fetched_at) values (?, ?, coalesce(?, CURRENT_TIMESTAMP), ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        int rows = jdbcTemplate.update(
                sql,
                priceHistory.getTicker(),
                priceHistory.getPriceDate(),
                priceHistory.getPricetime(),
                priceHistory.getOpenprice(),
                priceHistory.getHighprice(),
                priceHistory.getLowprice(),
                priceHistory.getCloseprice(),
                priceHistory.getAdjustedclose(),
                priceHistory.getVolume(),
                priceHistory.getCurrency(),
                priceHistory.getRawpayload()
        );
        if (rows == 0) {
            return null;
        }

        Integer id = jdbcTemplate.queryForObject("select LAST_INSERT_ID()", Integer.class);
        if (id == null) {
            return priceHistory;
        }
        return getPriceByTickerAndDate(priceHistory.getTicker(), priceHistory.getPriceDate());
    }

    public priceHistory saveOrUpdate(priceHistory priceHistory) {
        String sql = "insert into price_history (ticker, price_date, price_time, open_price, high_price, low_price, close_price, adjusted_close, volume, currency, raw_payload, fetched_at) " +
                "values (?, ?, coalesce(?, CURRENT_TIMESTAMP), ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "on duplicate key update " +
                "price_date = values(price_date), " +
                "open_price = values(open_price), " +
                "high_price = values(high_price), " +
                "low_price = values(low_price), " +
                "close_price = values(close_price), " +
                "adjusted_close = values(adjusted_close), " +
                "volume = values(volume), " +
                "currency = values(currency), " +
                "raw_payload = values(raw_payload), " +
                "fetched_at = CURRENT_TIMESTAMP";

        jdbcTemplate.update(
                sql,
                priceHistory.getTicker(),
                priceHistory.getPriceDate(),
                priceHistory.getPricetime(),
                priceHistory.getOpenprice(),
                priceHistory.getHighprice(),
                priceHistory.getLowprice(),
                priceHistory.getCloseprice(),
                priceHistory.getAdjustedclose(),
                priceHistory.getVolume(),
                priceHistory.getCurrency(),
                priceHistory.getRawpayload()
        );

        return getPriceByTickerAndDate(priceHistory.getTicker(), priceHistory.getPriceDate());
    }

    public int deleteByTickerAndDate(String ticker, LocalDate date) {
        String sql = "delete from price_history where ticker = ? and price_date = ?";
        return jdbcTemplate.update(sql, ticker, date);
    }
}

