package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public List<portfolio> getAllPortfolios() {
        return portfolioRepository.getAllPortfolios();
    }

    public portfolio getPortfolioById(int id) {
        return portfolioRepository.getPortfolioById(id);
    }

    public boolean existsById(int id) {
        return portfolioRepository.existsById(id);
    }

    @Transactional
    public portfolio create(portfolio portf) {
        return portfolioRepository.save(portf);
    }

    @Transactional
    public portfolio update(portfolio portf) {
        return portfolioRepository.update(portf);
    }

    @Transactional
    public int deleteById(int id) {
        return portfolioRepository.deleteById(id);
    }
}
