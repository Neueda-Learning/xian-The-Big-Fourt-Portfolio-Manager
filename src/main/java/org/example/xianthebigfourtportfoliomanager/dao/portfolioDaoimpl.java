package org.example.xianthebigfourtportfoliomanager.dao;


import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class portfolioDaoimpl implements PortfolioDao{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<portfolio> findAll() {
        String sql = "select * from portfolio";
        return jdbcTemplate.query(sql, (rs, row) -> {
            Timestamp createdAt = rs.getTimestamp("create_at");
            Timestamp updatedAt = rs.getTimestamp("update_at");
            return new portfolio(
                    rs.getInt("id"),
                    rs.getString("pro_name"),
                    rs.getString("pro_description"),
                    createdAt == null ? null : createdAt.toLocalDateTime(),
                    updatedAt == null ? null : updatedAt.toLocalDateTime()
            );
        });

    }

    @Override
    public portfolio findById(int id) {
        String sql = "select * from portfolio where id = ?";
        List<portfolio> results = jdbcTemplate.query(sql, (rs, row) -> {
            Timestamp createdAt = rs.getTimestamp("create_at");
            Timestamp updatedAt = rs.getTimestamp("update_at");
            return new portfolio(
                    rs.getInt("id"),
                    rs.getString("pro_name"),
                    rs.getString("pro_description"),
                    createdAt == null ? null : createdAt.toLocalDateTime(),
                    updatedAt == null ? null : updatedAt.toLocalDateTime()
            );
        }, id);

        return results.isEmpty() ? null : results.get(0);

    }

    //insert data
    @Override
    public portfolio save(portfolio portf) {
        String sql = "insert into portfolio (pro_name,pro_description) values (?,?)";
        int rows = jdbcTemplate.update(sql, portf.getName(), portf.getDescription());
        if (rows == 0) {
            return null;
        }

        // MySQL-specific: returns auto-increment id from this session.
        Integer generatedId = jdbcTemplate.queryForObject("select LAST_INSERT_ID()", Integer.class);
        if (generatedId == null) {
            return portf;
        }

        portf.setId(generatedId);
        return findById(generatedId);
    }

    @Override
    public portfolio update(portfolio portf) {
        String sql = "update portfolio set pro_name = ?, pro_description = ?, update_at = CURRENT_TIMESTAMP where id = ?";
        jdbcTemplate.update(sql, portf.getName(), portf.getDescription(), portf.getId());
        return findById(portf.getId());
    }

    @Override
    public void deleteById(int id) {
        jdbcTemplate.update("delete from portfolio where id = ?",id);

    }

    @Override
    public boolean existsById(int id) {
        String sql = "select count(*) from portfolio where id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);

        return count != null && count > 0;
    }
}
