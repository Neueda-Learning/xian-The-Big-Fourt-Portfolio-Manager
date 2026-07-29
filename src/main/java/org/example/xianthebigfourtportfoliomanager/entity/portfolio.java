package org.example.xianthebigfourtportfoliomanager.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class portfolio {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal initialCash;
    private BigDecimal cashBalance;
    private LocalDateTime createAt;
    private LocalDateTime uodataAt;

    public portfolio() {
    }

    public portfolio(Integer id, String name, String description, BigDecimal initialCash, BigDecimal cashBalance, LocalDateTime createAt, LocalDateTime uodataAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.initialCash = initialCash;
        this.cashBalance = cashBalance;
        this.createAt = createAt;
        this.uodataAt = uodataAt;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getInitialCash() {
        return initialCash;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public LocalDateTime getUodataAt() {
        return uodataAt;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setInitialCash(BigDecimal initialCash) {
        this.initialCash = initialCash;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public void setUodataAt(LocalDateTime uodataAt) {
        this.uodataAt = uodataAt;
    }
}
