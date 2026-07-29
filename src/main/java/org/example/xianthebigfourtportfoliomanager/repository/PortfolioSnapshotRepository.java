package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.PortfolioSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Repository
public class PortfolioSnapshotRepository {

    private final JdbcTemplate jdbcTemplate;

    public PortfolioSnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int upsertDailySnapshot(int portfolioId, LocalDate snapshotDate,
                                   BigDecimal totalValue, BigDecimal cashBalance, BigDecimal holdingsValue) {
        String sql = """
                insert into portfolio_snapshot (portfolio_id, snapshot_date, total_value, cash_balance, holdings_value)
                values (?, ?, ?, ?, ?)
                on duplicate key update
                    total_value = values(total_value),
                    cash_balance = values(cash_balance),
                    holdings_value = values(holdings_value)
                """;
        return jdbcTemplate.update(
                sql,
                portfolioId,
                snapshotDate,
                totalValue,
                cashBalance,
                holdingsValue
        );
    }

    public List<PortfolioSnapshot> getByPortfolioAndRange(int portfolioId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                select * from portfolio_snapshot
                where portfolio_id = ? and snapshot_date between ? and ?
                order by snapshot_date asc
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date snapshotDate = rs.getDate("snapshot_date");
            Timestamp createdAt = rs.getTimestamp("created_at");
            return new PortfolioSnapshot(
                    rs.getInt("id"),
                    rs.getInt("portfolio_id"),
                    snapshotDate == null ? null : snapshotDate.toLocalDate(),
                    rs.getBigDecimal("total_value"),
                    rs.getBigDecimal("cash_balance"),
                    rs.getBigDecimal("holdings_value"),
                    createdAt == null ? null : createdAt.toLocalDateTime()
            );
        }, portfolioId, startDate, endDate);
    }
}
