package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.PortfolioSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final PortfolioSnapshotService portfolioSnapshotService;

    public PortfolioSummaryService(PerformanceService performanceService,
                                   PortfolioSnapshotService portfolioSnapshotService) {
        this.performanceService = performanceService;
        this.portfolioSnapshotService = portfolioSnapshotService;
    }

    public PortfolioSummaryResponse getSummary(int portfolioId) {
        PerformanceService.PerformanceResult perf = performanceService.getPerformance(portfolioId);
        if (perf == null) {
            return null;
        }

        List<AllocationItem> allocation = buildAllocation(perf.getHoldingsDetail(), perf.getHoldingsMarketValue());

        DayChange dayChange = computeDayChange(portfolioId);

        return new PortfolioSummaryResponse(
                perf.getPortfolioId(),
                perf.getPortfolioName(),
                defaultZero(perf.getTotalMarketValue()),
                defaultZero(perf.getTotalReturn()),
                defaultZero(perf.getReturnRate()),
                defaultZero(perf.getCashBalance()),
                dayChange.dayChangeAmount,
                dayChange.dayChangePercent,
                dayChange.reliable,
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

    private DayChange computeDayChange(int portfolioId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<PortfolioSnapshot> rows = portfolioSnapshotService.getSnapshots(portfolioId, yesterday, today);
        if (rows == null || rows.isEmpty()) {
            return new DayChange(null, null, false);
        }

        PortfolioSnapshot todayRow = null;
        PortfolioSnapshot yesterdayRow = null;
        for (PortfolioSnapshot row : rows) {
            if (row == null || row.getSnapshotDate() == null) {
                continue;
            }
            if (today.equals(row.getSnapshotDate())) {
                todayRow = row;
            } else if (yesterday.equals(row.getSnapshotDate())) {
                yesterdayRow = row;
            }
        }

        if (todayRow == null || yesterdayRow == null) {
            return new DayChange(null, null, false);
        }

        BigDecimal todayValue = defaultZero(todayRow.getTotalValue());
        BigDecimal yesterdayValue = defaultZero(yesterdayRow.getTotalValue());
        BigDecimal amount = todayValue.subtract(yesterdayValue).setScale(4, RoundingMode.HALF_UP);

        BigDecimal pct = null;
        if (yesterdayValue.compareTo(ZERO) > 0) {
            pct = amount.divide(yesterdayValue, 6, RoundingMode.HALF_UP).multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP);
        }

        return new DayChange(amount, pct, true);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private record DayChange(BigDecimal dayChangeAmount, BigDecimal dayChangePercent, boolean reliable) {
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
            BigDecimal dayChangeAmount,
            BigDecimal dayChangePercentage,
            boolean dayChangeReliable,
            List<AllocationItem> allocation
    ) {
    }
}
