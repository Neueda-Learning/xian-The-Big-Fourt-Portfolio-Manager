package org.example.xianthebigfourtportfoliomanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class YahooFinanceService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final Map<String, BigDecimal> priceCache = new ConcurrentHashMap<>();

    public YahooFinanceService(@Value("${yahoo.finance.base-url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    public BigDecimal getCurrentPrice(String ticker) {
        if (ticker == null || ticker.isBlank()) return null;
        BigDecimal cached = priceCache.get(ticker.toUpperCase());
        if (cached != null) return cached;

        try {
            String url = baseUrl + "?ticker=" + ticker.toUpperCase();
            String response = restTemplate.getForObject(url, String.class);
            BigDecimal price = parsePrice(response);
            if (price != null) {
                priceCache.put(ticker.toUpperCase(), price);
            }
            return price;
        } catch (Exception e) {
            return null;
        }
    }

    public void clearCache() {
        priceCache.clear();
    }

    private BigDecimal parsePrice(String response) {
        if (response == null || response.isBlank()) return null;
        try {
            String trimmed = response.trim();
            if (trimmed.startsWith("{")) {
                int idx = trimmed.indexOf(":");
                if (idx > 0) {
                    String val = trimmed.substring(idx + 1).replaceAll("[^0-9.]", "");
                    return new BigDecimal(val);
                }
            }
            return new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
