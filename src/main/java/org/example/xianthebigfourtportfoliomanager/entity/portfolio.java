package org.example.xianthebigfourtportfoliomanager.entity;

import java.time.LocalDateTime;

public class portfolio {
    private int id;
    private String name;
    private String description;
    private LocalDateTime createAt;
    private LocalDateTime uodataAt;

    public portfolio(int id, String name, String description, LocalDateTime createAt, LocalDateTime uodataAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createAt = createAt;
        this.uodataAt = uodataAt;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public LocalDateTime getUodataAt() {
        return uodataAt;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public void setUodataAt(LocalDateTime uodataAt) {
        this.uodataAt = uodataAt;
    }
}
