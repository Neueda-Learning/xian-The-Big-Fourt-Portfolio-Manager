package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
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
                    createAt == null ? null : createAt.toLocalDateTime(),
                    updateAt == null ? null : updateAt.toLocalDateTime()
            );
        });
    }

    public portfolio save(portfolio portf) {
        String sql = "insert into portfolio (pro_name, pro_description) values (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rows = jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, portf.getName());
            ps.setString(2, portf.getDescription());
            return ps;
        }, keyHolder);
        if (rows == 0) {
            return null;
        }
        Number key = keyHolder.getKey();
        return key == null ? portf : getPortfolioById(key.intValue());
    }

    public portfolio update(portfolio portf) {
        String sql = "update portfolio set pro_name = ?, pro_description = ?, update_at = CURRENT_TIMESTAMP where id = ?";
        jdbcTemplate.update(sql, portf.getName(), portf.getDescription(), portf.getId());
        return getPortfolioById(portf.getId());
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
