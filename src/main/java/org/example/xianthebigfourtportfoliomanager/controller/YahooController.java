package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.Holding;
import org.example.xianthebigfourtportfoliomanager.entity.portfolio;
import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.service.HoldingService;
import org.example.xianthebigfourtportfoliomanager.service.PortfolioService;
import org.example.xianthebigfourtportfoliomanager.service.YahooFinanceService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class YahooController {

    private final YahooFinanceService yahooFinanceService;
    private final HoldingService holdingService;
    private final PortfolioService portfolioService;

    public YahooController(
            YahooFinanceService yahooFinanceService,
            HoldingService holdingService,
            PortfolioService portfolioService
    ) {
        this.yahooFinanceService = yahooFinanceService;
        this.holdingService = holdingService;
        this.portfolioService = portfolioService;
    }

    @PostMapping("/yahoo/sync/ticker/{ticker}")
    public Map<String, Object> syncTicker(@PathVariable String ticker) {
        priceHistory saved = yahooFinanceService.fetchAndStoreCurrentPrice(ticker);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticker", ticker == null ? null : ticker.toUpperCase());
        result.put("saved", saved != null);
        result.put("record", saved);
        return result;
    }

    @PostMapping("/yahoo/sync/portfolio/{portfolioId}")
    public Map<String, Object> syncPortfolio(@PathVariable int portfolioId) {
        List<Holding> holdings = holdingService.getHoldingsByPortfolioId(portfolioId);
        Set<String> tickers = new LinkedHashSet<>();
        for (Holding h : holdings) {
            if (h.getTicker() != null && !h.getTicker().isBlank()) {
                tickers.add(h.getTicker().toUpperCase());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int success = 0;
        for (String ticker : tickers) {
            priceHistory saved = yahooFinanceService.fetchAndStoreCurrentPrice(ticker);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ticker", ticker);
            row.put("saved", saved != null);
            row.put("record", saved);
            rows.add(row);
            if (saved != null) {
                success++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("portfolioId", portfolioId);
        result.put("tickerCount", tickers.size());
        result.put("savedCount", success);
        result.put("items", rows);
        return result;
    }

    @PostMapping("/yahoo/sync/all")
    public Map<String, Object> syncAll() {
        List<portfolio> portfolios = portfolioService.getAllPortfolios();
        Set<String> tickers = new LinkedHashSet<>();

        for (portfolio p : portfolios) {
            List<Holding> holdings = holdingService.getHoldingsByPortfolioId(p.getId());
            for (Holding h : holdings) {
                if (h.getTicker() != null && !h.getTicker().isBlank()) {
                    tickers.add(h.getTicker().toUpperCase());
                }
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int success = 0;
        for (String ticker : tickers) {
            priceHistory saved = yahooFinanceService.fetchAndStoreCurrentPrice(ticker);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ticker", ticker);
            row.put("saved", saved != null);
            row.put("record", saved);
            rows.add(row);
            if (saved != null) {
                success++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("portfolioCount", portfolios.size());
        result.put("tickerCount", tickers.size());
        result.put("savedCount", success);
        result.put("items", rows);
        return result;
    }
}

