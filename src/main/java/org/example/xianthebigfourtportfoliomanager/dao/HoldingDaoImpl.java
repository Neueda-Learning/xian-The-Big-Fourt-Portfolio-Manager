package org.example.xianthebigfourtportfoliomanager.dao;

import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class HoldingDaoImpl implements HoldingDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Holding> findByPortfolioId(int portfolioId) {
        String sql = "select * from holding where portfolio_id = ?";
        return jdbcTemplate.query(sql, (rs, row) -> {
            Date purchaseDate = rs.getDate("purchase_date");
            Timestamp createdAt = rs.getTimestamp("create_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");

            return new Holding(
                    rs.getInt("id"),
                    rs.getInt("portfolio_id"),
                    AssetType.valueOf(rs.getString("asset_type")),
                    rs.getString("ticker"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("purchase_price"),
                    purchaseDate == null ? null : purchaseDate.toLocalDate(),
                    rs.getString("currency"),
                    createdAt == null ? null : createdAt.toLocalDateTime(),
                    updatedAt == null ? null : updatedAt.toLocalDateTime()
            );
        }, portfolioId);

    }

    @Override
    public Holding findById(int id) {
        String sql = "select * from holding where id = ?";
        List<Holding> list = jdbcTemplate.query(sql, (rs, row) -> {
            Date purchaseDate = rs.getDate("purchase_date");
            Timestamp createdAt = rs.getTimestamp("create_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");

            return new Holding(
                    rs.getInt("id"),
                    rs.getInt("portfolio_id"),
                    AssetType.valueOf(rs.getString("asset_type")),
                    rs.getString("ticker"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("purchase_price"),
                    purchaseDate == null ? null : purchaseDate.toLocalDate(),
                    rs.getString("currency"),
                    createdAt == null ? null : createdAt.toLocalDateTime(),
                    updatedAt == null ? null : updatedAt.toLocalDateTime()
            );
        }, id);

        return list.isEmpty() ? null : list.get(0);

    }

    @Override
    public Holding save(Holding holding) {
        String sql = "insert into holding (portfolio_id, asset_type, ticker, quantity, purchase_price, purchase_date, currency) " +
                "values (?, ?, ?, ?, ?, ?, ?)";
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

        Integer generatedId = jdbcTemplate.queryForObject("select LAST_INSERT_ID()", Integer.class);
        if (generatedId == null) {
            return holding;
        }

        holding.setId(generatedId);
        return findById(generatedId);
    }

    @Override
    public Holding update(Holding holding) {
        String sql = "update holding set portfolio_id = ?, asset_type = ?, ticker = ?, quantity = ?, " +
                "purchase_price = ?, purchase_date = ?, currency = ?, updated_at = CURRENT_TIMESTAMP where id = ?";
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

        return findById(holding.getId());
    }

    @Override
    public void deletebyid(int id) {
        jdbcTemplate.update("delete from holding where id = ?", id);
    }

    @Override
    public boolean existbyid(int id) {
        String sql = "select count(*) from holding where id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
