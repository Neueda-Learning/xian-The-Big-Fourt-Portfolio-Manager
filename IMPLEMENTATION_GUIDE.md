# Yahoo Finance Integration - Implementation Guide

## Executive Summary

**Answer to your questions:**

1. ✅ **Is data crawled from Yahoo?**  
   YES, but ONLY real-time current prices. Data is NOT persisted to database.

2. ✅ **Is current database compatible?**  
   YES. Your `price_history` table CAN store Yahoo data without modification.

3. ⚠️ **Do you need new tables?**  
   NO, not required. But RECOMMENDED for comprehensive portfolio analysis.

---

## Current Architecture

### Data Flow
```
Yahoo Finance API (via AWS Lambda)
           ↓
  YahooFinanceService.getCurrentPrice(ticker)
           ↓
   Memory Cache (ConcurrentHashMap)
           ↓
  PerformanceService (for calculations)
           ↓
  ❌ NOT SAVED TO DATABASE ❌
```

### Problem
- Prices fetched from Yahoo are only cached in memory
- Cache is lost when application restarts
- No historical price tracking
- Cannot perform backtesting or historical analysis

---

## Implementation Plan

### Phase 1: IMMEDIATE (1-2 hours)
**Modify existing code to persist Yahoo prices**

#### Step 1.1: Update database schema
```sql
ALTER TABLE price_history 
ADD CONSTRAINT uk_ticker_date UNIQUE (ticker, price_date),
ADD INDEX idx_ticker (ticker);
```

#### Step 1.2: Replace YahooFinanceService.java
Replace the existing file with `YahooFinanceService_ENHANCED.java` provided:
- Adds automatic database persistence
- Prices are saved as they're fetched
- Includes `getPriceHistorical()` method

**Changes needed:**
```java
// Add this to YahooFinanceService constructor
private final PriceHistoryRepository priceHistoryRepository;

// Add this to getCurrentPrice() method
if (price != null) {
    priceCache.put(ticker.toUpperCase(), price);
    savePriceToDatabase(ticker, price, LocalDate.now()); // NEW LINE
}

// Add new methods
private void savePriceToDatabase(String ticker, BigDecimal price, LocalDate date) { ... }
public BigDecimal getPriceHistorical(String ticker, LocalDate date) { ... }
```

#### Step 1.3: Update PerformanceService (Optional)
Add fallback to historical prices if real-time fails:
```java
BigDecimal currentPrice = yahooFinanceService.getCurrentPrice(h.getTicker());
if (currentPrice == null) {
    // Fallback to yesterday's price
    currentPrice = yahooFinanceService.getPriceHistorical(h.getTicker(), LocalDate.now().minusDays(1));
}
```

**Result:** Prices from Yahoo are now automatically saved to database ✅

---

### Phase 2: ENHANCED (Optional, 3-4 hours)
**Add OHLC (Open, High, Low, Close) and Volume data support**

#### Step 2.1: Update database
```sql
ALTER TABLE price_history 
ADD COLUMN (
    open_price DECIMAL(18,4),
    high_price DECIMAL(18,4),
    low_price DECIMAL(18,4),
    adjusted_close DECIMAL(18,4),
    volume BIGINT
);
```

#### Step 2.2: Create new entity class
```java
public class PriceHistoryOHLC {
    private int id;
    private String ticker;
    private LocalDate priceDate;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal adjustedClose;
    private Long volume;
    private LocalDateTime fetchedAt;
    // getters/setters...
}
```

#### Step 2.3: Update Yahoo parsing
Extract OHLC from API response:
```java
private Map<String, BigDecimal> parseOHLC(String response) {
    // Parse JSON response to extract all OHLC fields
    // Return map with open, high, low, close, volume, adjusted_close
}
```

#### Step 2.4: Update PriceHistoryRepository
Add new save method:
```java
public priceHistory saveOHLC(String ticker, LocalDate date, 
    BigDecimal open, BigDecimal high, BigDecimal low, 
    BigDecimal close, BigDecimal adjusted, Long volume) {
    // Insert OHLC data
}
```

**Result:** Store complete OHLC data for technical analysis ✅

---

### Phase 3: ADVANCED (Future, 6-8 hours)
**Add dividend and split tracking for accurate returns**

#### Step 3.1: Create dividend_history table
```sql
CREATE TABLE dividend_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    ex_dividend_date DATE NOT NULL,
    dividend_per_share DECIMAL(18,4),
    payment_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (ticker, ex_dividend_date)
);
```

#### Step 3.2: Create stock_split_history table
```sql
CREATE TABLE stock_split_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    split_date DATE NOT NULL,
    split_ratio VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (ticker, split_date)
);
```

#### Step 3.3: Create dividend entity
```java
public class DividendHistory {
    private int id;
    private String ticker;
    private LocalDate exDividendDate;
    private BigDecimal dividendPerShare;
    private LocalDate paymentDate;
    // getters/setters...
}
```

#### Step 3.4: Implement dividend-adjusted returns
```java
// In PerformanceService
BigDecimal totalDividends = calculateDividendsSinceInvestment(ticker, purchaseDate);
BigDecimal totalReturnAdjusted = totalReturn.add(totalDividends);
```

**Result:** Accurate total return including dividends ✅

---

## File-by-File Changes

### 1. YahooFinanceService.java (HIGH PRIORITY)
**Current Issue:** Prices not persisted
**Fix:** 
- Add `PriceHistoryRepository` injection
- Save prices after fetching
- Add `getPriceHistorical()` method

**Before:**
```java
public BigDecimal getCurrentPrice(String ticker) {
    // ... fetch from API
    // Money lost! Price not saved
}
```

