package org.example.xianthebigfourtportfoliomanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.xianthebigfourtportfoliomanager.entity.priceHistory;
import org.example.xianthebigfourtportfoliomanager.repository.PriceHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class YahooFinanceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final PriceHistoryRepository priceHistoryRepository;
    private final Map<String, BigDecimal> priceCache = new ConcurrentHashMap<>();

    public YahooFinanceService(
            @Value("${yahoo.finance.base-url}") String baseUrl,
            PriceHistoryRepository priceHistoryRepository
    ) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public BigDecimal getCurrentPrice(String ticker) {
        if (ticker == null || ticker.isBlank()) return null;
        String normalized = ticker.toUpperCase();

        BigDecimal cached = priceCache.get(normalized);
        if (cached != null) return cached;

        try {
            String url = baseUrl + "?ticker=" + normalized;
            String response = restTemplate.getForObject(url, String.class);
            BigDecimal price = parsePrice(response, normalized);
            if (price != null) {
                priceCache.put(normalized, price);
            }
            return price;
        } catch (Exception e) {
            return null;
        }
    }

    public priceHistory fetchAndStoreCurrentPrice(String ticker) {
        if (ticker == null || ticker.isBlank()) return null;
        String normalized = ticker.toUpperCase();

        try {
            String url = baseUrl + "?ticker=" + normalized;
            String response = restTemplate.getForObject(url, String.class);
            List<priceHistory> savedRows = parseAndPersistSeries(response, normalized);
            if (savedRows.isEmpty()) {
                return null;
            }
            priceHistory latest = savedRows.get(savedRows.size() - 1);
            priceCache.put(normalized, latest.getCloseprice());
            return latest;
        } catch (Exception e) {
            return null;
        }
    }

    public void clearCache() {
        priceCache.clear();
    }

    private BigDecimal parsePrice(String response, String ticker) {
        if (response == null || response.isBlank()) return null;
        String trimmed = response.trim();

        // Plain numeric payload from lightweight endpoints
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException ignored) {
        }

        // JSON map payload e.g. {"AAPL": 195.12} or {"price": 195.12}
        BigDecimal mapPrice = extractNumericFromJsonMap(trimmed, ticker);
        if (mapPrice != null) {
            return mapPrice;
        }

        // Lambda proxy payload e.g. {"statusCode":200,"body":"{\"AAPL\":195.12}"}
        String nestedBody = extractStringValue(trimmed, "body");
        if (nestedBody != null) {
            BigDecimal nestedPrice = extractNumericFromJsonMap(nestedBody, ticker);
            if (nestedPrice != null) {
                return nestedPrice;
            }
        }

        return null;
    }

    private YahooSnapshot parseSnapshot(String response, String ticker) {
        YahooSnapshot snapshot = new YahooSnapshot();
        snapshot.priceDate = null;
        snapshot.closePrice = parsePrice(response, ticker);
        snapshot.openPrice = extractNumericFromJsonMapByKeys(response, new String[]{"open", "regularMarketOpen"});
        snapshot.highPrice = extractNumericFromJsonMapByKeys(response, new String[]{"high", "regularMarketDayHigh"});
        snapshot.lowPrice = extractNumericFromJsonMapByKeys(response, new String[]{"low", "regularMarketDayLow"});
        snapshot.adjustedClose = extractNumericFromJsonMapByKeys(response, new String[]{"adjClose", "adjustedClose"});
        snapshot.volume = extractLongByKeys(response, new String[]{"volume", "regularMarketVolume"});
        snapshot.currency = extractStringByKeys(response, new String[]{"currency"});

        String dateText = extractStringByKeys(response, new String[]{"date", "tradeDate", "regularMarketDate"});
        if (dateText != null) {
            try {
                snapshot.priceDate = LocalDate.parse(dateText);
            } catch (Exception ignored) {
            }
        }

        if (snapshot.priceDate == null) {
            Long epoch = extractLongByKeys(response, new String[]{"regularMarketTime", "timestamp"});
            if (epoch != null) {
                snapshot.priceDate = Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.UTC).toLocalDate();
            }
        }

        if (snapshot.priceDate == null) {
            snapshot.priceDate = LocalDate.now();
        }

        if (snapshot.adjustedClose == null) {
            snapshot.adjustedClose = snapshot.closePrice;
        }

        return snapshot;
    }

    private List<priceHistory> parseAndPersistSeries(String response, String ticker) {
        List<priceHistory> savedRows = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return savedRows;
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.has("body") && root.get("body").isTextual()) {
                root = objectMapper.readTree(root.get("body").asText());
            }

            JsonNode priceData = root.path("price_data");
            JsonNode closeArr = priceData.path("close");
            JsonNode timeArr = priceData.path("timestamp");

            if (closeArr.isArray() && timeArr.isArray() && closeArr.size() > 0 && timeArr.size() > 0) {
                int len = Math.min(closeArr.size(), timeArr.size());
                for (int i = 0; i < len; i++) {
                    LocalDateTime pointTime = parseTimestamp(timeArr.get(i).asText(null));
                    if (pointTime == null) {
                        continue;
                    }

                    BigDecimal close = asDecimal(closeArr.get(i));
                    if (close == null) {
                        continue;
                    }

                    priceHistory row = new priceHistory();
                    row.setTicker(ticker);
                    row.setPriceDate(pointTime.toLocalDate());
                    row.setPricetime(pointTime);
                    row.setOpenprice(readArrayDecimal(priceData.path("open"), i));
                    row.setHighprice(readArrayDecimal(priceData.path("high"), i));
                    row.setLowprice(readArrayDecimal(priceData.path("low"), i));
                    row.setCloseprice(close);
                    row.setAdjustedclose(close);
                    row.setVolume(readArrayLong(priceData.path("volume"), i));
                    row.setCurrency(root.path("currency").isTextual() ? root.path("currency").asText() : null);
                    row.setRawpayload(i == len - 1 ? response : null);
                    row.setFetchedat(LocalDateTime.now());

                    priceHistory saved = priceHistoryRepository.saveOrUpdate(row);
                    if (saved != null) {
                        savedRows.add(saved);
                    }
                }

                if (!savedRows.isEmpty()) {
                    return savedRows;
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback: save single current snapshot for payloads without array data.
        YahooSnapshot snapshot = parseSnapshot(response, ticker);
        if (snapshot.closePrice == null) {
            return savedRows;
        }
        if (snapshot.priceDate == null) {
            snapshot.priceDate = LocalDate.now();
        }

        priceHistory row = new priceHistory();
        row.setTicker(ticker);
        row.setPriceDate(snapshot.priceDate);
        row.setPricetime(snapshot.priceDate.atStartOfDay());
        row.setOpenprice(snapshot.openPrice);
        row.setHighprice(snapshot.highPrice);
        row.setLowprice(snapshot.lowPrice);
        row.setCloseprice(snapshot.closePrice);
        row.setAdjustedclose(snapshot.adjustedClose);
        row.setVolume(snapshot.volume);
        row.setCurrency(snapshot.currency);
        row.setRawpayload(response);
        row.setFetchedat(LocalDateTime.now());

        priceHistory saved = priceHistoryRepository.saveOrUpdate(row);
        if (saved != null) {
            savedRows.add(saved);
        }
        return savedRows;
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        try {
            long epoch = Long.parseLong(value);
            return Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return null;
    }

    private BigDecimal readArrayDecimal(JsonNode array, int index) {
        if (!array.isArray() || index >= array.size()) return null;
        return asDecimal(array.get(index));
    }

    private Long readArrayLong(JsonNode array, int index) {
        if (!array.isArray() || index >= array.size()) return null;
        JsonNode node = array.get(index);
        if (node == null || node.isNull()) return null;
        if (node.isIntegralNumber()) return node.asLong();
        if (node.isFloatingPointNumber()) return (long) node.asDouble();
        try {
            return Long.parseLong(node.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal asDecimal(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isNumber()) return node.decimalValue();
        try {
            return new BigDecimal(node.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal extractNumericFromJsonMap(String json, String ticker) {
        String[] candidateKeys = new String[]{ticker, ticker.toLowerCase(), "price", "regularMarketPrice", "close"};
        for (String key : candidateKeys) {
            BigDecimal value = extractDecimalByKey(json, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal extractNumericFromJsonMapByKeys(String json, String[] keys) {
        if (json == null || json.isBlank()) {
            return null;
        }
        for (String key : keys) {
            BigDecimal value = extractDecimalByKey(json, key);
            if (value != null) {
                return value;
            }
        }
        String nestedBody = extractStringValue(json, "body");
        if (nestedBody != null) {
            for (String key : keys) {
                BigDecimal value = extractDecimalByKey(nestedBody, key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private BigDecimal extractDecimalByKey(String json, String key) {
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex < 0) return null;

        int colon = json.indexOf(':', keyIndex + token.length());
        if (colon < 0) return null;

        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                end++;
                continue;
            }
            break;
        }

        if (end <= start) return null;

        try {
            return new BigDecimal(json.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractStringValue(String json, String key) {
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex < 0) return null;

        int colon = json.indexOf(':', keyIndex + token.length());
        if (colon < 0) return null;

        int quoteStart = json.indexOf('"', colon + 1);
        if (quoteStart < 0) return null;

        int quoteEnd = quoteStart + 1;
        StringBuilder builder = new StringBuilder();
        while (quoteEnd < json.length()) {
            char c = json.charAt(quoteEnd);
            if (c == '\\' && quoteEnd + 1 < json.length()) {
                char next = json.charAt(quoteEnd + 1);
                // Keep unescaped nested JSON text usable for key lookup.
                builder.append(next);
                quoteEnd += 2;
                continue;
            }
            if (c == '"') {
                return builder.toString();
            }
            builder.append(c);
            quoteEnd++;
        }

        return null;
    }

    private Long extractLongByKeys(String json, String[] keys) {
        if (json == null || json.isBlank()) {
            return null;
        }
        for (String key : keys) {
            Long value = extractLongByKey(json, key);
            if (value != null) {
                return value;
            }
        }
        String nestedBody = extractStringValue(json, "body");
        if (nestedBody != null) {
            for (String key : keys) {
                Long value = extractLongByKey(nestedBody, key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private Long extractLongByKey(String json, String key) {
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex < 0) return null;

        int colon = json.indexOf(':', keyIndex + token.length());
        if (colon < 0) return null;

        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if ((c >= '0' && c <= '9') || c == '-') {
                end++;
                continue;
            }
            break;
        }

        if (end <= start) return null;

        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractStringByKeys(String json, String[] keys) {
        if (json == null || json.isBlank()) {
            return null;
        }
        for (String key : keys) {
            String value = extractStringValue(json, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        String nestedBody = extractStringValue(json, "body");
        if (nestedBody != null) {
            for (String key : keys) {
                String value = extractStringValue(nestedBody, key);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static class YahooSnapshot {
        private LocalDate priceDate;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal closePrice;
        private BigDecimal adjustedClose;
        private Long volume;
        private String currency;
    }
}
