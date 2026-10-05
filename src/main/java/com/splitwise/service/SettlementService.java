package com.splitwise.service;

import com.splitwise.model.Transaction;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class SettlementService {

    static class BalanceEntry {
        String userId;
        double amount;

        BalanceEntry(String userId, double amount) {
            this.userId = userId;
            this.amount = amount;
        }
    }

    public List<Transaction> simplifyDebts(Map<String, Map<String, Double>> balanceSheet, Map<String, User> userMap) {
        // Step A: Compute the NET balance for every user across the system
        Map<String, Double> netBalances = new HashMap<>();
        
        if (userMap != null) {
            for (String userId : userMap.keySet()) {
                netBalances.put(userId, 0.0);
            }
        }

        if (balanceSheet != null) {
            for (Map.Entry<String, Map<String, Double>> outer : balanceSheet.entrySet()) {
                String userA = outer.getKey();
                netBalances.putIfAbsent(userA, 0.0);

                for (Map.Entry<String, Double> inner : outer.getValue().entrySet()) {
                    String userB = inner.getKey();
                    netBalances.putIfAbsent(userB, 0.0);

                    double amount = inner.getValue(); // userA owes userB this amount
                    
                    // To prevent double counting symmetrical matrix entries, evaluate only positive debts
                    if (amount > 0) {
                        netBalances.put(userA, netBalances.get(userA) - amount);
                        netBalances.put(userB, netBalances.get(userB) + amount);
                    }
                }
            }
        }

        // Step B: Partition users into two max-heaps
        PriorityQueue<BalanceEntry> debtorsQueue = new PriorityQueue<>(
                (a, b) -> Double.compare(b.amount, a.amount)
        ); // Stores debtors (negative net balances) as positive magnitudes

        PriorityQueue<BalanceEntry> creditorsQueue = new PriorityQueue<>(
                (a, b) -> Double.compare(b.amount, a.amount)
        ); // Stores creditors (positive net balances)

        for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
            String userId = entry.getKey();
            double net = entry.getValue();

            if (net > 0.01) {
                creditorsQueue.offer(new BalanceEntry(userId, net));
            } else if (net < -0.01) {
                debtorsQueue.offer(new BalanceEntry(userId, Math.abs(net)));
            }
        }

        // Step C: Greedy Matching Loop
        List<Transaction> transactions = new ArrayList<>();

        while (!debtorsQueue.isEmpty() && !creditorsQueue.isEmpty()) {
            BalanceEntry debtor = debtorsQueue.poll();
            BalanceEntry creditor = creditorsQueue.poll();

            double minSettlement = Math.min(debtor.amount, creditor.amount);
            minSettlement = Math.round(minSettlement * 100.0) / 100.0;

            if (minSettlement > 0.01) {
                transactions.add(new Transaction(
                        userMap.get(debtor.userId),
                        userMap.get(creditor.userId),
                        minSettlement
                ));
            }

            double remainingDebtor = Math.round((debtor.amount - minSettlement) * 100.0) / 100.0;
            double remainingCreditor = Math.round((creditor.amount - minSettlement) * 100.0) / 100.0;

            if (remainingDebtor > 0.01) {
                debtorsQueue.offer(new BalanceEntry(debtor.userId, remainingDebtor));
            }
            if (remainingCreditor > 0.01) {
                creditorsQueue.offer(new BalanceEntry(creditor.userId, remainingCreditor));
            }
        }

        // Step D: Return the simplified transactions
        return transactions;
    }

    public void printSettlementPlan(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("All debts are already fully settled!");
            return;
        }

        for (Transaction transaction : transactions) {
            System.out.println(transaction);
        }
    }
}
