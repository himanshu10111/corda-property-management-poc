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
    private final String address;
    private final String pincode;
    private final Double price;
    private final String ownerName;
    private final Double sqrtFeet;
    private final List<String> amenities;
    private final String propertyType;
    private final String bhkInfo;
    private final String description;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public AddPropertyFlow(String propertyDetails, UniqueIdentifier ownerId, String address, String pincode,
                           Double price, String ownerName, Double sqrtFeet, List<String> amenities,
                           String propertyType, String bhkInfo, String description) {
        this.propertyDetails = propertyDetails;
        this.ownerId = ownerId;
        this.address = address;
        this.pincode = pincode;
        this.price = price;
        this.ownerName = ownerName;
        this.sqrtFeet = sqrtFeet;
        this.amenities = amenities;
        this.propertyType = propertyType;
        this.bhkInfo = bhkInfo;
        this.description = description;
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

        // Initialize currentContractId as null when first creating the property
        UniqueIdentifier currentContractId = null;

        // Create the output state with new fields
        PropertyState outputState = new PropertyState(propertyDetails, ownerId, getOurIdentity(), propertyLinearId,
                address, pincode, price, ownerName, sqrtFeet, amenities,
                propertyType, bhkInfo, description, currentContractId);

        // Build the transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, PropertyContract.ID)
                .addCommand(new Command<>(new PropertyContract.Commands.Add(), getOurIdentity().getOwningKey()));

        // Verify the transaction
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));
        return propertyLinearId;
    }

    private boolean isOwnerRegistered(UniqueIdentifier ownerId) {
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
