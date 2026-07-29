package org.example.xianthebigfourtportfoliomanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            if (trimmed.matches("^-?\\d+(\\.\\d+)?$")) {
                return new BigDecimal(trimmed);
            }

            BigDecimal keyed = extractByKnownKeys(trimmed);
            if (keyed != null) {
                return keyed;
            }

            String body = extractJsonStringField(trimmed, "body");
            if (body != null && !body.isBlank()) {
                String unescapedBody = body.replace("\\\"", "\"");
                BigDecimal bodyPrice = parsePrice(unescapedBody);
                if (bodyPrice != null) {
                    return bodyPrice;
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal extractByKnownKeys(String payload) {
        Pattern pattern = Pattern.compile("\\\"(?:price|regularMarketPrice|close|c|lastPrice)\\\"\\s*:\\s*\\\"?(-?\\d+(?:\\.\\d+)?)\\\"?");
        Matcher matcher = pattern.matcher(payload);
        while (matcher.find()) {
            String raw = matcher.group(1);
            try {
                BigDecimal value = new BigDecimal(raw);
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Continue trying other matches.
            }
        }
        return null;
    }

    private String extractJsonStringField(String payload, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"(.*?)\\\"");
        Matcher matcher = pattern.matcher(payload);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
