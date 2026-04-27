package com.example.smartexpensetracker.model;

public class CategoryTotal {

    private final String category;
    private final double amount;

    public CategoryTotal(String category, double amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }
}
