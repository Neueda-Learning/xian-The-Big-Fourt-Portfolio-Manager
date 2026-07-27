package org.example.xianthebigfourtportfoliomanager.dao;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioDao {
    List<portfolio> findAll();
    Optional<portfolio> findById(int id);
    portfolio save(portfolio portf);
    portfolio update(portfolio portf);
    void deleteById(int id);
    boolean existsById(int id);
}
