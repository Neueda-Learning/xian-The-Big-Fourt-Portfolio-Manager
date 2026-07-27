package org.example.xianthebigfourtportfoliomanager.dao;


import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class portfolioDaoimpl implements PortfolioDao{

    @Override
    public List<portfolio> findAll() {
        return List.of();
    }

    @Override
    public Optional<portfolio> findById(int id) {
        return Optional.empty();
    }

    @Override
    public portfolio save(portfolio portf) {
        return null;
    }

    @Override
    public portfolio update(portfolio portf) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public boolean existsById(int id) {
        return false;
    }
}
