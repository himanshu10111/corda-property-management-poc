package com.riequation.property.contracts;

import com.riequation.property.states.OwnerState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

public class OwnerContract implements Contract {
    public static final String ID = "com.riequation.property.contracts.OwnerContract";

    @Override
    public void verify(LedgerTransaction tx) {
        CommandData command = tx.getCommand(0).getValue();

        if (command instanceof Commands.Register) {
            verifyRegister(tx);
        } else if (command instanceof Commands.Update) {
            verifyUpdate(tx);
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    private void verifyRegister(LedgerTransaction tx) {
        if (tx.getInputStates().size() != 0)
            throw new IllegalArgumentException("Registration transaction must have no inputs");
        if (tx.getOutputStates().size() != 1)
            throw new IllegalArgumentException("Registration transaction must have one output");

        OwnerState state = (OwnerState) tx.getOutputStates().get(0);

        if (state.getName().trim().isEmpty())
            throw new IllegalArgumentException("Owner's name cannot be empty");

        if (!state.getEmail().contains("@"))
            throw new IllegalArgumentException("Email address is invalid");

        if (state.getPassword().trim().isEmpty())
            throw new IllegalArgumentException("Password cannot be empty");

        if (!state.getMobileNumber().matches("\\d{10}"))
            throw new IllegalArgumentException("Mobile number is invalid");

        if (state.getAddress().trim().isEmpty())
            throw new IllegalArgumentException("Address cannot be empty");
    }
    private void verifyUpdate(LedgerTransaction tx) {
        if (tx.getInputStates().size() != 1 || tx.getOutputStates().size() != 1)
            throw new IllegalArgumentException("Update transaction must have exactly one input and one output");

        OwnerState inputState = (OwnerState) tx.getInputStates().get(0);
        OwnerState outputState = (OwnerState) tx.getOutputStates().get(0);

        // Verify that non-password fields are unchanged
        if (!inputState.getName().equals(outputState.getName()) ||
                !inputState.getEmail().equals(outputState.getEmail()) ||
                !inputState.getMobileNumber().equals(outputState.getMobileNumber()) ||
                !inputState.getAddress().equals(outputState.getAddress()) ||
                !inputState.getHost().equals(outputState.getHost())) {
            throw new IllegalArgumentException("Only the password field can be updated");
        }

        // Additional checks or validation related to the password update could go here
        // For example, ensuring the new password meets certain complexity requirements
    }

    public interface Commands extends CommandData {
        class Register implements Commands {}
        class Update implements Commands {}
    }
}
