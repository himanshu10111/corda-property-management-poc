package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.PropertyContract;
import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Collections;
import java.util.UUID;

@InitiatingFlow
@StartableByRPC
public class AddPropertyFlow extends FlowLogic<UniqueIdentifier> {
    private final String address;
    private final String propertyType;
    private final UUID ownerAccountId;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public AddPropertyFlow(String address, String propertyType, UUID ownerAccountId) {
        this.address = address;
        this.propertyType = propertyType;
        this.ownerAccountId = ownerAccountId;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public UniqueIdentifier call() throws FlowException {
        // Obtain the notary from the network map cache
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Create the output state
        Party host = getOurIdentity(); // Node's identity
        PropertyState outputState = new PropertyState(address, propertyType, ownerAccountId, new UniqueIdentifier(), host);

        // Build the transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, PropertyContract.ID)
                .addCommand(new Command<>(new PropertyContract.Commands.RegisterProperty(), host.getOwningKey()));

        // Verify the transaction
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));

        // Return the linear ID of the new property
        return outputState.getLinearId();
    }
}
