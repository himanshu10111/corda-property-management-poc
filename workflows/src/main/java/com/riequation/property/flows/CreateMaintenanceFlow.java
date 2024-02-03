package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.MaintenanceContract;
import com.riequation.property.states.MaintenanceState;
import com.riequation.property.states.AgentState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.utilities.ProgressTracker;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

// Additional imports as needed

@InitiatingFlow
@StartableByRPC
public class CreateMaintenanceFlow extends FlowLogic<UniqueIdentifier> {
    private final UniqueIdentifier propertyId;
    private final UniqueIdentifier agentId;
    private final String maintenanceDetails;
    private final Date maintenanceDate;
    private final String status;
    private final Double estimatedCost;
    private final String priority;
    private final String type;
    private final String workDescription;
    private final UniqueIdentifier contractId;
    private final List<UniqueIdentifier> validPropertyIds;

    // Define your ProgressTracker steps (if needed)
    private final ProgressTracker progressTracker = new ProgressTracker();

    public CreateMaintenanceFlow(UniqueIdentifier propertyId,
                                 UniqueIdentifier agentId,
                                 String maintenanceDetails,
                                 Date maintenanceDate,
                                 String status,
                                 Double estimatedCost,
                                 String priority,
                                 String type,
                                 String workDescription,
                                 UniqueIdentifier contractId,
                                 List<UniqueIdentifier> validPropertyIds) {
        this.propertyId = propertyId;
        this.agentId = agentId;
        this.maintenanceDetails = maintenanceDetails;
        this.maintenanceDate = maintenanceDate;
        this.status = status;
        this.estimatedCost = estimatedCost;
        this.priority = priority;
        this.type = type;
        this.workDescription = workDescription;
        this.contractId = contractId;
        this.validPropertyIds = validPropertyIds;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public UniqueIdentifier call() throws FlowException {
        // Check if the agent exists in the vault
        if (!isAgentPresent(agentId)) {
            throw new FlowException("Agent with ID " + agentId + " does not exist.");
        }
        // Check if the property exists in the vault
        if (!validPropertyIds.contains(propertyId)) {
            throw new FlowException("Property with ID " + propertyId + " is not valid or does not exist.");
        }
        // Ensure the maintenance date is not in the past
        if (maintenanceDate.before(new Date())) {
            throw new FlowException("Maintenance date cannot be in the past.");
        }

        // Remaining logic to create the MaintenanceState
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        MaintenanceState maintenanceState = new MaintenanceState(propertyId, agentId, maintenanceDetails, maintenanceDate,
                status, contractId, new UniqueIdentifier(), Arrays.asList(getOurIdentity()), estimatedCost, priority, type, workDescription);

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(maintenanceState, MaintenanceContract.ID)
                .addCommand(new MaintenanceContract.Commands.Add(), getOurIdentity().getOwningKey());

        txBuilder.verify(getServiceHub());
        SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        SignedTransaction finalizedTx = subFlow(new FinalityFlow(partSignedTx, Arrays.asList()));

        return maintenanceState.getLinearId();
    }

    private boolean isAgentPresent(UniqueIdentifier agentId) {
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(null, Arrays.asList(agentId.getId()), null, Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<AgentState>> results = getServiceHub().getVaultService().queryBy(AgentState.class, criteria).getStates();
        return !results.isEmpty();
    }
}
