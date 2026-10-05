package com.splitwise.service;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.SplitType;
import com.splitwise.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExpenseManagerTest {

    private ExpenseManager expenseManager;

    @BeforeEach
    void setUp() {
        expenseManager = new ExpenseManager();
        expenseManager.registerUser(new User("U1", "Alice", "a@a.com", "1"));
        expenseManager.registerUser(new User("U2", "Bob", "b@b.com", "2"));
        expenseManager.registerUser(new User("U3", "Charlie", "c@c.com", "3"));
    }

    @Test
    void testAddEqualExpenseUpdatesBalanceSheet() throws InvalidSplitException {
        List<String> participants = Arrays.asList("U1", "U2", "U3");
        expenseManager.addExpense("Lunch", 300.0, "U1", participants, SplitType.EQUAL, new ArrayList<>());

        Map<String, Map<String, Double>> balances = expenseManager.getBalanceSheet();
        
        // U2 owes U1 100.0
        assertEquals(100.0, balances.get("U2").get("U1"));
        assertEquals(-100.0, balances.get("U1").get("U2"));
        
        // U3 owes U1 100.0
        assertEquals(100.0, balances.get("U3").get("U1"));
        assertEquals(-100.0, balances.get("U1").get("U3"));
    }

    @Test
    void testAddExactExpenseUpdatesBalanceSheet() throws InvalidSplitException {
        List<String> participants = Arrays.asList("U2", "U3");
        List<Double> exactValues = Arrays.asList(200.0, 300.0);
        expenseManager.addExpense("Groceries", 500.0, "U1", participants, SplitType.EXACT, exactValues);

        Map<String, Map<String, Double>> balances = expenseManager.getBalanceSheet();
        
        // U2 owes U1 200.0
        assertEquals(200.0, balances.get("U2").get("U1"));
        
        // U3 owes U1 300.0
        assertEquals(300.0, balances.get("U3").get("U1"));
    }

    @Test
    void testAddPercentExpenseUpdatesBalanceSheet() throws InvalidSplitException {
        List<String> participants = Arrays.asList("U1", "U2", "U3");
        List<Double> percentages = Arrays.asList(20.0, 30.0, 50.0);
        // U2 pays for everyone
        expenseManager.addExpense("Trip", 1000.0, "U2", participants, SplitType.PERCENT, percentages);

        Map<String, Map<String, Double>> balances = expenseManager.getBalanceSheet();
        
        // U1 owes U2 20% of 1000 = 200
        assertEquals(200.0, balances.get("U1").get("U2"));
        
        // U3 owes U2 50% of 1000 = 500
        assertEquals(500.0, balances.get("U3").get("U2"));
    }

    @Test
    void testAddExpenseWithUnregisteredUserThrowsException() {
        List<String> participants = Arrays.asList("U1", "U99"); // U99 doesn't exist
        
        assertThrows(IllegalArgumentException.class, () -> 
            expenseManager.addExpense("Invalid", 100.0, "U1", participants, SplitType.EQUAL, new ArrayList<>())
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            expenseManager.addExpense("Invalid", 100.0, "U99", Arrays.asList("U1", "U2"), SplitType.EQUAL, new ArrayList<>())
        );
    }
}
