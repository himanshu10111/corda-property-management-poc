package com.riequation.property.contracts;

import com.riequation.property.states.OwnerState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import java.util.List;

public class OwnerContract implements Contract {
    public static final String ID = "com.riequation.property.contracts.OwnerContract";

    @Override
    public void verify(LedgerTransaction tx) {
        if (tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        CommandData command = tx.getCommand(0).getValue();

        if (command instanceof Commands.Register) {
            // Registration-specific constraints
            verifyRegister(tx);
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

    public interface Commands extends CommandData {
        class Register implements Commands {}
    }
}


