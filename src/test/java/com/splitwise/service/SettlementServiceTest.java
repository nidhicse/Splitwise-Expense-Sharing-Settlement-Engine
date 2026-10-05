package com.splitwise.service;

import com.splitwise.model.Transaction;
import com.splitwise.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettlementServiceTest {

    private SettlementService settlementService;
    private Map<String, User> userMap;
    private Map<String, Map<String, Double>> balanceSheet;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService();
        userMap = new HashMap<>();
        balanceSheet = new HashMap<>();

        userMap.put("A", new User("A", "User A", "a@a.com", "1"));
        userMap.put("B", new User("B", "User B", "b@b.com", "2"));
        userMap.put("C", new User("C", "User C", "c@c.com", "3"));
        userMap.put("D", new User("D", "User D", "d@d.com", "4"));
    }

    private void addDebt(String debtorId, String creditorId, double amount) {
        balanceSheet.putIfAbsent(debtorId, new HashMap<>());
        balanceSheet.putIfAbsent(creditorId, new HashMap<>());

        double currentDebt = balanceSheet.get(debtorId).getOrDefault(creditorId, 0.0);
        balanceSheet.get(debtorId).put(creditorId, currentDebt + amount);

        double currentCredit = balanceSheet.get(creditorId).getOrDefault(debtorId, 0.0);
        balanceSheet.get(creditorId).put(debtorId, currentCredit - amount);
    }

    @Test
    void testCircularDebtSimplification() {
        // A owes B 500, B owes C 500
        addDebt("A", "B", 500.0);
        addDebt("B", "C", 500.0);

        List<Transaction> transactions = settlementService.simplifyDebts(balanceSheet, userMap);

        // Should simplify directly to A pays C 500
        assertEquals(1, transactions.size());
        Transaction t = transactions.get(0);
        assertEquals("A", t.fromUser().getId());
        assertEquals("C", t.toUser().getId());
        assertEquals(500.0, t.amount());
    }

    @Test
    void testMultiPartyDebtSimplification() {
        // A owes B 200, B owes C 300, C owes D 100, D owes A 50
        addDebt("A", "B", 200.0);
        addDebt("B", "C", 300.0);
        addDebt("C", "D", 100.0);
        addDebt("D", "A", 50.0);

        List<Transaction> transactions = settlementService.simplifyDebts(balanceSheet, userMap);

        // For N=4, max transactions optimally should be <= 3 (N-1)
        assertTrue(transactions.size() <= 3);

        // Verify total cash flow satisfies all original net positions
        Map<String, Double> postNet = new HashMap<>();
        for (String u : userMap.keySet()) {
            postNet.put(u, 0.0);
        }
        
        for (Transaction t : transactions) {
            postNet.put(t.fromUser().getId(), postNet.get(t.fromUser().getId()) + t.amount());
            postNet.put(t.toUser().getId(), postNet.get(t.toUser().getId()) - t.amount());
        }

        // Original Nets:
        // A: owes 200, owed 50 -> net -150
        // B: owes 300, owed 200 -> net -100
        // C: owes 100, owed 300 -> net +200
        // D: owes 50, owed 100 -> net +50
        // After transactions, they should have paid off / received exactly those amounts
        assertEquals(150.0, postNet.get("A"), 0.01);
        assertEquals(100.0, postNet.get("B"), 0.01);
        assertEquals(-200.0, postNet.get("C"), 0.01);
        assertEquals(-50.0, postNet.get("D"), 0.01);
    }

    @Test
    void testFullySettledBalanceSheetReturnsEmptyList() {
        addDebt("A", "B", 100.0);
        addDebt("B", "A", 100.0); // symmetrically offsets
        
        List<Transaction> transactions = settlementService.simplifyDebts(balanceSheet, userMap);
        assertTrue(transactions.isEmpty());
    }
}
