package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class HoldingRepository {

    private final JdbcTemplate jdbcTemplate;

    public HoldingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Holding getHoldingById(int id) {
        String sql = "select * from holding where id = ?";
        List<Holding> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date purchaseDate = rs.getDate("purchase_date");
            Timestamp createAt = rs.getTimestamp("create_at");
            Timestamp updateAt = rs.getTimestamp("updated_at");
            return new Holding(
                    rs.getInt("id"),
                    rs.getInt("portfolio_id"),
                    AssetType.valueOf(rs.getString("asset_type")),
                    rs.getString("ticker"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("purchase_price"),
                    purchaseDate == null ? null : purchaseDate.toLocalDate(),
                    rs.getString("currency"),
                    createAt == null ? null : createAt.toLocalDateTime(),
                    updateAt == null ? null : updateAt.toLocalDateTime()
            );
        }, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Holding> getAllHoldings() {
        String sql = "select * from holding order by portfolio_id asc, asset_type asc, ticker asc, id asc";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date purchaseDate = rs.getDate("purchase_date");
            Timestamp createAt = rs.getTimestamp("create_at");
            Timestamp updateAt = rs.getTimestamp("updated_at");
            return new Holding(
                    rs.getInt("id"),
                    rs.getInt("portfolio_id"),
                    AssetType.valueOf(rs.getString("asset_type")),
                    rs.getString("ticker"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("purchase_price"),
                    purchaseDate == null ? null : purchaseDate.toLocalDate(),
                    rs.getString("currency"),
                    createAt == null ? null : createAt.toLocalDateTime(),
                    updateAt == null ? null : updateAt.toLocalDateTime()
            );
        });
    }

    public List<Holding> getHoldingsByPortfolioId(int portfolioId) {
        String sql = "select * from holding where portfolio_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date purchaseDate = rs.getDate("purchase_date");
            Timestamp createAt = rs.getTimestamp("create_at");
            Timestamp updateAt = rs.getTimestamp("updated_at");
            return new Holding(
                    rs.getInt("id"),
                    rs.getInt("portfolio_id"),
                    AssetType.valueOf(rs.getString("asset_type")),
                    rs.getString("ticker"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("purchase_price"),
                    purchaseDate == null ? null : purchaseDate.toLocalDate(),
                    rs.getString("currency"),
                    createAt == null ? null : createAt.toLocalDateTime(),
                    updateAt == null ? null : updateAt.toLocalDateTime()
            );
        }, portfolioId);
    }

    public List<String> getDistinctTickers() {
        String sql = "select distinct upper(trim(ticker)) as ticker from holding where ticker is not null and trim(ticker) <> '' order by upper(trim(ticker))";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public Holding save(Holding holding) {
        String sql = "insert into holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) values (?, ?, ?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(
                sql,
                holding.getPortfolioId(),
                holding.getAssetType().name(),
                holding.getTicker(),
                holding.getQuantity(),
                holding.getPurchasePrice(),
                holding.getPurchasedata(),
                holding.getCurrency()
        );
        if (rows == 0) {
            return null;
        }

        Integer id = jdbcTemplate.queryForObject("select LAST_INSERT_ID()", Integer.class);
        if (id == null) {
            return holding;
        }
        return getHoldingById(id);
    }

    public Holding update(Holding holding) {
        String sql = "update holding set portfolio_id = ?, asset_type = ?, ticker = ?, quantity = ?, purchase_price = ?, purchase_date = ?, currency = ?, updated_at = CURRENT_TIMESTAMP where id = ?";
        jdbcTemplate.update(
                sql,
                holding.getPortfolioId(),
                holding.getAssetType().name(),
                holding.getTicker(),
                holding.getQuantity(),
                holding.getPurchasePrice(),
                holding.getPurchasedata(),
                holding.getCurrency(),
                holding.getId()
        );
        return getHoldingById(holding.getId());
    }

    public int deleteById(int id) {
        String sql = "delete from holding where id = ?";
        return jdbcTemplate.update(sql, id);
    }

    public boolean existsById(int id) {
        String sql = "select count(*) from holding where id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count.intValue() > 0;
    }
}

