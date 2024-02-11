package com.riequation.property.contracts;


import com.riequation.property.states.MessageState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class MessageContract implements Contract {
    public static final String ID = "com.riequation.property.contracts.MessageContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        if (tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        CommandData command = tx.getCommand(0).getValue();

        if (command instanceof Commands.Create) {
            // Validation for Create command
            if (tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException("Create transaction must have one output");

            MessageState outputState = tx.outputsOfType(MessageState.class).get(0);
            if (outputState.getMessageTemplate().isEmpty())
                throw new IllegalArgumentException("Message template cannot be empty");
            if (outputState.getPlaceholder1().isEmpty() || outputState.getPlaceholder2().isEmpty())
                throw new IllegalArgumentException("Placeholders cannot be empty");
        }
        // Add more conditions here for other commands like Update and Delete
    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Update implements Commands {}
        class Delete implements Commands {}
    }
}
