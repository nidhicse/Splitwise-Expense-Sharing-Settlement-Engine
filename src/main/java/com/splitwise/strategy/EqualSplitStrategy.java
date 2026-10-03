package com.splitwise.strategy;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.EqualSplit;
import com.splitwise.model.Split;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.List;

public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> calculateAndValidateSplits(double totalAmount, List<User> participants, List<Double> values) throws InvalidSplitException {
        if (participants == null || participants.isEmpty()) {
            throw new InvalidSplitException("Participants list cannot be null or empty.");
        }

        int n = participants.size();
        double splitAmount = Math.round((totalAmount / n) * 100.0) / 100.0;

        List<Split> splits = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            EqualSplit split = new EqualSplit(participants.get(i));
            if (i == 0) {
                // Distribute any fractional cent rounding difference to the first participant
                double difference = Math.round((totalAmount - (splitAmount * (n - 1))) * 100.0) / 100.0;
                split.setAmount(difference);
            } else {
                split.setAmount(splitAmount);
            }
            splits.add(split);
        }

        return splits;
    }
}
