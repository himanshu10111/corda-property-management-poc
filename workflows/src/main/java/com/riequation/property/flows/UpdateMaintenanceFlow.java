package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.MaintenanceContract;
import com.riequation.property.states.MaintenanceState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Collections;
import java.util.Date;

@InitiatingFlow
@StartableByRPC
public class UpdateMaintenanceFlow extends FlowLogic<Void> {
    private final UniqueIdentifier maintenanceId;
    private final String maintenanceDetails;
    private final Date maintenanceDate;
    private final String status;
    private final String estimatedCost;
    private final String priority;
    private final String type;
    private final String workDescription;

    public UpdateMaintenanceFlow(UniqueIdentifier maintenanceId, String maintenanceDetails, Date maintenanceDate,
                                 String status, String estimatedCost, String priority,
                                 String type, String workDescription) {
        this.maintenanceId = maintenanceId;
        this.maintenanceDetails = maintenanceDetails;
        this.maintenanceDate = maintenanceDate;
        this.status = status;
        this.estimatedCost = estimatedCost;
        this.priority = priority;
        this.type = type;
        this.workDescription = workDescription;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // Query the vault to find the input state
        StateAndRef<MaintenanceState> inputStateRef = getServiceHub().getVaultService()
                .queryBy(MaintenanceState.class)
                .getStates()
                .stream()
                .filter(sr -> sr.getState().getData().getLinearId().equals(maintenanceId))
                .findAny()
                .orElseThrow(() -> new FlowException("MaintenanceState with ID " + maintenanceId + " not found."));

        MaintenanceState inputState = inputStateRef.getState().getData();

        // Create the updated maintenance state with the new values
        MaintenanceState updatedState = new MaintenanceState(
                inputState.getPropertyId(),
                inputState.getAgentId(),
                maintenanceDetails,
                maintenanceDate, // Updated to include maintenanceDate
                status,
                inputState.getContractId(),
                inputState.getLinearId(),
                inputState.getParticipants(),
                estimatedCost,
                priority,
                type,
                workDescription
        );

        // Building the transaction
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(inputStateRef)
                .addOutputState(updatedState, MaintenanceContract.ID)
                .addCommand(new MaintenanceContract.Commands.Update(), getOurIdentity().getOwningKey());

        // Verifying the transaction
        txBuilder.verify(getServiceHub());

        // Signing the transaction
        SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalising the transaction
        subFlow(new FinalityFlow(partSignedTx, Collections.emptyList()));

        return null;
    }
}
