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
    private final PortfolioSnapshotService portfolioSnapshotService;

    public HoldingService(HoldingRepository holdingRepository,
                          PortfolioSnapshotService portfolioSnapshotService) {
        this.holdingRepository = holdingRepository;
        this.portfolioSnapshotService = portfolioSnapshotService;
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

        holding.setCurrentPrice(currentPrice.setScale(4, RoundingMode.HALF_UP));
        Holding updated = holdingRepository.update(holding);
        if (updated != null && updated.getPortfolioId() != null) {
            portfolioSnapshotService.captureToday(updated.getPortfolioId());
        }
        return updated;
    }
}
