package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        if (portf == null) {
            throw new IllegalArgumentException("Portfolio payload is required.");
        }

        String name = normalizeName(portf.getName());
        BigDecimal initialCash = normalizeInitialCash(portf.getInitialCash());

        portf.setName(name);
        portf.setDescription(normalizeDescription(portf.getDescription()));
        portf.setInitialCash(initialCash);
        portf.setCashBalance(initialCash);

        return portfolioRepository.save(portf);
    }

    @Transactional
    public portfolio update(portfolio portf) {
        if (portf == null || portf.getId() == null) {
            throw new IllegalArgumentException("Portfolio id is required for update.");
        }

        portfolio existing = portfolioRepository.getPortfolioById(portf.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Portfolio not found: " + portf.getId());
        }

        existing.setName(normalizeName(portf.getName()));
        existing.setDescription(normalizeDescription(portf.getDescription()));

        return portfolioRepository.update(existing);
    }

    @Transactional
    public int deleteById(int id) {
        return portfolioRepository.deleteById(id);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Portfolio name is required.");
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private BigDecimal normalizeInitialCash(BigDecimal initialCash) {
        BigDecimal normalized = initialCash == null ? BigDecimal.ZERO : initialCash;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("initialCash cannot be negative.");
        }
        return normalized;
    }
}
