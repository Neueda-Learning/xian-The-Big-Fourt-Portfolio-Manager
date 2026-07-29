package org.example.xianthebigfourtportfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Holding {
    private Integer id;
    private Integer portfolioId;
    private AssetType assetType;
    private String ticker;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private LocalDate purchasedata;
    private String currency;
    private LocalDateTime createAt;
    private LocalDateTime updataAt;

    public Holding() {
    }

    public Holding(Integer id, Integer portfolioId, AssetType assetType, String ticker, BigDecimal quantity, BigDecimal purchasePrice, LocalDate purchasedata, String currency, LocalDateTime createAt, LocalDateTime updataAt) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.assetType = assetType;
        this.ticker = ticker;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchasedata = purchasedata;
        this.currency = currency;
        this.createAt = createAt;
        this.updataAt = updataAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getPortfolioId() {
        return portfolioId;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public LocalDate getPurchasedata() {
        return purchasedata;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public LocalDateTime getUpdataAt() {
        return updataAt;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public void setPurchasedata(LocalDate purchasedata) {
        this.purchasedata = purchasedata;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public void setUpdataAt(LocalDateTime updataAt) {
        this.updataAt = updataAt;
    }
}
