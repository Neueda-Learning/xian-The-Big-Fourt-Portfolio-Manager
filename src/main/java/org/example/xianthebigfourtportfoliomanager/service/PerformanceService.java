package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.repository.HoldingRepository;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.example.xianthebigfourtportfoliomanager.repository.PortfolioRepository;
import org.example.xianthebigfourtportfoliomanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PerformanceService {

    /**
     * Eren issue: holding had no explicit currentPrice column, which mixed average cost and market price semantics.
     * Fix: use averagePrice for cost basis and currentPrice (manual/local quote/Yahoo) for valuation.
     * Reviewer: GitHub Copilot (GPT-5.3-Codex).
     */

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TransactionRepository transactionRepository;

    public PerformanceService(PortfolioRepository portfolioRepository,
                              HoldingRepository holdingRepository,
                              PriceHistoryRepository priceHistoryRepository,
                              TransactionRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public PerformanceResult getPerformance(int portfolioId) {
        portfolio portf = portfolioRepository.getPortfolioById(portfolioId);
        if (portf == null) return null;

        List<Holding> holdings = holdingRepository.getHoldingsByPortfolioId(portfolioId);
        Map<Integer, List<Transaction>> transactionsByHolding = buildTransactionsByHolding(portfolioId);
        BigDecimal holdingsMarketValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<HoldingDetail> details = new ArrayList<>();

        for (Holding h : holdings) {
            if (h.getAssetType() == org.example.xianthebigfourtportfoliomanager.entity.AssetType.CASH
                    || "CASH".equalsIgnoreCase(Objects.toString(h.getTicker(), ""))) {
                continue;
            }

            BigDecimal currentPrice = resolveCurrentPrice(h);
            if (currentPrice == null) currentPrice = h.getAveragePrice();
            if (currentPrice == null) currentPrice = BigDecimal.ZERO;

            BigDecimal marketValue = h.getQuantity().multiply(currentPrice);
            BigDecimal cost = h.getQuantity().multiply(
                h.getAveragePrice() != null ? h.getAveragePrice() : BigDecimal.ZERO
            );
            HoldingPnl pnl = computeHoldingPnl(transactionsByHolding.get(h.getId()), cost, marketValue);
            holdingsMarketValue = holdingsMarketValue.add(marketValue);
            totalCost = totalCost.add(cost);

            details.add(new HoldingDetail(
                h.getId(), h.getTicker(), h.getAssetType().name(),
                h.getQuantity(), h.getAveragePrice(), currentPrice,
                marketValue, cost,
                pnl.realizedPnl,
                pnl.unrealizedPnl,
                pnl.totalPnl
            ));
        }

        portfolio firstPortfolio = portfolioRepository.getPortfolioById(1);
        BigDecimal cashBalance = (firstPortfolio != null && firstPortfolio.getCashBalance() != null) 
            ? firstPortfolio.getCashBalance() 
            : (portf.getCashBalance() == null ? BigDecimal.ZERO : portf.getCashBalance());

        if (holdingsMarketValue.compareTo(BigDecimal.ZERO) <= 0) {
            return new PerformanceResult(portfolioId, portf.getName(),
                null, null, null, null, details, cashBalance, holdingsMarketValue);
        }
        BigDecimal initialCash = portf.getInitialCash() == null ? BigDecimal.ZERO : portf.getInitialCash();
        BigDecimal totalPortfolioValue = totalCost.add(totalCost.compareTo(BigDecimal.ZERO) > 0 ? holdingsMarketValue.subtract(totalCost) : BigDecimal.ZERO);

        BigDecimal totalReturn = holdingsMarketValue.subtract(totalCost);
        BigDecimal returnRate = totalCost.compareTo(BigDecimal.ZERO) > 0
            ? totalReturn.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
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

        if (holding.getCurrentPrice() != null) {
            return holding.getCurrentPrice();
        }

        priceHistory latest = priceHistoryRepository.getLatestPriceByTicker(ticker.toUpperCase());
        if (latest != null && latest.getCloseprice() != null) {
            return latest.getCloseprice();
        }

        return null;
    }

    private Map<Integer, List<Transaction>> buildTransactionsByHolding(int portfolioId) {
        List<Transaction> transactions = transactionRepository.getTransactionsByPortfolioId(portfolioId);
        Map<Integer, List<Transaction>> byHolding = new HashMap<>();
        for (Transaction tx : transactions) {
            if (tx == null || tx.getHoldingId() == null) {
                continue;
            }
            byHolding.computeIfAbsent(tx.getHoldingId(), key -> new ArrayList<>()).add(tx);
        }

        for (List<Transaction> txList : byHolding.values()) {
            txList.sort(Comparator
                    .comparing(Transaction::getTradeDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(tx -> tx.getId() == null ? Integer.MAX_VALUE : tx.getId()));
        }

        return byHolding;
    }

    private HoldingPnl computeHoldingPnl(List<Transaction> transactions, BigDecimal currentCost, BigDecimal marketValue) {
        BigDecimal realized = BigDecimal.ZERO;
        BigDecimal openQty = BigDecimal.ZERO;
        BigDecimal openCostPool = BigDecimal.ZERO;

        if (transactions != null) {
            for (Transaction tx : transactions) {
                if (tx == null || tx.getQuantity() == null || tx.getPrice() == null || tx.getType() == null) {
                    continue;
                }

                String type = tx.getType().trim().toUpperCase();
                BigDecimal qty = tx.getQuantity().max(BigDecimal.ZERO);
                BigDecimal price = tx.getPrice().max(BigDecimal.ZERO);

                if ("BUY".equals(type)) {
                    openQty = openQty.add(qty);
                    openCostPool = openCostPool.add(qty.multiply(price));
                } else if ("SELL".equals(type)) {
                    if (openQty.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    BigDecimal sellQty = qty.min(openQty);
                    if (sellQty.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    BigDecimal avgCost = openCostPool.divide(openQty, 8, RoundingMode.HALF_UP);
                    BigDecimal sellPnl = price.subtract(avgCost).multiply(sellQty);
                    realized = realized.add(sellPnl);

                    openQty = openQty.subtract(sellQty);
                    openCostPool = openCostPool.subtract(avgCost.multiply(sellQty));
                }
            }
        }

        BigDecimal unrealized = marketValue.subtract(currentCost);
        BigDecimal total = realized.add(unrealized);

        return new HoldingPnl(
                realized.setScale(4, RoundingMode.HALF_UP),
                unrealized.setScale(4, RoundingMode.HALF_UP),
                total.setScale(4, RoundingMode.HALF_UP)
        );
    }

    private record HoldingPnl(BigDecimal realizedPnl, BigDecimal unrealizedPnl, BigDecimal totalPnl) {
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
        private BigDecimal averagePrice;
        private BigDecimal currentPrice;
        private BigDecimal marketValue;
        private BigDecimal cost;
        private BigDecimal realizedPnl;
        private BigDecimal unrealizedPnl;
        private BigDecimal totalPnl;

        public HoldingDetail() {}

        public HoldingDetail(int holdingId, String ticker, String assetType,
                             BigDecimal quantity, BigDecimal averagePrice, BigDecimal currentPrice,
                             BigDecimal marketValue, BigDecimal cost,
                             BigDecimal realizedPnl, BigDecimal unrealizedPnl, BigDecimal totalPnl) {
            this.holdingId = holdingId;
            this.ticker = ticker;
            this.assetType = assetType;
            this.quantity = quantity;
            this.averagePrice = averagePrice;
            this.currentPrice = currentPrice;
            this.marketValue = marketValue;
            this.cost = cost;
            this.realizedPnl = realizedPnl;
            this.unrealizedPnl = unrealizedPnl;
            this.totalPnl = totalPnl;
        }

        public int getHoldingId() { return holdingId; }
        public void setHoldingId(int holdingId) { this.holdingId = holdingId; }
        public String getTicker() { return ticker; }
        public void setTicker(String ticker) { this.ticker = ticker; }
        public String getAssetType() { return assetType; }
        public void setAssetType(String assetType) { this.assetType = assetType; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
        public BigDecimal getMarketValue() { return marketValue; }
        public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
        public BigDecimal getRealizedPnl() { return realizedPnl; }
        public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
        public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
        public void setUnrealizedPnl(BigDecimal unrealizedPnl) { this.unrealizedPnl = unrealizedPnl; }
        public BigDecimal getTotalPnl() { return totalPnl; }
        public void setTotalPnl(BigDecimal totalPnl) { this.totalPnl = totalPnl; }
    }
}
