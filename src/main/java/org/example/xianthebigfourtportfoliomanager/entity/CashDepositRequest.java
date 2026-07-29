package org.example.xianthebigfourtportfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CashDepositRequest {
    private BigDecimal amount;
    private LocalDateTime tradeDate;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDateTime tradeDate) {
        this.tradeDate = tradeDate;
    }
}