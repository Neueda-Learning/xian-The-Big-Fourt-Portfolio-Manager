package org.example.xianthebigfourtportfoliomanager.service;

import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class YahooBootstrapService {

    private final HoldingService holdingService;
    private final YahooFinanceService yahooFinanceService;

    private volatile boolean bootstrapCompleted;
    private volatile LocalDateTime bootstrapAt;
    private volatile int tickerCount;
    private volatile int savedCount;
    private volatile List<Map<String, Object>> bootstrapItems = new ArrayList<>();

    public YahooBootstrapService(HoldingService holdingService, YahooFinanceService yahooFinanceService) {
        this.holdingService = holdingService;
        this.yahooFinanceService = yahooFinanceService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncYahooDataOnStartup() {
        List<String> tickers = holdingService.getDistinctTickers();
        List<Map<String, Object>> rows = new ArrayList<>();
        int success = 0;

        for (String ticker : tickers) {
            priceHistory saved = yahooFinanceService.fetchAndStoreCurrentPrice(ticker);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ticker", ticker);
            item.put("saved", saved != null);
            item.put("priceDate", saved == null ? null : saved.getPriceDate());
            item.put("priceTime", saved == null ? null : saved.getPricetime());
            item.put("closePrice", saved == null ? null : saved.getCloseprice());
            rows.add(item);
            if (saved != null) {
                success++;
            }
        }

        this.tickerCount = tickers.size();
        this.savedCount = success;
        this.bootstrapItems = rows;
        this.bootstrapAt = LocalDateTime.now();
        this.bootstrapCompleted = true;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBootstrapStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completed", bootstrapCompleted);
        result.put("bootstrapAt", bootstrapAt);
        result.put("tickerCount", tickerCount);
        result.put("savedCount", savedCount);
        result.put("items", bootstrapItems);
        return result;
    }
}

