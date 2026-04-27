package com.example.smartexpensetracker.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Locale;

@Entity(tableName = "expenses")
public class Expense {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public double amount;
    public String category;
    public String date;
    public String paymentMethod;
    public String reason;
    public int necessityLevel;
    public String mood;
    public long timestamp;
    public String source;

    public Expense(
            double amount,
            String category,
            String date,
            String paymentMethod,
            String reason,
            int necessityLevel,
            String mood,
            long timestamp,
            String source
    ) {
        this.amount = amount;
        this.category = sanitize(category);
        this.date = sanitize(date);
        this.paymentMethod = sanitize(paymentMethod);
        this.reason = sanitize(reason).toLowerCase(Locale.ROOT);
        this.necessityLevel = necessityLevel;
        this.mood = sanitize(mood).toLowerCase(Locale.ROOT);
        this.timestamp = timestamp;
        this.source = sanitize(source);
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getReason() {
        return reason;
    }

    public int getNecessityLevel() {
        return necessityLevel;
    }

    public String getMood() {
        return mood;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }
}
