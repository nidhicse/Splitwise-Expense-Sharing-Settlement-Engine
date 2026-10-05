package com.splitwise;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.SplitType;
import com.splitwise.model.Transaction;
import com.splitwise.model.User;
import com.splitwise.service.ExpenseManager;
import com.splitwise.service.SettlementService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final ExpenseManager expenseManager = new ExpenseManager();
    private static final SettlementService settlementService = new SettlementService();

    public static void main(String[] args) {
        seedData();
        printBanner();
        startREPL();
    }

    private static void seedData() {
        try {
            // Register 4 Users
            User u1 = new User("U1", "Amit", "amit@example.com", "9876543210");
            User u2 = new User("U2", "Priya", "priya@example.com", "9876543211");
            User u3 = new User("U3", "Rohan", "rohan@example.com", "9876543212");
            User u4 = new User("U4", "Sneha", "sneha@example.com", "9876543213");

            expenseManager.registerUser(u1);
            expenseManager.registerUser(u2);
            expenseManager.registerUser(u3);
            expenseManager.registerUser(u4);

            // Add 3 pre-seeded expenses
            expenseManager.addExpense("Dinner", 1000, "U1", 
                Arrays.asList("U1", "U2", "U3", "U4"), SplitType.EQUAL, new ArrayList<>());
            
            expenseManager.addExpense("Cab Fare", 500, "U2", 
                Arrays.asList("U2", "U3"), SplitType.EXACT, Arrays.asList(200.0, 300.0));
            
            expenseManager.addExpense("Groceries", 1200, "U3", 
                Arrays.asList("U1", "U2", "U3"), SplitType.PERCENT, Arrays.asList(40.0, 20.0, 40.0));
                
            System.out.println("[System] Demo data seeded successfully.");
        } catch (Exception e) {
            System.out.println("[System] Error seeding demo data: " + e.getMessage());
        }
    }

    private static void printBanner() {
        System.out.println("=========================================");
        System.out.println("  SPLITWISE EXPENSE & SETTLEMENT ENGINE  ");
        System.out.println("=========================================");
        System.out.println("Type 'HELP' for available commands.");
        System.out.println("-----------------------------------------");
    }

    private static void printHelp() {
        System.out.println("\n--- Available Commands ---");
        System.out.println("1. REGISTER <id> <name> <email> <phone>");
        System.out.println("   Example: REGISTER U5 John john@example.com 1234567890");
        System.out.println("2. EXPENSE <paidByUserId> <amount> <numParticipants> <id1> <id2>... <EQUAL|EXACT|PERCENT> [values...] <description>");
        System.out.println("   Example Equal:   EXPENSE U1 1200 3 U1 U2 U3 EQUAL Lunch");
        System.out.println("   Example Exact:   EXPENSE U2 500 2 U2 U3 EXACT 200 300 Snacks");
        System.out.println("   Example Percent: EXPENSE U3 1000 2 U1 U2 PERCENT 60 40 Movie");
        System.out.println("3. SHOW");
        System.out.println("   Shows all active balances across the system.");
        System.out.println("4. SHOW <userId>");
        System.out.println("   Shows active balances for a specific user.");
        System.out.println("5. SIMPLIFY");
        System.out.println("   Computes the optimal greedy settlement plan using priority queues.");
        System.out.println("6. EXIT");
        System.out.println("   Terminates the application.\n");
    }

    private static void startREPL() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;
            
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            String command = tokens[0].toUpperCase();

            try {
                switch (command) {
                    case "REGISTER":
                        handleRegister(tokens);
                        break;
                    case "EXPENSE":
                        handleExpense(tokens);
                        break;
                    case "SHOW":
                        if (tokens.length == 1) {
                            expenseManager.showBalances();
                        } else {
                            expenseManager.showBalanceForUser(tokens[1]);
                        }
                        break;
                    case "SIMPLIFY":
                        List<Transaction> plan = settlementService.simplifyDebts(
                                expenseManager.getBalanceSheet(), expenseManager.getUserMap());
                        System.out.println("\n--- Optimized Settlement Plan ---");
                        settlementService.printSettlementPlan(plan);
                        System.out.println("---------------------------------\n");
                        break;
                    case "HELP":
                        printHelp();
                        break;
                    case "EXIT":
                        System.out.println("Thank you for using Splitwise Engine. Goodbye!");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Unknown command: " + command + ". Type HELP for syntax.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Number parsing error. Ensure amounts and percentages are numeric.");
            } catch (InvalidSplitException | IllegalArgumentException e) {
                System.out.println("Validation Error: " + e.getMessage());
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Error parsing command. Please check syntax using HELP command.");
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }
    }

    private static void handleRegister(String[] tokens) {
        if (tokens.length < 5) {
            throw new IllegalArgumentException("Invalid REGISTER syntax.");
        }
        String id = tokens[1];
        String name = tokens[2];
        String email = tokens[3];
        String phone = tokens[4];
        
        expenseManager.registerUser(new User(id, name, email, phone));
        System.out.println("User '" + name + "' registered successfully with ID: " + id);
    }

    private static void handleExpense(String[] tokens) throws InvalidSplitException {
        int index = 1;
        String paidByUserId = tokens[index++];
        double amount = Double.parseDouble(tokens[index++]);
        int numParticipants = Integer.parseInt(tokens[index++]);

        List<String> participantIds = new ArrayList<>();
        for (int i = 0; i < numParticipants; i++) {
            participantIds.add(tokens[index++]);
        }

        SplitType splitType = SplitType.valueOf(tokens[index++].toUpperCase());

        List<Double> splitValues = new ArrayList<>();
        if (splitType == SplitType.EXACT || splitType == SplitType.PERCENT) {
            for (int i = 0; i < numParticipants; i++) {
                splitValues.add(Double.parseDouble(tokens[index++]));
            }
        }

        StringBuilder descBuilder = new StringBuilder();
        for (int i = index; i < tokens.length; i++) {
            descBuilder.append(tokens[i]).append(" ");
        }
        String description = descBuilder.toString().trim();
        if (description.isEmpty()) {
            description = "Miscellaneous Expense";
        }

        expenseManager.addExpense(description, amount, paidByUserId, participantIds, splitType, splitValues);
        System.out.println("Expense '" + description + "' added successfully!");
    }
}
