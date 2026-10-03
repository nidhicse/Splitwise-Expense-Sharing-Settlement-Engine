package com.splitwise.model;

import java.time.LocalDateTime;
import java.util.List;

public class Expense {
    private final String id;
    private final String description;
    private final double totalAmount;
    private final User paidBy;
    private final List<Split> splits;
    private final SplitType splitType;
    private final LocalDateTime createdAt;

    public Expense(String id, String description, double totalAmount, User paidBy, List<Split> splits, SplitType splitType, LocalDateTime createdAt) {
        this.id = id;
        this.description = description;
        this.totalAmount = totalAmount;
        this.paidBy = paidBy;
        this.splits = splits;
        this.splitType = splitType;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean validate() {
        if (splits == null || splits.isEmpty()) {
            return false;
        }

        double sum = 0;
        for (Split split : splits) {
            sum += split.getAmount();
        }

        // Use a 0.01 tolerance to account for floating-point inaccuracies
        return Math.abs(sum - totalAmount) <= 0.01;
    }
}
