package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.PortfolioSnapshot;
import org.example.xianthebigfourtportfoliomanager.repository.PortfolioSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PortfolioSnapshotService {

    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final PerformanceService performanceService;

    public PortfolioSnapshotService(PortfolioSnapshotRepository portfolioSnapshotRepository,
                                    PerformanceService performanceService) {
        this.portfolioSnapshotRepository = portfolioSnapshotRepository;
        this.performanceService = performanceService;
    }

    @Transactional
    public void captureToday(int portfolioId) {
        PerformanceService.PerformanceResult perf = performanceService.getPerformance(portfolioId);
        if (perf == null || perf.getTotalMarketValue() == null || perf.getCashBalance() == null || perf.getTotalReturn() == null) {
            return;
        }

        portfolioSnapshotRepository.upsertDailySnapshot(
                portfolioId,
                LocalDate.now(),
                perf.getTotalMarketValue(),
                perf.getCashBalance(),
                perf.getTotalReturn()
        );
    }

    public List<PortfolioSnapshot> getSnapshots(int portfolioId, LocalDate startDate, LocalDate endDate) {
        return portfolioSnapshotRepository.getByPortfolioAndRange(portfolioId, startDate, endDate);
    }
}
