package com.splitwise.strategy;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.Split;
import com.splitwise.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SplitStrategyTest {

    private User u1, u2, u3;
    private SplitStrategy equalStrategy, exactStrategy, percentStrategy;

    @BeforeEach
    void setUp() {
        u1 = new User("U1", "Alice", "a@test.com", "111");
        u2 = new User("U2", "Bob", "b@test.com", "222");
        u3 = new User("U3", "Charlie", "c@test.com", "333");

        equalStrategy = new EqualSplitStrategy();
        exactStrategy = new ExactSplitStrategy();
        percentStrategy = new PercentSplitStrategy();
    }

    @Test
    void testEqualSplitEvenlyDivisible() throws InvalidSplitException {
        List<User> participants = Arrays.asList(u1, u2, u3);
        List<Split> splits = equalStrategy.calculateAndValidateSplits(300.0, participants, null);
        
        assertEquals(3, splits.size());
        assertEquals(100.0, splits.get(0).getAmount());
        assertEquals(100.0, splits.get(1).getAmount());
        assertEquals(100.0, splits.get(2).getAmount());
    }

    @Test
    void testEqualSplitFractionalPennyDistribution() throws InvalidSplitException {
        List<User> participants = Arrays.asList(u1, u2, u3);
        List<Split> splits = equalStrategy.calculateAndValidateSplits(100.0, participants, null);
        
        assertEquals(3, splits.size());
        assertEquals(33.34, splits.get(0).getAmount()); // Fractional remainder given to first participant
        assertEquals(33.33, splits.get(1).getAmount());
        assertEquals(33.33, splits.get(2).getAmount());
        
        double sum = splits.stream().mapToDouble(Split::getAmount).sum();
        assertEquals(100.0, Math.round(sum * 100.0) / 100.0);
    }

    @Test
    void testExactSplitValid() throws InvalidSplitException {
        List<User> participants = Arrays.asList(u1, u2);
        List<Double> values = Arrays.asList(200.0, 300.0);
        
        List<Split> splits = exactStrategy.calculateAndValidateSplits(500.0, participants, values);
        
        assertEquals(2, splits.size());
        assertEquals(200.0, splits.get(0).getAmount());
        assertEquals(300.0, splits.get(1).getAmount());
    }

    @Test
    void testExactSplitSumMismatchThrowsException() {
        List<User> participants = Arrays.asList(u1, u2);
        List<Double> values = Arrays.asList(200.0, 300.0);
        
        assertThrows(InvalidSplitException.class, () -> 
            exactStrategy.calculateAndValidateSplits(600.0, participants, values)
        );
    }

    @Test
    void testPercentSplitValid() throws InvalidSplitException {
        List<User> participants = Arrays.asList(u1, u2);
        List<Double> percentages = Arrays.asList(40.0, 60.0);
        
        List<Split> splits = percentStrategy.calculateAndValidateSplits(1000.0, participants, percentages);
        
        assertEquals(2, splits.size());
        assertEquals(400.0, splits.get(0).getAmount());
        assertEquals(600.0, splits.get(1).getAmount());
    }

    @Test
    void testPercentSplitInvalidSumThrowsException() {
        List<User> participants = Arrays.asList(u1, u2);
        List<Double> percentages = Arrays.asList(40.0, 70.0); // Sums to 110%
        
        assertThrows(InvalidSplitException.class, () -> 
            percentStrategy.calculateAndValidateSplits(1000.0, participants, percentages)
        );
    }

    @Test
    void testMismatchedParticipantAndValueSizesThrowsException() {
        List<User> participants = Arrays.asList(u1, u2);
        List<Double> values = Arrays.asList(200.0); // Only 1 value for 2 participants
        
        assertThrows(InvalidSplitException.class, () -> 
            exactStrategy.calculateAndValidateSplits(500.0, participants, values)
        );
        
        assertThrows(InvalidSplitException.class, () -> 
            percentStrategy.calculateAndValidateSplits(500.0, participants, values)
        );
    }
}
