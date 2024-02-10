package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.OwnerContract;
import com.riequation.property.states.OwnerState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.contracts.Command;
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;

import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class ChangeOwnerPasswordFlow extends FlowLogic<Void> {
    private final UniqueIdentifier ownerId;
    private final String newPassword;
    private final String confirmPassword;

    public ChangeOwnerPasswordFlow(String ownerId, String newPassword, String confirmPassword) {
        // Assuming ownerId is a UUID string
        this.ownerId = UniqueIdentifier.Companion.fromString(ownerId);
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new FlowException("New password and confirm password do not match.");
        }

        // Create criteria to find the OwnerState by its linearId (UniqueIdentifier)
        QueryCriteria criteria = new LinearStateQueryCriteria(null, Collections.singletonList(ownerId.getId()), null, Vault.StateStatus.UNCONSUMED);

        // Execute the query
        List<StateAndRef<OwnerState>> results = getServiceHub().getVaultService().queryBy(OwnerState.class, criteria).getStates();

        if (results.isEmpty()) {
            throw new FlowException("OwnerState with ID " + ownerId + " not found.");
        }

        StateAndRef<OwnerState> stateRef = results.get(0);
        OwnerState inputState = stateRef.getState().getData();

        // Create a new OwnerState with the updated password
        OwnerState updatedState = new OwnerState(inputState.getName(), inputState.getEmail(), newPassword, inputState.getMobileNumber(), inputState.getAddress(), inputState.getHost(), ownerId);

        // Building the transaction
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(stateRef)
                .addOutputState(updatedState, OwnerContract.ID)
                .addCommand(new Command<>(new OwnerContract.Commands.Update(), getOurIdentity().getOwningKey()));

        // Verify the transaction
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));

        return null;
    }
}
