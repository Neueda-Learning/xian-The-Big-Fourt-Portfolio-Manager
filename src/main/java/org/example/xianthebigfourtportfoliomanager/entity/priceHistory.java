package org.example.xianthebigfourtportfoliomanager.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class priceHistory {
    private int id;
    private String ticker;
    private LocalDate priceDate;
    private LocalDateTime pricetime;
    private BigDecimal openprice;
    private BigDecimal highprice;
    private BigDecimal lowprice;
    private BigDecimal closeprice;
    private BigDecimal adjustedclose;
    private Long volume;
    private String currency;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String rawpayload;
    private LocalDateTime fetchedat;
    private LocalDateTime createat;

    public priceHistory() {
    }

    public priceHistory(int id, String ticker, LocalDate priceDate, BigDecimal closeprice, LocalDateTime createat) {
        this.id = id;
        this.ticker = ticker;
        this.priceDate = priceDate;
        this.pricetime = createat;
        this.closeprice = closeprice;
        this.adjustedclose = closeprice;
        this.fetchedat = createat;
        this.createat = createat;
    }

    public priceHistory(int id, String ticker, LocalDate priceDate, LocalDateTime pricetime, BigDecimal openprice, BigDecimal highprice,
                        BigDecimal lowprice, BigDecimal closeprice, BigDecimal adjustedclose, Long volume,
                        String currency, String rawpayload, LocalDateTime fetchedat, LocalDateTime createat) {
        this.id = id;
        this.ticker = ticker;
        this.priceDate = priceDate;
        this.pricetime = pricetime;
        this.openprice = openprice;
        this.highprice = highprice;
        this.lowprice = lowprice;
        this.closeprice = closeprice;
        this.adjustedclose = adjustedclose;
        this.volume = volume;
        this.currency = currency;
        this.rawpayload = rawpayload;
        this.fetchedat = fetchedat;
        this.createat = createat;
    }

    public int getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public LocalDateTime getPricetime() {
        return pricetime;
    }

    public BigDecimal getCloseprice() {
        return closeprice;
    }

    public BigDecimal getOpenprice() {
        return openprice;
    }

    public BigDecimal getHighprice() {
        return highprice;
    }

    public BigDecimal getLowprice() {
        return lowprice;
    }

    public BigDecimal getAdjustedclose() {
        return adjustedclose;
    }

    public Long getVolume() {
        return volume;
    }

    public String getCurrency() {
        return currency;
    }

    public String getRawpayload() {
        return rawpayload;
    }

    public LocalDateTime getFetchedat() {
        return fetchedat;
    }

    public LocalDateTime getCreateat() {
        return createat;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public void setPricetime(LocalDateTime pricetime) {
        this.pricetime = pricetime;
    }

    public void setCloseprice(BigDecimal closeprice) {
        this.closeprice = closeprice;
    }

    public void setOpenprice(BigDecimal openprice) {
        this.openprice = openprice;
    }

    public void setHighprice(BigDecimal highprice) {
        this.highprice = highprice;
    }

    public void setLowprice(BigDecimal lowprice) {
        this.lowprice = lowprice;
    }

    public void setAdjustedclose(BigDecimal adjustedclose) {
        this.adjustedclose = adjustedclose;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setRawpayload(String rawpayload) {
        this.rawpayload = rawpayload;
    }

    public void setFetchedat(LocalDateTime fetchedat) {
        this.fetchedat = fetchedat;
    }

    public void setCreateat(LocalDateTime createat) {
        this.createat = createat;
    }
}
