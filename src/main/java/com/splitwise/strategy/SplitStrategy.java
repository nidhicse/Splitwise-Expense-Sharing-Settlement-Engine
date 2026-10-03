package com.splitwise.strategy;

import com.splitwise.exception.InvalidSplitException;
import com.splitwise.model.Split;
import com.splitwise.model.User;

import java.util.List;

public interface SplitStrategy {
    List<Split> calculateAndValidateSplits(double totalAmount, List<User> participants, List<Double> values) throws InvalidSplitException;
}
