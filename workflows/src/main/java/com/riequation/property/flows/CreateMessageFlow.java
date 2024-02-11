package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.MessageContract;
import com.riequation.property.states.MessageState;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import java.util.Collections;
import java.util.UUID; // Import UUID class

@InitiatingFlow
@StartableByRPC
public class CreateMessageFlow extends FlowLogic<String> { // Changed return type to String
    private final String refId; // Reference ID for grouping messages
    private final String messageTemplate;
    private final String placeholder1;
    private final String placeholder2;

    public CreateMessageFlow(String refId, String messageTemplate, String placeholder1, String placeholder2) {
        this.refId = refId;
        this.messageTemplate = messageTemplate;
        this.placeholder1 = placeholder1;
        this.placeholder2 = placeholder2;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        // Obtain a reference to the notary we want to use.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Generate a unique message ID using Java's UUID class
        String messageId = UUID.randomUUID().toString();

        // Create the output state with the newly generated message ID
        MessageState outputState = new MessageState(
                messageId, // Use the generated UUID as the message ID
                refId,
                messageTemplate,
                placeholder1,
                placeholder2,
                Collections.singletonList(getOurIdentity()));

        // Build the transaction
        TransactionBuilder builder = new TransactionBuilder(notary)
                .addOutputState(outputState, MessageContract.ID)
                .addCommand(new MessageContract.Commands.Create(), getOurIdentity().getOwningKey());

        // Verify the transaction
        builder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder);

        // Finalize the transaction and wait for the response
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));

        // Return the generated message ID
        return messageId;
    }
}
