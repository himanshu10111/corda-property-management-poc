package com.riequation.property.contracts;

import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

public class PropertyContract implements Contract {
    public static final String ID = "com.riequation.property.contracts.PropertyContract";

    @Override
    public void verify(LedgerTransaction tx) {
        if (tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        CommandData command = tx.getCommand(0).getValue();

        if (command instanceof Commands.Add) {
            verifyAdd(tx);
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    private void verifyAdd(LedgerTransaction tx) {
        // Verify that the transaction has no input states
        if (tx.getInputStates().size() != 0)
            throw new IllegalArgumentException("Add property transaction must have no input states");

        // Verify that the transaction has exactly one output state
        if (tx.getOutputStates().size() != 1)
            throw new IllegalArgumentException("Add property transaction must have one output state");

        // Get the output state and perform specific checks
        PropertyState outputState = (PropertyState) tx.getOutputStates().get(0);

        // Perform checks on property details
        if (outputState.getPropertyDetails().trim().isEmpty())
            throw new IllegalArgumentException("Property details cannot be empty");

        // Additional validations can be added here. For example, checking if the owner is registered.
        // This might involve using a reference state to refer to the OwnerState or some other mechanism.
    }

    public interface Commands extends CommandData {
        class Add implements Commands {}
    }
}