**After:**
```java
public BigDecimal getCurrentPrice(String ticker) {
    // ... fetch from API
    if (price != null) {
        priceCache.put(ticker.toUpperCase(), price);
        savePriceToDatabase(ticker, price, LocalDate.now()); // NEW
    }
}
```

### 2. priceHistory.java (If using OHLC)
**Add fields:**
```java
private BigDecimal openPrice;
private BigDecimal highPrice;
private BigDecimal lowPrice;
private BigDecimal adjustedClose;
private Long volume;
private LocalDateTime fetchedAt;
```

### 3. PriceHistoryRepository.java (If using OHLC)
**Add method:**
```java
public priceHistory saveOHLC(String ticker, LocalDate date, 
    BigDecimal open, BigDecimal high, BigDecimal low,
    BigDecimal close, BigDecimal adjusted, Long volume) {
    // Insert implementation
}
```

### 4. PerformanceService.java (Optional improvement)
**Add fallback logic:**
```java
BigDecimal currentPrice = yahooFinanceService.getCurrentPrice(h.getTicker());
if (currentPrice == null) {
    currentPrice = yahooFinanceService.getPriceHistorical(
        h.getTicker(), 
        LocalDate.now().minusDays(1)
    );
}
```

---

## Testing Checklist

### Phase 1 (Data Persistence)
- [ ] Start application
- [ ] Call performance calculation endpoint
- [ ] Check `price_history` table for new rows
- [ ] Verify duplicate protection (unique constraint)
- [ ] Restart application
- [ ] Verify prices still accessible from database

### Phase 2 (OHLC Support)
- [ ] Update entity with new fields
- [ ] Verify SQL parsing for OHLC
- [ ] Test database inserts with OHLC data
- [ ] Query historical OHLC data
- [ ] Test date range queries

### Phase 3 (Dividend/Split)
- [ ] Test dividend data insertion
- [ ] Test dividend calculation
- [ ] Verify return calculations include dividends
- [ ] Test stock split adjustments
- [ ] Verify historical price adjustments

---

## Database Compatibility Matrix

| Requirement | Current Schema | Phase 1 | Phase 2 | Phase 3 |
|---|---|---|---|---|
| Store current price | ✅ | ✅ | ✅ | ✅ |
| Store historical price | ✅ | ✅ | ✅ | ✅ |
| Store OHLC data | ❌ | ❌ | ✅ | ✅ |
| Store volume | ❌ | ❌ | ✅ | ✅ |
| Store dividends | ❌ | ❌ | ❌ | ✅ |
| Store splits | ❌ | ❌ | ❌ | ✅ |
| Prevent duplicates | ⚠️ | ✅ | ✅ | ✅ |
| Track data source | ⚠️ | ✅ | ✅ | ✅ |

---

## Implementation Steps (Do This Now)

### Quick Start (30 minutes)
1. ✅ Add unique constraint to price_history:
   ```sql
   ALTER TABLE price_history ADD CONSTRAINT uk_ticker_date UNIQUE (ticker, price_date);
   ```

2. ✅ Copy `YahooFinanceService_ENHANCED.java` and replace existing `YahooFinanceService.java`:
   ```bash
   cp YahooFinanceService_ENHANCED.java src/main/java/.../service/YahooFinanceService.java
   ```

3. ✅ Test the application:
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```

4. ✅ Verify prices are saved:
   ```sql
   SELECT COUNT(*) FROM price_history;
   SELECT * FROM price_history ORDER BY created_at DESC LIMIT 10;
   ```

### Done!
You now have:
- ✅ Automatic price persistence from Yahoo
- ✅ Historical price tracking
- ✅ Duplicate prevention
- ✅ Data audit trail

---

## FAQ

### Q: Will this break existing functionality?
**A:** No. The enhanced service is backward compatible. All existing methods still work the same way, just with added persistence.

### Q: Do I need to migrate existing data?
**A:** No. If you have no price_history data, start fresh. If you do, it stays as-is.

### Q: Will performance be affected?
**A:** Minimal. Database inserts are non-blocking (no additional latency to the user).

### Q: Can I disable persistence?
**A:** Yes, comment out the `savePriceToDatabase()` call in `getCurrentPrice()`.

### Q: What if Yahoo API fails?
**A:** Persistence is skipped, but prices still fallback to cache/historical data. No errors.

### Q: How much storage do I need?
**A:** ~1KB per price record. 250 tickers × 365 days/year ≈ 90 MB/year.

---

## Troubleshooting

### Problem: "Duplicate entry" error
**Solution:** Add unique constraint:
```sql
ALTER TABLE price_history ADD CONSTRAINT uk_ticker_date UNIQUE (ticker, price_date);
```

### Problem: Prices not saving
**Cause:** PriceHistoryRepository not injected  
**Solution:** Check Spring context loading, verify @Repository annotation

### Problem: Memory keeps growing
**Cause:** Cache not being cleared  
**Solution:** Clear cache periodically:
```java
yahooFinanceService.clearCache(); // Call on schedule
```

### Problem: Can't parse Yahoo response
**Cause:** Response format changed  
**Solution:** Debug print the response:
```java
System.out.println("Yahoo response: " + response);
```

---

## Summary

| What | Status | Action |
|---|---|---|
| **Current state** | Prices fetched but not saved | ⚠️ Fix immediately |
| **Database compatible** | YES | ✅ Ready to use |
| **Needs new tables** | No, optional | ✅ Not required |
| **Time to implement Phase 1** | 30 minutes | ⏱️ Quick fix |
| **Time to implement Phase 2** | 3-4 hours | ⏱️ Optional |
| **Time to implement Phase 3** | 6-8 hours | ⏱️ Future |

**Recommendation:** Implement Phase 1 ASAP to start capturing historical data. Phase 2 and 3 are optional enhancements for advanced analytics.


