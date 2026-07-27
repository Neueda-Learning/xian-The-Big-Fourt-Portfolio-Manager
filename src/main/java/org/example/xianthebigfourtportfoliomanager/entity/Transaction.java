package org.example.xianthebigfourtportfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private int holdingId;
    private String type;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDateTime tradeDate;

    public Transaction(int id, int holdingId, String type, BigDecimal quantity, BigDecimal price, LocalDateTime tradeDate) {
        this.id = id;
        this.holdingId = holdingId;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.tradeDate = tradeDate;
    }

    public int getId() {
        return id;
    }

    public int getHoldingId() {
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

    public void setId(int id) {
        this.id = id;
    }

    public void setHoldingId(int holdingId) {
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
