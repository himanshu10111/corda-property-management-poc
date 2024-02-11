package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.MessageContract;
import com.riequation.property.states.MessageState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class DeleteMessageFlow extends FlowLogic<Void> {
    private final String messageId;

    public DeleteMessageFlow(String messageId) {
        this.messageId = messageId;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        // Define a criteria to find the state by messageId
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<MessageState>> messageStates = getServiceHub().getVaultService().queryBy(MessageState.class, generalCriteria).getStates();

        // Filter the list for a matching messageId
        StateAndRef<MessageState> messageStateRef = messageStates.stream()
                .filter(stateAndRef -> stateAndRef.getState().getData().getMessageId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new FlowException("Message not found with ID: " + messageId));

        // Build the transaction to consume the message state
        TransactionBuilder txBuilder = new TransactionBuilder(messageStateRef.getState().getNotary())
                .addInputState(messageStateRef)
                .addCommand(new Command<>(new MessageContract.Commands.Delete(), getOurIdentity().getOwningKey()));

        // Verify and sign the transaction
        txBuilder.verify(getServiceHub());
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction, consuming the state
        subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));

        return null;
    }
}
