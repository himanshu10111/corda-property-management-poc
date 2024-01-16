package com.riequation.property.contracts;

import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

public class PropertyContract implements Contract {
    public static final String ID = "com.riequation.property.contracts.PropertyContract";

    @Override
    public void verify(LedgerTransaction tx) {
        // Ensure there is exactly one command in the transaction
        if (tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have exactly one command");

        CommandData command = tx.getCommand(0).getValue();

        // Handle different commands such as RegisterProperty
        if (command instanceof Commands.RegisterProperty) {
            // Registration-specific constraints
            verifyRegister(tx);
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    private void verifyRegister(LedgerTransaction tx) {
        // Registration transaction should not have input states
        if (!tx.getInputStates().isEmpty())
            throw new IllegalArgumentException("Registration transaction should not have input states");

        // Registration transaction should have exactly one output state
        if (tx.getOutputStates().size() != 1)
            throw new IllegalArgumentException("Registration transaction should have exactly one output state");

        PropertyState state = (PropertyState) tx.getOutputStates().get(0);

        // Property address cannot be empty
        if (state.getAddress().trim().isEmpty())
            throw new IllegalArgumentException("Property address cannot be empty");

        // Property type cannot be empty
        if (state.getPropertyType().trim().isEmpty())
            throw new IllegalArgumentException("Property type cannot be empty");
    }

    public interface Commands extends CommandData {
        class RegisterProperty implements Commands {}

    }
}
