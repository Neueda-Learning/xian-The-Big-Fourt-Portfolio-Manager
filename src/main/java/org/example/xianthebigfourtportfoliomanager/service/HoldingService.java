package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HoldingService {

    private final HoldingRepository holdingRepository;

    public HoldingService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    public List<Holding> getHoldingsByPortfolioId(int portfolioId) {
        return holdingRepository.getHoldingsByPortfolioId(portfolioId);
    }

    public Holding getHoldingById(int holdingId) {
        return holdingRepository.getHoldingById(holdingId);
    }

    @Transactional(readOnly = true)
    public List<Holding> getAllHoldings() {
        return holdingRepository.getAllHoldings();
    }

    public boolean existsById(int holdingId) {
        return holdingRepository.existsById(holdingId);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctTickers() {
        return holdingRepository.getDistinctTickers();
    }

    @Transactional
    public Holding create(Holding holding) {
        return holdingRepository.save(holding);
    }

    @Transactional
    public Holding update(Holding holding) {
        return holdingRepository.update(holding);
    }

    @Transactional
    public int deleteById(int holdingId) {
        return holdingRepository.deleteById(holdingId);
    }
}
