package com.splitwise.strategy;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.PercentSplit;
import com.splitwise.model.Split;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.List;

public class PercentSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> calculateAndValidateSplits(double totalAmount, List<User> participants, List<Double> values) throws InvalidSplitException {
        if (participants == null || values == null || participants.size() != values.size()) {
            throw new InvalidSplitException("Participants and percentage values lists must be of the same size.");
        }

        double totalPercent = 0;
        for (Double percent : values) {
            totalPercent += percent;
        }

        if (Math.abs(totalPercent - 100.0) > 0.01) {
            throw new InvalidSplitException("The total percentage must equal 100.0.");
        }

        List<Split> splits = new ArrayList<>();
        double calculatedTotalAmount = 0;

        for (int i = 0; i < participants.size(); i++) {
            double percent = values.get(i);
            double amount = Math.round((totalAmount * percent / 100.0) * 100.0) / 100.0;

            PercentSplit split = new PercentSplit(participants.get(i), percent);

            if (i == 0) {
                // Defer setting the first participant's amount to adjust for rounding discrepancies later
                splits.add(split);
            } else {
                split.setAmount(amount);
                calculatedTotalAmount += amount;
                splits.add(split);
            }
        }

        if (!splits.isEmpty()) {
            PercentSplit firstSplit = (PercentSplit) splits.get(0);
            double remainingAmount = Math.round((totalAmount - calculatedTotalAmount) * 100.0) / 100.0;
            firstSplit.setAmount(remainingAmount);
        }

        return splits;
    }
}
