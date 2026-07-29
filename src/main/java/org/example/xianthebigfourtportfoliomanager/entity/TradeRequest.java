package org.example.xianthebigfourtportfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeRequest {
    private Integer holdingId;
    private String ticker;
    private String assetType;
    private String currency;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDateTime tradeDate;

    public Integer getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Integer holdingId) {
        this.holdingId = holdingId;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDateTime tradeDate) {
        this.tradeDate = tradeDate;
    }
}