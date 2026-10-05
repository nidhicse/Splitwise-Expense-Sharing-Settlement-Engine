package com.splitwise;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.Split;
import com.splitwise.model.SplitType;
import com.splitwise.model.Transaction;
import com.splitwise.model.User;
import com.splitwise.service.ExpenseManager;
import com.splitwise.service.SettlementService;
import com.splitwise.strategy.EqualSplitStrategy;
import com.splitwise.strategy.ExactSplitStrategy;
import com.splitwise.strategy.PercentSplitStrategy;
import com.splitwise.strategy.SplitStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestRunner {

    public static void main(String[] args) {
        System.out.println("Starting custom TestRunner...");
        System.out.println("IMPORTANT: Ensure you run this with the -ea flag (e.g., java -ea com.splitwise.TestRunner) to enable assertions.\n");
        
        try {
            // Split Strategy Tests
            testEqualSplitEvenlyDivisible();
            testEqualSplitFractionalPennyDistribution();
            testExactSplitValid();
            testExactSplitSumMismatchThrowsException();
            testPercentSplitValid();
            testPercentSplitInvalidSumThrowsException();
            
            // Settlement Service Tests
            testCircularDebtSimplification();
            testMultiPartyDebtSimplification();
            testFullySettledBalanceSheetReturnsEmptyList();
            
            // Expense Manager Tests
            testAddEqualExpenseUpdatesBalanceSheet();
            testAddExactExpenseUpdatesBalanceSheet();
            testAddPercentExpenseUpdatesBalanceSheet();
            testAddExpenseWithUnregisteredUserThrowsException();
            
            System.out.println("\n✅ ALL TESTS PASSED SUCCESSFULLY!");
        } catch (AssertionError e) {
            System.err.println("\n❌ ASSERTION FAILED: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n❌ UNEXPECTED EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static User u1 = new User("U1", "Alice", "a@test.com", "111");
    private static User u2 = new User("U2", "Bob", "b@test.com", "222");
    private static User u3 = new User("U3", "Charlie", "c@test.com", "333");

    // --- SplitStrategy Tests ---
    private static void testEqualSplitEvenlyDivisible() throws Exception {
        SplitStrategy strategy = new EqualSplitStrategy();
        List<Split> splits = strategy.calculateAndValidateSplits(300.0, Arrays.asList(u1, u2, u3), null);
        assert splits.size() == 3 : "Should have 3 splits";
        assert splits.get(0).getAmount() == 100.0;
        assert splits.get(1).getAmount() == 100.0;
        assert splits.get(2).getAmount() == 100.0;
        System.out.println("testEqualSplitEvenlyDivisible passed.");
    }

    private static void testEqualSplitFractionalPennyDistribution() throws Exception {
        SplitStrategy strategy = new EqualSplitStrategy();
        List<Split> splits = strategy.calculateAndValidateSplits(100.0, Arrays.asList(u1, u2, u3), null);
        assert splits.size() == 3;
        assert splits.get(0).getAmount() == 33.34 : "First participant should receive the extra fractional penny";
        assert splits.get(1).getAmount() == 33.33;
        assert splits.get(2).getAmount() == 33.33;
        
        double sum = splits.stream().mapToDouble(Split::getAmount).sum();
        assert Math.abs(sum - 100.0) < 0.01 : "Total should strictly equal 100.0";
        System.out.println("testEqualSplitFractionalPennyDistribution passed.");
    }

    private static void testExactSplitValid() throws Exception {
        SplitStrategy strategy = new ExactSplitStrategy();
        List<Split> splits = strategy.calculateAndValidateSplits(500.0, Arrays.asList(u1, u2), Arrays.asList(200.0, 300.0));
        assert splits.size() == 2;
        assert splits.get(0).getAmount() == 200.0;
        assert splits.get(1).getAmount() == 300.0;
        System.out.println("testExactSplitValid passed.");
    }

    private static void testExactSplitSumMismatchThrowsException() {
        SplitStrategy strategy = new ExactSplitStrategy();
        boolean thrown = false;
        try {
            strategy.calculateAndValidateSplits(600.0, Arrays.asList(u1, u2), Arrays.asList(200.0, 300.0));
        } catch (InvalidSplitException e) {
            thrown = true;
        }
        assert thrown : "Should throw InvalidSplitException on sum mismatch";
        System.out.println("testExactSplitSumMismatchThrowsException passed.");
    }

    private static void testPercentSplitValid() throws Exception {
        SplitStrategy strategy = new PercentSplitStrategy();
        List<Split> splits = strategy.calculateAndValidateSplits(1000.0, Arrays.asList(u1, u2), Arrays.asList(40.0, 60.0));
        assert splits.size() == 2;
        assert splits.get(0).getAmount() == 400.0;
        assert splits.get(1).getAmount() == 600.0;
        System.out.println("testPercentSplitValid passed.");
    }

    private static void testPercentSplitInvalidSumThrowsException() {
        SplitStrategy strategy = new PercentSplitStrategy();
        boolean thrown = false;
        try {
            strategy.calculateAndValidateSplits(1000.0, Arrays.asList(u1, u2), Arrays.asList(40.0, 70.0));
        } catch (InvalidSplitException e) {
            thrown = true;
        }
        assert thrown : "Should throw InvalidSplitException on percentage mismatch";
        System.out.println("testPercentSplitInvalidSumThrowsException passed.");
    }

    // --- SettlementService Tests ---
    private static void testCircularDebtSimplification() {
        SettlementService service = new SettlementService();
        Map<String, User> users = new HashMap<>();
        users.put("A", new User("A", "User A", "", ""));
        users.put("B", new User("B", "User B", "", ""));
        users.put("C", new User("C", "User C", "", ""));
        
        Map<String, Map<String, Double>> balances = new HashMap<>();
        addDebt(balances, "A", "B", 500.0);
        addDebt(balances, "B", "C", 500.0);

        List<Transaction> transactions = service.simplifyDebts(balances, users);
        assert transactions.size() == 1 : "Circular debt should simplify to 1 transaction";
        assert transactions.get(0).fromUser().getId().equals("A");
        assert transactions.get(0).toUser().getId().equals("C");
        assert transactions.get(0).amount() == 500.0;
        System.out.println("testCircularDebtSimplification passed.");
    }

    private static void testMultiPartyDebtSimplification() {
        SettlementService service = new SettlementService();
        Map<String, User> users = new HashMap<>();
        users.put("A", new User("A", "User A", "", ""));
        users.put("B", new User("B", "User B", "", ""));
        users.put("C", new User("C", "User C", "", ""));
        users.put("D", new User("D", "User D", "", ""));
        
        Map<String, Map<String, Double>> balances = new HashMap<>();
        addDebt(balances, "A", "B", 200.0);
        addDebt(balances, "B", "C", 300.0);
        addDebt(balances, "C", "D", 100.0);
        addDebt(balances, "D", "A", 50.0);

        List<Transaction> transactions = service.simplifyDebts(balances, users);
        assert transactions.size() <= 3 : "Max edges should be <= N-1";
        
        Map<String, Double> postNet = new HashMap<>();
        for (String u : users.keySet()) postNet.put(u, 0.0);
        
        for (Transaction t : transactions) {
            postNet.put(t.fromUser().getId(), postNet.get(t.fromUser().getId()) + t.amount());
            postNet.put(t.toUser().getId(), postNet.get(t.toUser().getId()) - t.amount());
        }
        
        assert Math.abs(postNet.get("A") - 150.0) < 0.01;
        assert Math.abs(postNet.get("B") - 100.0) < 0.01;
        assert Math.abs(postNet.get("C") - (-200.0)) < 0.01;
        assert Math.abs(postNet.get("D") - (-50.0)) < 0.01;
        System.out.println("testMultiPartyDebtSimplification passed.");
    }

    private static void testFullySettledBalanceSheetReturnsEmptyList() {
        SettlementService service = new SettlementService();
        Map<String, User> users = new HashMap<>();
        users.put("A", new User("A", "User A", "", ""));
        users.put("B", new User("B", "User B", "", ""));
        
        Map<String, Map<String, Double>> balances = new HashMap<>();
        addDebt(balances, "A", "B", 100.0);
        addDebt(balances, "B", "A", 100.0); 
        
        List<Transaction> transactions = service.simplifyDebts(balances, users);
        assert transactions.isEmpty() : "Should return empty list for settled debts";
        System.out.println("testFullySettledBalanceSheetReturnsEmptyList passed.");
    }

    private static void addDebt(Map<String, Map<String, Double>> balanceSheet, String debtorId, String creditorId, double amount) {
        balanceSheet.putIfAbsent(debtorId, new HashMap<>());
        balanceSheet.putIfAbsent(creditorId, new HashMap<>());

        double currentDebt = balanceSheet.get(debtorId).getOrDefault(creditorId, 0.0);
        balanceSheet.get(debtorId).put(creditorId, currentDebt + amount);

        double currentCredit = balanceSheet.get(creditorId).getOrDefault(debtorId, 0.0);
        balanceSheet.get(creditorId).put(debtorId, currentCredit - amount);
    }

    // --- ExpenseManager Tests ---
    private static void testAddEqualExpenseUpdatesBalanceSheet() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.registerUser(new User("U1", "Alice", "", ""));
        manager.registerUser(new User("U2", "Bob", "", ""));
        manager.registerUser(new User("U3", "Charlie", "", ""));
        
        manager.addExpense("Lunch", 300.0, "U1", Arrays.asList("U1", "U2", "U3"), SplitType.EQUAL, new ArrayList<>());
        
        Map<String, Map<String, Double>> balances = manager.getBalanceSheet();
        assert balances.get("U2").get("U1") == 100.0;
        assert balances.get("U1").get("U2") == -100.0;
        assert balances.get("U3").get("U1") == 100.0;
        assert balances.get("U1").get("U3") == -100.0;
        System.out.println("testAddEqualExpenseUpdatesBalanceSheet passed.");
    }

    private static void testAddExactExpenseUpdatesBalanceSheet() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.registerUser(new User("U1", "Alice", "", ""));
        manager.registerUser(new User("U2", "Bob", "", ""));
        manager.registerUser(new User("U3", "Charlie", "", ""));
        
        manager.addExpense("Groceries", 500.0, "U1", Arrays.asList("U2", "U3"), SplitType.EXACT, Arrays.asList(200.0, 300.0));
        
        Map<String, Map<String, Double>> balances = manager.getBalanceSheet();
        assert balances.get("U2").get("U1") == 200.0;
        assert balances.get("U3").get("U1") == 300.0;
        System.out.println("testAddExactExpenseUpdatesBalanceSheet passed.");
    }

    private static void testAddPercentExpenseUpdatesBalanceSheet() throws Exception {
        ExpenseManager manager = new ExpenseManager();
        manager.registerUser(new User("U1", "Alice", "", ""));
        manager.registerUser(new User("U2", "Bob", "", ""));
        manager.registerUser(new User("U3", "Charlie", "", ""));
        
        manager.addExpense("Trip", 1000.0, "U2", Arrays.asList("U1", "U2", "U3"), SplitType.PERCENT, Arrays.asList(20.0, 30.0, 50.0));
        
        Map<String, Map<String, Double>> balances = manager.getBalanceSheet();
        assert balances.get("U1").get("U2") == 200.0;
        assert balances.get("U3").get("U2") == 500.0;
        System.out.println("testAddPercentExpenseUpdatesBalanceSheet passed.");
    }

    private static void testAddExpenseWithUnregisteredUserThrowsException() {
        ExpenseManager manager = new ExpenseManager();
        manager.registerUser(new User("U1", "Alice", "", ""));
        
        boolean thrown1 = false;
        try {
            manager.addExpense("Invalid", 100.0, "U1", Arrays.asList("U1", "U99"), SplitType.EQUAL, new ArrayList<>());
        } catch (IllegalArgumentException | InvalidSplitException e) {
            thrown1 = true;
        }
        assert thrown1 : "Should throw exception for unknown participant";
        
        boolean thrown2 = false;
        try {
            manager.addExpense("Invalid", 100.0, "U99", Arrays.asList("U1"), SplitType.EQUAL, new ArrayList<>());
        } catch (IllegalArgumentException | InvalidSplitException e) {
            thrown2 = true;
        }
        assert thrown2 : "Should throw exception for unknown paidBy user";
        System.out.println("testAddExpenseWithUnregisteredUserThrowsException passed.");
    }
}
