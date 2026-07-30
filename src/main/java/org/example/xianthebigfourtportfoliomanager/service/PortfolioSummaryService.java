package org.example.xianthebigfourtportfoliomanager.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioSummaryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PerformanceService performanceService;

    public PortfolioSummaryService(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    public PortfolioSummaryResponse getSummary(int portfolioId) {
        PerformanceService.PerformanceResult perf = performanceService.getPerformance(portfolioId);
        if (perf == null) {
            return null;
        }

        List<AllocationItem> allocation = buildAllocation(perf.getHoldingsDetail(), perf.getHoldingsMarketValue());

        if (perf.getTotalMarketValue() == null) {
            return new PortfolioSummaryResponse(
                    perf.getPortfolioId(),
                    perf.getPortfolioName(),
                    null,
                    null,
                    null,
                    defaultZero(perf.getCashBalance()),
                    allocation
            );
        }

        return new PortfolioSummaryResponse(
                perf.getPortfolioId(),
                perf.getPortfolioName(),
                defaultZero(perf.getTotalMarketValue()),
                defaultZero(perf.getTotalReturn()),
                defaultZero(perf.getReturnRate()),
                defaultZero(perf.getCashBalance()),
                allocation
        );
    }

    private List<AllocationItem> buildAllocation(List<PerformanceService.HoldingDetail> details, BigDecimal holdingsMarketValue) {
        BigDecimal total = defaultZero(holdingsMarketValue);
        if (details == null || details.isEmpty() || total.compareTo(ZERO) <= 0) {
            return List.of();
        }

        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        for (PerformanceService.HoldingDetail detail : details) {
            if (detail == null || detail.getAssetType() == null) {
                continue;
            }
            BigDecimal value = defaultZero(detail.getMarketValue());
            grouped.put(detail.getAssetType(), grouped.getOrDefault(detail.getAssetType(), ZERO).add(value));
        }

        List<AllocationItem> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : grouped.entrySet()) {
            BigDecimal pct = entry.getValue().divide(total, 6, RoundingMode.HALF_UP).multiply(HUNDRED);
            result.add(new AllocationItem(
                    entry.getKey(),
                    entry.getValue().setScale(4, RoundingMode.HALF_UP),
                    pct.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        result.sort(Comparator.comparing(AllocationItem::assetType));
        return result;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    public record AllocationItem(String assetType, BigDecimal value, BigDecimal percentage) {
    }

    public record PortfolioSummaryResponse(
            int portfolioId,
            String portfolioName,
            BigDecimal totalValue,
            BigDecimal totalGain,
            BigDecimal totalGainPercentage,
            BigDecimal cashBalance,
            List<AllocationItem> allocation
    ) {
    }
}
