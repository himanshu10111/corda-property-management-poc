package com.riequation.property.contracts;

import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

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

        if (outputState.getCurrentContractId() != null)
            throw new IllegalArgumentException("Current contract ID must be null for a new property");


        // Additional validations for the new fields
        validatePropertyState(outputState);
    }
    private void validatePropertyState(PropertyState state) {
        if (state.getAddress().trim().isEmpty())
            throw new IllegalArgumentException("Address cannot be empty");

        if (state.getPincode().trim().isEmpty())
            throw new IllegalArgumentException("Pincode cannot be empty");

        // Price validation
        try {
            double priceValue = Double.parseDouble(state.getPrice());
            if (priceValue <= 0) {
                throw new IllegalArgumentException("Price must be a positive value");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Price must be a valid number");
        }

        if (state.getOwnerName().trim().isEmpty())
            throw new IllegalArgumentException("Owner name cannot be empty");

        // Square feet validation
        try {
            double sqrtFeetValue = Double.parseDouble(state.getSqrtFeet());
            if (sqrtFeetValue <= 0) {
                throw new IllegalArgumentException("Square feet must be a positive value");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Square feet must be a valid number");
        }

        if (state.getAmenities() == null || state.getAmenities().isEmpty())
            throw new IllegalArgumentException("Amenities list cannot be empty");

        if (state.getPropertyType().trim().isEmpty())
            throw new IllegalArgumentException("Property type cannot be empty");

        if (state.getBhkInfo().trim().isEmpty())
            throw new IllegalArgumentException("BHK info cannot be empty");

        if (state.getDescription().trim().isEmpty())
            throw new IllegalArgumentException("Description cannot be empty");
    }

    public interface Commands extends CommandData {
        class Add implements Commands {}
    }
}
