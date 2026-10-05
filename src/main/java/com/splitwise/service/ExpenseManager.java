package com.splitwise.service;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.Expense;
import com.splitwise.model.Split;
import com.splitwise.model.SplitType;
import com.splitwise.model.User;
import com.splitwise.strategy.EqualSplitStrategy;
import com.splitwise.strategy.ExactSplitStrategy;
import com.splitwise.strategy.PercentSplitStrategy;
import com.splitwise.strategy.SplitStrategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExpenseManager {
    private final Map<String, User> userMap = new HashMap<>();
    private final List<Expense> expenses = new ArrayList<>();
    private final Map<String, Map<String, Double>> balanceSheet = new HashMap<>();
    private final Map<SplitType, SplitStrategy> strategyMap = new HashMap<>();

    public ExpenseManager() {
        strategyMap.put(SplitType.EQUAL, new EqualSplitStrategy());
        strategyMap.put(SplitType.EXACT, new ExactSplitStrategy());
        strategyMap.put(SplitType.PERCENT, new PercentSplitStrategy());
    }

    public void registerUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and User ID cannot be null");
        }
        userMap.put(user.getId(), user);
        balanceSheet.putIfAbsent(user.getId(), new HashMap<>());
    }

    public User getUser(String userId) {
        return userMap.get(userId);
    }

    public void addExpense(String description, double totalAmount, String paidByUserId, 
                           List<String> participantIds, SplitType splitType, List<Double> splitValues) throws InvalidSplitException {
        
        User paidBy = userMap.get(paidByUserId);
        if (paidBy == null) {
            throw new IllegalArgumentException("PaidBy user does not exist in the system.");
        }

        List<User> participants = new ArrayList<>();
        if (participantIds != null) {
            for (String id : participantIds) {
                User user = userMap.get(id);
                if (user == null) {
                    throw new IllegalArgumentException("Participant with ID " + id + " does not exist.");
                }
                participants.add(user);
            }
        }

        SplitStrategy strategy = strategyMap.get(splitType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown SplitType provided.");
        }

        List<Split> splits = strategy.calculateAndValidateSplits(totalAmount, participants, splitValues);

        Expense expense = new Expense(UUID.randomUUID().toString(), description, totalAmount, paidBy, splits, splitType, LocalDateTime.now());
        expenses.add(expense);

        updateBalanceSheet(paidByUserId, splits);
    }

    private void updateBalanceSheet(String paidByUserId, List<Split> splits) {
        for (Split split : splits) {
            String paidToId = split.getUser().getId();
            
            if (paidToId.equals(paidByUserId)) {
                continue; // User doesn't owe themselves
            }

            double owedAmount = split.getAmount();

            // Ensure nested maps are initialized if not present
            balanceSheet.putIfAbsent(paidToId, new HashMap<>());
            balanceSheet.putIfAbsent(paidByUserId, new HashMap<>());

            // Update paidTo's ledger: they owe paidBy
            double currentDebt = balanceSheet.get(paidToId).getOrDefault(paidByUserId, 0.0);
            balanceSheet.get(paidToId).put(paidByUserId, currentDebt + owedAmount);

            // Symmetrically update paidBy's ledger: decrease their debt towards paidTo
            double currentCredit = balanceSheet.get(paidByUserId).getOrDefault(paidToId, 0.0);
            balanceSheet.get(paidByUserId).put(paidToId, currentCredit - owedAmount);
        }
    }

    public void showBalances() {
        boolean isEmpty = true;
        for (Map.Entry<String, Map<String, Double>> allBalances : balanceSheet.entrySet()) {
            String user1Id = allBalances.getKey();
            for (Map.Entry<String, Double> userBalance : allBalances.getValue().entrySet()) {
                String user2Id = userBalance.getKey();
                double amount = userBalance.getValue();
                
                // Only print positive balances to avoid printing duplicates or epsilon differences
                if (amount > 0.01) {
                    isEmpty = false;
                    printBalance(user1Id, user2Id, amount);
                }
            }
        }
        
        if (isEmpty) {
            System.out.println("No balances.");
        }
    }

    public void showBalanceForUser(String userId) {
        if (!userMap.containsKey(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " does not exist.");
        }

        boolean isEmpty = true;
        Map<String, Double> userBalances = balanceSheet.get(userId);
        
        if (userBalances != null) {
            for (Map.Entry<String, Double> entry : userBalances.entrySet()) {
                String otherUserId = entry.getKey();
                double amount = entry.getValue();
                
                if (amount > 0.01) {
                    isEmpty = false;
                    printBalance(userId, otherUserId, amount);
                } else if (amount < -0.01) {
                    isEmpty = false;
                    printBalance(otherUserId, userId, Math.abs(amount));
                }
            }
        }

        if (isEmpty) {
            System.out.println("No balances.");
        }
    }
    
    private void printBalance(String user1Id, String user2Id, double amount) {
        User user1 = userMap.get(user1Id);
        User user2 = userMap.get(user2Id);
        System.out.printf("%s owes %s: ₹%.2f%n", user1.getName(), user2.getName(), amount);
    }

    public Map<String, Map<String, Double>> getBalanceSheet() {
        return balanceSheet;
    }

    public Map<String, User> getUserMap() {
        return userMap;
    }
}
