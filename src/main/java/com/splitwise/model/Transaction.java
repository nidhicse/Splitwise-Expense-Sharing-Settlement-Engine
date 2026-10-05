package com.splitwise.model;

public record Transaction(User fromUser, User toUser, double amount) {
    @Override
    public String toString() {
        return String.format("%s pays %s: ₹%.2f", fromUser.getName(), toUser.getName(), amount);
    }
}
