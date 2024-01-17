package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.PropertyContract;
import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import com.riequation.property.states.OwnerState;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow
@StartableByRPC
public class AddPropertyFlow extends FlowLogic<UniqueIdentifier> {
    private final String propertyDetails;
    private final UniqueIdentifier ownerId;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public AddPropertyFlow(String propertyDetails, UniqueIdentifier ownerId) {
        this.propertyDetails = propertyDetails;
        this.ownerId = ownerId;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public UniqueIdentifier call() throws FlowException {
        // Ensure the calling party is a registered owner
        if (!isOwnerRegistered(ownerId)) {
            throw new FlowException("Only registered owners can add properties.");
        }

        // Obtain the notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Generate a unique identifier for the property
        UniqueIdentifier propertyLinearId = new UniqueIdentifier();

        // Create the output state
        PropertyState outputState = new PropertyState(propertyDetails, ownerId, getOurIdentity(), propertyLinearId);

        // Build the transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, PropertyContract.ID)
                .addCommand(new Command<>(new PropertyContract.Commands.Add(), getOurIdentity().getOwningKey()));

        // Verify the transaction
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction and return the linearId
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));
        return propertyLinearId;
    }

    private boolean isOwnerRegistered(UniqueIdentifier ownerId) throws FlowException {
        // Query the vault for OwnerState with the given ownerId
        List<StateAndRef<OwnerState>> ownerStates = getServiceHub().getVaultService()
                .queryBy(OwnerState.class)
                .getStates()
                .stream()
                .filter(stateAndRef -> stateAndRef.getState().getData().getLinearId().equals(ownerId))
                .collect(Collectors.toList());

        return !ownerStates.isEmpty();
    }
}
