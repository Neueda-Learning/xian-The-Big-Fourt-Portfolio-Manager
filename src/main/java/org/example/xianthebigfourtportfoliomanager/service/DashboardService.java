package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.AssetType;
import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.Transaction;
import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final HoldingService holdingService;
    private final TransactionService transactionService;
    private final PortfolioService portfolioService;
    private final PriceHistoryRepository priceHistoryRepository;
    private final YahooFinanceService yahooFinanceService;

    public DashboardService(
            HoldingService holdingService,
            TransactionService transactionService,
            PortfolioService portfolioService,
            PriceHistoryRepository priceHistoryRepository,
            YahooFinanceService yahooFinanceService
    ) {
        this.holdingService = holdingService;
        this.transactionService = transactionService;
        this.portfolioService = portfolioService;
        this.priceHistoryRepository = priceHistoryRepository;
        this.yahooFinanceService = yahooFinanceService;
    }

    @Transactional(readOnly = true)
    public DashboardOverview getOverview() {
        List<Holding> holdings = holdingService.getAllHoldings();
        List<Transaction> transactions = transactionService.getAllTransactions();
        List<priceHistory> latestPrices = priceHistoryRepository.getLatestPrices();
        List<priceHistory> allPrices = priceHistoryRepository.getAllPrices();

        Map<String, priceHistory> latestByTicker = new LinkedHashMap<>();
        for (priceHistory row : latestPrices) {
            if (row.getTicker() != null && !row.getTicker().isBlank()) {
                latestByTicker.put(row.getTicker().toUpperCase(), row);
            }
        }

        Summary summary = buildSummary(holdings, latestByTicker, transactions.size(), portfolioService.getAllPortfolios().size());
        List<TrendPoint> assetTrend = buildTrendPoints(holdings, allPrices, latestByTicker);

        DashboardOverview overview = new DashboardOverview();
        overview.setSummary(summary);
        overview.setAssetTrend(assetTrend);
        overview.setHoldings(holdings);
        overview.setTransactions(transactions);
        return overview;
    }

    private Summary buildSummary(
            List<Holding> holdings,
            Map<String, priceHistory> latestByTicker,
            int transactionCount,
            int portfolioCount
    ) {
        BigDecimal cashAssets = ZERO;
        BigDecimal stockSpent = ZERO;
        BigDecimal bondSpent = ZERO;
        BigDecimal stockProfitLoss = ZERO;
        BigDecimal bondProfitLoss = ZERO;

        for (Holding holding : holdings) {
            BigDecimal quantity = scale(safe(holding.getQuantity()));
            BigDecimal purchasePrice = scale(safe(holding.getPurchasePrice()));
            BigDecimal cost = scale(quantity.multiply(purchasePrice));

            if (holding.getAssetType() == AssetType.CASH) {
                cashAssets = cashAssets.add(cost);
                continue;
            }

            BigDecimal currentPrice = resolveCurrentPrice(holding, latestByTicker);
            BigDecimal currentValue = scale(quantity.multiply(currentPrice));
            BigDecimal profitLoss = scale(currentValue.subtract(cost));

            if (holding.getAssetType() == AssetType.STOCK) {
                stockSpent = stockSpent.add(cost);
                stockProfitLoss = stockProfitLoss.add(profitLoss);
            } else if (holding.getAssetType() == AssetType.BOND) {
                bondSpent = bondSpent.add(cost);
                bondProfitLoss = bondProfitLoss.add(profitLoss);
            }
        }

        BigDecimal investedAmount = scale(stockSpent.add(bondSpent));
        BigDecimal stockBondProfitLoss = scale(stockProfitLoss.add(bondProfitLoss));
        BigDecimal totalAssets = scale(cashAssets.add(investedAmount).add(stockBondProfitLoss));

        Summary summary = new Summary();
        summary.setPortfolioCount(portfolioCount);
        summary.setHoldingCount(holdings.size());
        summary.setTransactionCount(transactionCount);
        summary.setCashAssets(cashAssets);
        summary.setStockSpent(stockSpent);
        summary.setBondSpent(bondSpent);
        summary.setInvestedAmount(investedAmount);
        summary.setStockProfitLoss(stockProfitLoss);
        summary.setBondProfitLoss(bondProfitLoss);
        summary.setStockBondProfitLoss(stockBondProfitLoss);
        summary.setTotalAssets(totalAssets);
        return summary;
    }

    private List<LatestPriceRow> buildLatestPriceRows(List<Holding> holdings, Map<String, priceHistory> latestByTicker) {
        Map<String, Set<String>> assetTypesByTicker = new TreeMap<>();
        for (Holding holding : holdings) {
            if (holding.getTicker() == null || holding.getTicker().isBlank() || holding.getAssetType() == AssetType.CASH) {
                continue;
            }
            assetTypesByTicker.computeIfAbsent(holding.getTicker().toUpperCase(), key -> new LinkedHashSet<>())
                    .add(holding.getAssetType().name());
        }

        List<LatestPriceRow> rows = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : assetTypesByTicker.entrySet()) {
            priceHistory latest = latestByTicker.get(entry.getKey());
            LatestPriceRow row = new LatestPriceRow();
            row.setTicker(entry.getKey());
            row.setAssetTypes(String.join(", ", entry.getValue()));
            if (latest != null) {
                row.setPriceDate(latest.getPriceDate());
                row.setPriceTime(latest.getPricetime());
                row.setOpenPrice(scaleNullable(latest.getOpenprice()));
                row.setHighPrice(scaleNullable(latest.getHighprice()));
                row.setLowPrice(scaleNullable(latest.getLowprice()));
                row.setClosePrice(scaleNullable(latest.getCloseprice()));
                row.setVolume(latest.getVolume());
                row.setCurrency(latest.getCurrency());
            }
            rows.add(row);
        }
        return rows;
    }

    private List<TrendPoint> buildTrendPoints(
            List<Holding> holdings,
            List<priceHistory> allPrices,
            Map<String, priceHistory> latestByTicker
    ) {
        Map<String, TreeMap<LocalDate, BigDecimal>> pricesByTickerDate = new LinkedHashMap<>();
        TreeSet<LocalDate> allDates = new TreeSet<>();

        for (priceHistory row : allPrices) {
            if (row.getTicker() == null || row.getTicker().isBlank() || row.getPriceDate() == null || row.getCloseprice() == null) {
                continue;
            }
            String ticker = row.getTicker().toUpperCase();
            pricesByTickerDate.computeIfAbsent(ticker, key -> new TreeMap<>());

            LocalDate date = row.getPriceDate();
            if (!pricesByTickerDate.get(ticker).containsKey(date)) {
                BigDecimal close = scale(row.getCloseprice());
                pricesByTickerDate.get(ticker).put(date, close);
            }
            allDates.add(date);
        }

        if (allDates.isEmpty()) {
            allDates.add(LocalDate.now());
        }

        List<LocalDate> dateWindow = new ArrayList<>(allDates);
        if (dateWindow.size() > 30) {
            dateWindow = dateWindow.subList(dateWindow.size() - 30, dateWindow.size());
        }

        List<TrendPoint> points = new ArrayList<>();
        for (LocalDate date : dateWindow) {
            BigDecimal cashValue = ZERO;
            BigDecimal stockValue = ZERO;
            BigDecimal bondValue = ZERO;

            for (Holding holding : holdings) {
                BigDecimal quantity = scale(safe(holding.getQuantity()));
                BigDecimal purchasePrice = scale(safe(holding.getPurchasePrice()));
                if (holding.getAssetType() == AssetType.CASH) {
                    cashValue = cashValue.add(scale(quantity.multiply(purchasePrice)));
                    continue;
                }

                BigDecimal unitPrice = resolvePriceOnDate(holding, date, pricesByTickerDate, latestByTicker);
                BigDecimal value = scale(quantity.multiply(unitPrice));
                if (holding.getAssetType() == AssetType.STOCK) {
                    stockValue = stockValue.add(value);
                } else if (holding.getAssetType() == AssetType.BOND) {
                    bondValue = bondValue.add(value);
                }
            }

            TrendPoint point = new TrendPoint();
            point.setDate(date);
            point.setCash(cashValue);
            point.setStock(stockValue);
            point.setBond(bondValue);
            point.setTotalAssets(scale(cashValue.add(stockValue).add(bondValue)));
            points.add(point);
        }

        points.sort(Comparator.comparing(TrendPoint::getDate));
        return points;
    }

    private BigDecimal resolvePriceOnDate(
            Holding holding,
            LocalDate date,
            Map<String, TreeMap<LocalDate, BigDecimal>> pricesByTickerDate,
            Map<String, priceHistory> latestByTicker
    ) {
        String ticker = normalizeTicker(holding.getTicker());
        if (ticker == null) {
            return scale(safe(holding.getPurchasePrice()));
        }

        TreeMap<LocalDate, BigDecimal> series = pricesByTickerDate.get(ticker);
        if (series != null && series.floorEntry(date) != null) {
            return scale(series.floorEntry(date).getValue());
        }

        priceHistory latest = latestByTicker.get(ticker);
        if (latest != null && latest.getCloseprice() != null) {
            return scale(latest.getCloseprice());
        }

        BigDecimal live = yahooFinanceService.getCurrentPrice(ticker);
        if (live != null) {
            return scale(live);
        }

        return scale(safe(holding.getPurchasePrice()));
    }

    private BigDecimal resolveCurrentPrice(Holding holding, Map<String, priceHistory> latestByTicker) {
        String ticker = normalizeTicker(holding.getTicker());
        if (ticker == null) {
            return scale(safe(holding.getPurchasePrice()));
        }

        priceHistory latest = latestByTicker.get(ticker);
        if (latest != null && latest.getCloseprice() != null) {
            return scale(latest.getCloseprice());
        }

        BigDecimal live = yahooFinanceService.getCurrentPrice(ticker);
        if (live != null) {
            return scale(live);
        }

        return scale(safe(holding.getPurchasePrice()));
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return null;
        }
        return ticker.toUpperCase();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return safe(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleNullable(BigDecimal value) {
        return value == null ? null : scale(value);
    }

    public static class DashboardOverview {
        private Summary summary;
        private List<LatestPriceRow> latestYahooPrices = new ArrayList<>();
        private List<TrendPoint> assetTrend = new ArrayList<>();
        private List<Holding> holdings = new ArrayList<>();
        private List<Transaction> transactions = new ArrayList<>();
        private Map<String, Object> yahooBootstrap = new LinkedHashMap<>();

        public Summary getSummary() {
            return summary;
        }

        public void setSummary(Summary summary) {
            this.summary = summary;
        }

        public List<LatestPriceRow> getLatestYahooPrices() {
            return latestYahooPrices;
        }

        public void setLatestYahooPrices(List<LatestPriceRow> latestYahooPrices) {
            this.latestYahooPrices = latestYahooPrices;
        }

        public List<TrendPoint> getAssetTrend() {
            return assetTrend;
        }

        public void setAssetTrend(List<TrendPoint> assetTrend) {
            this.assetTrend = assetTrend;
        }

        public List<Holding> getHoldings() {
            return holdings;
        }

        public void setHoldings(List<Holding> holdings) {
            this.holdings = holdings;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        public Map<String, Object> getYahooBootstrap() {
            return yahooBootstrap;
        }

        public void setYahooBootstrap(Map<String, Object> yahooBootstrap) {
            this.yahooBootstrap = yahooBootstrap;
        }
    }

    public static class Summary {
        private int portfolioCount;
        private int holdingCount;
        private int transactionCount;
        private BigDecimal cashAssets;
        private BigDecimal stockSpent;
        private BigDecimal bondSpent;
        private BigDecimal investedAmount;
        private BigDecimal stockProfitLoss;
        private BigDecimal bondProfitLoss;
        private BigDecimal stockBondProfitLoss;
        private BigDecimal totalAssets;

        public int getPortfolioCount() {
            return portfolioCount;
        }

        public void setPortfolioCount(int portfolioCount) {
            this.portfolioCount = portfolioCount;
        }

        public int getHoldingCount() {
            return holdingCount;
        }

        public void setHoldingCount(int holdingCount) {
            this.holdingCount = holdingCount;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
        }

        public BigDecimal getCashAssets() {
            return cashAssets;
        }

        public void setCashAssets(BigDecimal cashAssets) {
            this.cashAssets = cashAssets;
        }

        public BigDecimal getStockSpent() {
            return stockSpent;
        }

        public void setStockSpent(BigDecimal stockSpent) {
            this.stockSpent = stockSpent;
        }

        public BigDecimal getBondSpent() {
            return bondSpent;
        }

        public void setBondSpent(BigDecimal bondSpent) {
            this.bondSpent = bondSpent;
        }

        public BigDecimal getInvestedAmount() {
            return investedAmount;
        }

        public void setInvestedAmount(BigDecimal investedAmount) {
            this.investedAmount = investedAmount;
        }

        public BigDecimal getStockProfitLoss() {
            return stockProfitLoss;
        }

        public void setStockProfitLoss(BigDecimal stockProfitLoss) {
            this.stockProfitLoss = stockProfitLoss;
        }

        public BigDecimal getBondProfitLoss() {
            return bondProfitLoss;
        }

        public void setBondProfitLoss(BigDecimal bondProfitLoss) {
            this.bondProfitLoss = bondProfitLoss;
        }

        public BigDecimal getStockBondProfitLoss() {
            return stockBondProfitLoss;
        }

        public void setStockBondProfitLoss(BigDecimal stockBondProfitLoss) {
            this.stockBondProfitLoss = stockBondProfitLoss;
        }

        public BigDecimal getTotalAssets() {
            return totalAssets;
        }

        public void setTotalAssets(BigDecimal totalAssets) {
            this.totalAssets = totalAssets;
        }
    }

    public static class LatestPriceRow {
        private String ticker;
        private String assetTypes;
        private LocalDate priceDate;
        private java.time.LocalDateTime priceTime;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal closePrice;
        private Long volume;
        private String currency;

        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }

        public String getAssetTypes() {
            return assetTypes;
        }

        public void setAssetTypes(String assetTypes) {
            this.assetTypes = assetTypes;
        }

        public LocalDate getPriceDate() {
            return priceDate;
        }

        public void setPriceDate(LocalDate priceDate) {
            this.priceDate = priceDate;
        }

        public java.time.LocalDateTime getPriceTime() {
            return priceTime;
        }

        public void setPriceTime(java.time.LocalDateTime priceTime) {
            this.priceTime = priceTime;
        }

        public BigDecimal getOpenPrice() {
            return openPrice;
        }

        public void setOpenPrice(BigDecimal openPrice) {
            this.openPrice = openPrice;
        }

        public BigDecimal getHighPrice() {
            return highPrice;
        }

        public void setHighPrice(BigDecimal highPrice) {
            this.highPrice = highPrice;
        }

        public BigDecimal getLowPrice() {
            return lowPrice;
        }

        public void setLowPrice(BigDecimal lowPrice) {
            this.lowPrice = lowPrice;
        }

        public BigDecimal getClosePrice() {
            return closePrice;
        }

        public void setClosePrice(BigDecimal closePrice) {
            this.closePrice = closePrice;
        }

        public Long getVolume() {
            return volume;
        }

        public void setVolume(Long volume) {
            this.volume = volume;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

    public static class TrendPoint {
        private LocalDate date;
        private BigDecimal cash;
        private BigDecimal stock;
        private BigDecimal bond;
        private BigDecimal totalAssets;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getCash() {
            return cash;
        }

        public void setCash(BigDecimal cash) {
            this.cash = cash;
        }

        public BigDecimal getStock() {
            return stock;
        }

        public void setStock(BigDecimal stock) {
            this.stock = stock;
        }

        public BigDecimal getBond() {
            return bond;
        }

        public void setBond(BigDecimal bond) {
            this.bond = bond;
        }

        public BigDecimal getTotalAssets() {
            return totalAssets;
        }

        public void setTotalAssets(BigDecimal totalAssets) {
            this.totalAssets = totalAssets;
        }
    }
}

