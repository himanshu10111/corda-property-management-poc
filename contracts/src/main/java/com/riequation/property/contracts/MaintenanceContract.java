package com.riequation.property.contracts;

import com.riequation.property.states.MaintenanceState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import java.util.Date;

public class MaintenanceContract implements Contract {
    public static final String ID = "com.riequation.property.contracts.MaintenanceContract";

    @Override
    public void verify(LedgerTransaction tx) {
        if (tx.getCommands().size() != 1) {
            throw new IllegalArgumentException("Transaction must have one command");
        }

        CommandData command = tx.getCommand(0).getValue();
        if (command instanceof Commands.Add) {
            verifyAdd(tx);
        } else if (command instanceof Commands.Update) {
            verifyUpdate(tx);
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    private void verifyAdd(LedgerTransaction tx) {
        if (tx.getInputStates().size() != 0) {
            throw new IllegalArgumentException("Add maintenance transaction must have no input states");
        }
        if (tx.getOutputStates().size() != 1) {
            throw new IllegalArgumentException("Add maintenance transaction must have one output state");
        }

        MaintenanceState outputState = (MaintenanceState) tx.getOutputStates().get(0);
        requireNonNullFields(outputState);
        if (!outputState.getStatus().equals("Scheduled")) {
            throw new IllegalArgumentException("New maintenance task must be initially set to 'Scheduled' status");
        }
        if (outputState.getMaintenanceDate().before(new Date())) {
            throw new IllegalArgumentException("Maintenance date cannot be in the past");
        }
    }

    private void verifyUpdate(LedgerTransaction tx) {
        if (tx.getInputStates().size() != 1 || tx.getOutputStates().size() != 1) {
            throw new IllegalArgumentException("Update maintenance transaction must have one input and one output state");
        }

        MaintenanceState inputState = (MaintenanceState) tx.getInputStates().get(0);
        MaintenanceState outputState = (MaintenanceState) tx.getOutputStates().get(0);

        if (!inputState.getLinearId().equals(outputState.getLinearId())) {
            throw new IllegalArgumentException("Linear ID of maintenance task cannot change");
        }
        requireNonNullFields(outputState);
        if (inputState.getStatus().equals("Completed")) {
            throw new IllegalArgumentException("Cannot update a maintenance task that is already completed");
        }
        if (outputState.getMaintenanceDate().before(new Date())) {
            throw new IllegalArgumentException("Updated maintenance date cannot be in the past");
        }
    }

    private void requireNonNullFields(MaintenanceState state) {
        if (state.getPropertyId() == null || state.getAgentId() == null || state.getContractId() == null ||
                state.getMaintenanceDetails() == null || state.getMaintenanceDate() == null ||
                state.getStatus() == null || state.getEstimatedCost() == null || state.getPriority() == null ||
                state.getType() == null || state.getWorkDescription() == null) {
            throw new IllegalArgumentException("All fields must be non-null");
        }
    }

    public interface Commands extends CommandData {
        class Add implements Commands {}
        class Update implements Commands {}
    }
}
