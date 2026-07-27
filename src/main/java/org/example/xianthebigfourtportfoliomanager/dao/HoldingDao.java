package org.example.xianthebigfourtportfoliomanager.dao;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;

import java.util.List;

public interface HoldingDao {
    List<Holding>  findByPortfolioId(int portfolioId) ;
    Holding findById(int id);
    Holding save(Holding holding);
    Holding update(Holding holding);
    void deletebyid(int id);
    boolean existbyid(int id);}
