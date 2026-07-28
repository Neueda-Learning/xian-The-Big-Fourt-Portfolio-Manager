package org.example.xianthebigfourtportfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class priceHistory {
    private Integer id;
    private String ticker;
    private LocalDate priceDate;
    private BigDecimal closeprice;
    private LocalDateTime createat;

    public priceHistory() {
    }

    public priceHistory(Integer id, String ticker, LocalDate priceDate, BigDecimal closeprice, LocalDateTime createat) {
        this.id = id;
        this.ticker = ticker;
        this.priceDate = priceDate;
        this.closeprice = closeprice;
        this.createat = createat;
    }

    public Integer getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public BigDecimal getCloseprice() {
        return closeprice;
    }

    public LocalDateTime getCreateat() {
        return createat;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public void setCloseprice(BigDecimal closeprice) {
        this.closeprice = closeprice;
    }

    public void setCreateat(LocalDateTime createat) {
        this.createat = createat;
    }
}
