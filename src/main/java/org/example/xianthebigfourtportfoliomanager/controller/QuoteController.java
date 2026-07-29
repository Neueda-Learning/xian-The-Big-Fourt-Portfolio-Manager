package org.example.xianthebigfourtportfoliomanager.controller;

import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.example.xianthebigfourtportfoliomanager.service.YahooFinanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
public class QuoteController {

    private final YahooFinanceService yahooFinanceService;
    private final PriceHistoryRepository priceHistoryRepository;

    public QuoteController(YahooFinanceService yahooFinanceService,
                           PriceHistoryRepository priceHistoryRepository) {
        this.yahooFinanceService = yahooFinanceService;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @GetMapping("/quotes/yahoo/{ticker}")
    public Map<String, Object> fetchYahooQuote(@PathVariable String ticker) {
        String normalizedTicker = ticker.toUpperCase();

        BigDecimal yahooPrice = yahooFinanceService.getCurrentPrice(normalizedTicker);
        if (yahooPrice != null && yahooPrice.compareTo(BigDecimal.ZERO) > 0) {
            return Map.of(
                    "ticker", normalizedTicker,
                    "price", yahooPrice,
                    "source", "YAHOO"
            );
        }

        priceHistory latest = priceHistoryRepository.getLatestPriceByTicker(normalizedTicker);
        if (latest != null && latest.getCloseprice() != null && latest.getCloseprice().compareTo(BigDecimal.ZERO) > 0) {
            return Map.of(
                    "ticker", normalizedTicker,
                    "price", latest.getCloseprice(),
                    "source", "LOCAL_HISTORY"
            );
        }

        throw new IllegalArgumentException("No valid quote available for ticker: " + normalizedTicker);
    }

    @GetMapping("/quotes/latest/{ticker}")
    public Map<String, Object> fetchLatestQuote(@PathVariable String ticker) {
        return fetchYahooQuote(ticker);
    }
}
