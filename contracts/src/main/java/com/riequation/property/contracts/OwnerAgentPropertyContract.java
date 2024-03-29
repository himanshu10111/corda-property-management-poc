package com.riequation.property.contracts;

import com.riequation.property.states.OwnerAgentPropertyContractState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import java.util.Date;

public class OwnerAgentPropertyContract implements Contract {
    public static final String ID = "com.riequation.property.contracts.OwnerAgentPropertyContract";

    @Override
    public void verify(LedgerTransaction tx) {
        if (tx.getCommands().size() != 1) {
            throw new IllegalArgumentException("Transaction must have one command");
        }

        CommandData command = tx.getCommand(0).getValue();

        if (command instanceof Commands.Create) {
            verifyCreate(tx);
        } else if (command instanceof Commands.Amend) {
            verifyAmend(tx);
        } else if (command instanceof Commands.Terminate) {
            verifyTerminate(tx);
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    private void verifyCreate(LedgerTransaction tx) {
        // Validations for Create Contract transaction
        if (tx.getInputStates().size() != 0) {
            throw new IllegalArgumentException("Create contract transaction must have no input states");
        }
        if (tx.getOutputStates().size() != 1) {
            throw new IllegalArgumentException("Create contract transaction must have one output state");
        }

        OwnerAgentPropertyContractState outputState = (OwnerAgentPropertyContractState) tx.getOutputStates().get(0);

        // Validate date range
        if (!outputState.isDateRangeValid()) {
            throw new IllegalArgumentException("Contract start date must be before end date");
        }

        // Check for status
        if (outputState.getStatus() == null || outputState.getStatus().isEmpty()) {
            throw new IllegalArgumentException("Contract must have a status");
        }


    }



    private void verifyAmend(LedgerTransaction tx) {
        if (tx.getInputStates().size() != 1 || tx.getOutputStates().size() != 1) {
            throw new IllegalArgumentException("Amend contract transaction must have one input and one output state");
        }
        OwnerAgentPropertyContractState inputState = (OwnerAgentPropertyContractState) tx.getInputStates().get(0);
        OwnerAgentPropertyContractState outputState = (OwnerAgentPropertyContractState) tx.getOutputStates().get(0);

        if (!inputState.getLinearId().equals(outputState.getLinearId())) {
            throw new IllegalArgumentException("Linear ID cannot change");
        }
        if (!outputState.getStartDate().before(outputState.getEndDate())) {
            throw new IllegalArgumentException("Contract end date must be after start date");
        }

    }

    private void verifyTerminate(LedgerTransaction tx) {
        // Validate that the transaction has exactly one input state
        if (tx.getInputStates().size() != 1) {
            throw new IllegalArgumentException("Terminate contract transaction must have one input state");
        }

        // Validate that the transaction has no output states
        if (tx.getOutputStates().size() != 0) {
            throw new IllegalArgumentException("Terminate contract transaction must have no output states");
        }

        // Retrieve the input state
        OwnerAgentPropertyContractState inputState = (OwnerAgentPropertyContractState) tx.getInputStates().get(0);

        // Validate the status of the contract
        if (!inputState.getStatus().equals("Terminated")) {
            throw new IllegalArgumentException("Status must be set to 'Terminated' to terminate the contract");
        }

        // Check if the contract's end date is not after the current date
        if (inputState.getEndDate().after(new Date())) {
            throw new IllegalArgumentException("Cannot terminate a contract before its end date");
        }

        // Additional validations can be added as per specific business logic
    }


    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Amend implements Commands {}
        class Terminate implements Commands {}
    }
}
