package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public boolean existsById(int holdingId) {
        return holdingRepository.existsById(holdingId);
    }

    @Transactional
    public Holding updateCurrentPrice(int holdingId, BigDecimal currentPrice) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("currentPrice must be greater than 0.");
        }

        Holding holding = holdingRepository.getHoldingById(holdingId);
        if (holding == null) {
            throw new IllegalArgumentException("Holding not found: " + holdingId);
        }

        // In the current schema, purchase_price is used as the stored manual price.
        holding.setPurchasePrice(currentPrice.setScale(4, RoundingMode.HALF_UP));
        return holdingRepository.update(holding);
    }
}
