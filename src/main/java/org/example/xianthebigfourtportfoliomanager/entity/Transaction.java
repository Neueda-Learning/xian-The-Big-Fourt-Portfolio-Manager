package org.example.xianthebigfourtportfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private Integer id;
    private Integer holdingId;
    private String type;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDateTime tradeDate;

    public Transaction() {
    }

    public Transaction(Integer id, Integer holdingId, String type, BigDecimal quantity, BigDecimal price, LocalDateTime tradeDate) {
        this.id = id;
        this.holdingId = holdingId;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.tradeDate = tradeDate;
    }

    public Integer getId() {
        return id;
    }

    public Integer getHoldingId() {
        return holdingId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getTradeDate() {
        return tradeDate;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setHoldingId(Integer holdingId) {
        this.holdingId = holdingId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setTradeDate(LocalDateTime tradeDate) {
        this.tradeDate = tradeDate;
    }
}
