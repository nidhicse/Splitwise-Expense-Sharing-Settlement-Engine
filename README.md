# Splitwise-Style Expense Sharing & Settlement Engine

A pure Core Java 17 expense-sharing engine and debt settlement system designed with standard Object-Oriented Design (OOD) patterns, custom exception hierarchies, and a greedy heap-based debt minimization algorithm.

---

## Architecture & Design

### 1. Strategy Pattern (`com.splitwise.strategy`)
Split logic is decoupled from expense creation via the `SplitStrategy` interface:
- **`EqualSplitStrategy`**: Divides total cost equally among participants, allocating residual fractional cents to the first participant to guarantee the sum equals the exact total.
- **`ExactSplitStrategy`**: Validates that explicit participant contributions sum to the total amount within a 0.01 floating-point tolerance.
- **`PercentSplitStrategy`**: Validates that assigned participant percentages sum to 100.0% and derives exact monetary shares.

### 2. Symmetrical Ledger (`com.splitwise.service.ExpenseManager`)
Tracks net balances via a nested map structure (`Map<String, Map<String, Double>>`):
- If `UserA` owes `UserB` ₹200, `balanceSheet.get("UserA").get("UserB") == 200.0` and `balanceSheet.get("UserB").get("UserA") == -200.0`.
- Ensures constant-time $O(1)$ debt lookup and updates while eliminating stale balance states.

### 3. Debt Simplification Algorithm (`com.splitwise.service.SettlementService`)
Reduces the number of transactions needed to settle all accounts across the network:
1. Calculates net balance for every participant: $\text{Net} = \sum \text{Credits} - \sum \text{Debits}$.
2. Partitions users into two **Max-Heaps (`PriorityQueue`)**:
   - `debtorsQueue`: Users with negative balances (sorted descending by absolute amount owed).
   - `creditorsQueue`: Users with positive balances (sorted descending by amount to receive).
3. Greedily matches the largest debtor with the largest creditor, settles $\min(\text{debt}, \text{credit})$, and re-inserts remainders until all balances reach zero.

---

## Directory Layout
```text
.
├── pom.xml
└── src
    ├── main
    │   └── java
    │       └── com
    │           └── splitwise
    │               ├── Main.java
    │               ├── exception
    │               │   └── InvalidSplitException.java
    │               ├── model
    │               │   ├── EqualSplit.java
    │               │   ├── ExactSplit.java
    │               │   ├── Expense.java
    │               │   ├── PercentSplit.java
    │               │   ├── Split.java
    │               │   ├── SplitType.java
    │               │   ├── Transaction.java
    │               │   └── User.java
    │               ├── service
    │               │   ├── ExpenseManager.java
    │               │   └── SettlementService.java
    │               └── strategy
    │                   ├── EqualSplitStrategy.java
    │                   ├── ExactSplitStrategy.java
    │                   ├── PercentSplitStrategy.java
    │                   └── SplitStrategy.java
    └── test
        └── java
            └── com
                └── splitwise
                    ├── TestRunner.java
                    ├── service
                    │   ├── ExpenseManagerTest.java
                    │   └── SettlementServiceTest.java
                    └── strategy
                        └── SplitStrategyTest.java
```

## How to Run

### 1. Compile the Project
Using Maven:
```bash
mvn clean compile
```

### 2. Run the Interactive CLI
The Main driver is pre-seeded with 4 users and 3 transactions so you can instantly test the system without manual data entry.
```bash
mvn exec:java -Dexec.mainClass="com.splitwise.Main"
```

### 3. Run the Tests
Run the robust JUnit 5 test suite:
```bash
mvn test
```
Or run the standalone custom test runner (zero external dependencies):
```bash
javac -d out $(find src/main -name "*.java") src/test/java/com/splitwise/TestRunner.java
java -ea -cp out com.splitwise.TestRunner
```
