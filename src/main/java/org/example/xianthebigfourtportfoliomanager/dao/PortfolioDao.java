package org.example.xianthebigfourtportfoliomanager.dao;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;

import java.util.List;

public interface PortfolioDao {
    List<portfolio> findAll();
    portfolio findById(int id);
    portfolio save(portfolio portf);
    portfolio update(portfolio portf);
    void deleteById(int id);
    boolean existsById(int id);
}
