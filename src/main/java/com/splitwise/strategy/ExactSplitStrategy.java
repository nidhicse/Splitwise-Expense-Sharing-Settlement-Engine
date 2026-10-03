package com.splitwise.strategy;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.ExactSplit;
import com.splitwise.model.Split;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.List;

public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> calculateAndValidateSplits(double totalAmount, List<User> participants, List<Double> values) throws InvalidSplitException {
        if (participants == null || values == null || participants.size() != values.size()) {
            throw new InvalidSplitException("Participants and values lists must be of the same size.");
        }

        double calculatedTotal = 0;
        List<Split> splits = new ArrayList<>();

        for (int i = 0; i < participants.size(); i++) {
            double amount = values.get(i);
            splits.add(new ExactSplit(participants.get(i), amount));
            calculatedTotal += amount;
        }

        if (Math.abs(calculatedTotal - totalAmount) > 0.01) {
            throw new InvalidSplitException("The sum of exact split values must exactly equal the total amount.");
        }

        return splits;
    }
}
