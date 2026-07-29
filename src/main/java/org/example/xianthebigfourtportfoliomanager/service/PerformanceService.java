package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.example.xianthebigfourtportfoliomanager.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PerformanceService {

    /**
     * Eren issue: gain/loss could remain zero when live quotes failed, because valuation fell back too quickly to purchase cost.
     * Fix: resolve current price with this order: local latest close -> Yahoo quote -> purchase price.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final YahooFinanceService yahooFinanceService;

    public PerformanceService(PortfolioRepository portfolioRepository,
                              HoldingRepository holdingRepository,
                              PriceHistoryRepository priceHistoryRepository,
                              YahooFinanceService yahooFinanceService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.yahooFinanceService = yahooFinanceService;
    }

    public PerformanceResult getPerformance(int portfolioId) {
        portfolio portf = portfolioRepository.getPortfolioById(portfolioId);
        if (portf == null) return null;

        List<Holding> holdings = holdingRepository.getHoldingsByPortfolioId(portfolioId);
        BigDecimal holdingsMarketValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<HoldingDetail> details = new ArrayList<>();

        for (Holding h : holdings) {
            if (h.getAssetType() == org.example.xianthebigfourtportfoliomanager.entity.AssetType.CASH
                    || "CASH".equalsIgnoreCase(Objects.toString(h.getTicker(), ""))) {
                continue;
            }

            BigDecimal currentPrice = resolveCurrentPrice(h);
            if (currentPrice == null) currentPrice = h.getPurchasePrice();
            if (currentPrice == null) currentPrice = BigDecimal.ZERO;

            BigDecimal marketValue = h.getQuantity().multiply(currentPrice);
            BigDecimal cost = h.getQuantity().multiply(
                h.getPurchasePrice() != null ? h.getPurchasePrice() : BigDecimal.ZERO
            );
            holdingsMarketValue = holdingsMarketValue.add(marketValue);
            totalCost = totalCost.add(cost);

            details.add(new HoldingDetail(
                h.getId(), h.getTicker(), h.getAssetType().name(),
                h.getQuantity(), h.getPurchasePrice(), currentPrice,
                marketValue, cost
            ));
        }

        BigDecimal cashBalance = portf.getCashBalance() == null ? BigDecimal.ZERO : portf.getCashBalance();
        BigDecimal initialCash = portf.getInitialCash() == null ? BigDecimal.ZERO : portf.getInitialCash();
        BigDecimal totalPortfolioValue = cashBalance.add(holdingsMarketValue);

        BigDecimal totalReturn = totalPortfolioValue.subtract(initialCash);
        BigDecimal returnRate = initialCash.compareTo(BigDecimal.ZERO) > 0
            ? totalReturn.divide(initialCash, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        return new PerformanceResult(portfolioId, portf.getName(),
            totalPortfolioValue, totalCost, totalReturn, returnRate, details, cashBalance, holdingsMarketValue);
    }

    private BigDecimal resolveCurrentPrice(Holding holding) {
        if (holding == null) {
            return null;
        }

        String ticker = holding.getTicker();
        if (ticker == null || ticker.isBlank()) {
            return null;
        }

        priceHistory latest = priceHistoryRepository.getLatestPriceByTicker(ticker.toUpperCase());
        if (latest != null && latest.getCloseprice() != null) {
            return latest.getCloseprice();
        }

        return yahooFinanceService.getCurrentPrice(ticker);
    }

    public static class PerformanceResult {
        private int portfolioId;
        private String portfolioName;
        private BigDecimal totalMarketValue;
        private BigDecimal totalCost;
        private BigDecimal totalReturn;
        private BigDecimal returnRate;
        private List<HoldingDetail> holdingsDetail;
        private BigDecimal cashBalance;
        private BigDecimal holdingsMarketValue;

        public PerformanceResult() {}

        public PerformanceResult(int portfolioId, String portfolioName,
                                 BigDecimal totalMarketValue, BigDecimal totalCost,
                                 BigDecimal totalReturn, BigDecimal returnRate,
                                 List<HoldingDetail> holdingsDetail,
                                 BigDecimal cashBalance,
                                 BigDecimal holdingsMarketValue) {
            this.portfolioId = portfolioId;
            this.portfolioName = portfolioName;
            this.totalMarketValue = totalMarketValue;
            this.totalCost = totalCost;
            this.totalReturn = totalReturn;
            this.returnRate = returnRate;
            this.holdingsDetail = holdingsDetail;
            this.cashBalance = cashBalance;
            this.holdingsMarketValue = holdingsMarketValue;
        }

        public int getPortfolioId() { return portfolioId; }
        public void setPortfolioId(int portfolioId) { this.portfolioId = portfolioId; }
        public String getPortfolioName() { return portfolioName; }
        public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
        public BigDecimal getTotalMarketValue() { return totalMarketValue; }
        public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        public BigDecimal getTotalReturn() { return totalReturn; }
        public void setTotalReturn(BigDecimal totalReturn) { this.totalReturn = totalReturn; }
        public BigDecimal getReturnRate() { return returnRate; }
        public void setReturnRate(BigDecimal returnRate) { this.returnRate = returnRate; }
        public List<HoldingDetail> getHoldingsDetail() { return holdingsDetail; }
        public void setHoldingsDetail(List<HoldingDetail> holdingsDetail) { this.holdingsDetail = holdingsDetail; }
        public BigDecimal getCashBalance() { return cashBalance; }
        public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
        public BigDecimal getHoldingsMarketValue() { return holdingsMarketValue; }
        public void setHoldingsMarketValue(BigDecimal holdingsMarketValue) { this.holdingsMarketValue = holdingsMarketValue; }
    }

    public static class HoldingDetail {
        private int holdingId;
        private String ticker;
        private String assetType;
        private BigDecimal quantity;
        private BigDecimal purchasePrice;
        private BigDecimal currentPrice;
        private BigDecimal marketValue;
        private BigDecimal cost;

        public HoldingDetail() {}

        public HoldingDetail(int holdingId, String ticker, String assetType,
                             BigDecimal quantity, BigDecimal purchasePrice, BigDecimal currentPrice,
                             BigDecimal marketValue, BigDecimal cost) {
            this.holdingId = holdingId;
            this.ticker = ticker;
            this.assetType = assetType;
            this.quantity = quantity;
            this.purchasePrice = purchasePrice;
            this.currentPrice = currentPrice;
            this.marketValue = marketValue;
            this.cost = cost;
        }

        public int getHoldingId() { return holdingId; }
        public void setHoldingId(int holdingId) { this.holdingId = holdingId; }
        public String getTicker() { return ticker; }
        public void setTicker(String ticker) { this.ticker = ticker; }
        public String getAssetType() { return assetType; }
        public void setAssetType(String assetType) { this.assetType = assetType; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getPurchasePrice() { return purchasePrice; }
        public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
        public BigDecimal getMarketValue() { return marketValue; }
        public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
    }
}
