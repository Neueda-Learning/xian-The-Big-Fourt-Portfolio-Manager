package org.example.xianthebigfourtportfoliomanager.repository;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        String sql = "select * from portfolio order by id";
        List<portfolio> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
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

        Map<String, portfolio> unique = new LinkedHashMap<>();
        for (portfolio item : rows) {
            String key = (item.getName() == null ? "" : item.getName().trim()) + "\u0000"
                    + (item.getDescription() == null ? "" : item.getDescription().trim());
            unique.putIfAbsent(key, item);
        }
        return List.copyOf(unique.values());
    }

    public portfolio save(portfolio portf) {
        String sql = "insert into portfolio (pro_name, pro_description) values (?, ?)";
        int rows = jdbcTemplate.update(sql, portf.getName(), portf.getDescription());
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
