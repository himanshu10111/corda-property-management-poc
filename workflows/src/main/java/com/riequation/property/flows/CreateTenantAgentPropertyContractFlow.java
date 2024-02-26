package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.TenantAgentPropertyContract;
import com.riequation.property.states.TenantAgentPropertyContractState;
import com.riequation.property.states.TenantState;
import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow
@StartableByRPC
public class CreateTenantAgentPropertyContractFlow extends FlowLogic<UniqueIdentifier> {
    private final UniqueIdentifier tenantId;
    private final UniqueIdentifier agentId;
    private final UniqueIdentifier propertyId;
    private final Date startDate;
    private final Date endDate;
    private final String contractDetails;
    private final String status;
    private final List<UniqueIdentifier> validAgentIds;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public CreateTenantAgentPropertyContractFlow(UniqueIdentifier tenantId, UniqueIdentifier agentId,
                                                 UniqueIdentifier propertyId, Date startDate, Date endDate,
                                                 String contractDetails, String status,
                                                 List<UniqueIdentifier> validAgentIds) {
        this.tenantId = tenantId;
        this.agentId = agentId;
        this.propertyId = propertyId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.contractDetails = contractDetails;
        this.status = status;
        this.validAgentIds = validAgentIds;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public UniqueIdentifier call() throws FlowException {
        // Validate the tenant and property registration
        validateRegisteredEntities(tenantId, propertyId);

        // Validate the agent ID against the provided list of valid agent IDs
//        if (!validAgentIds.contains(agentId)) {
//            throw new FlowException("Agent not found or not registered");
//        }

        // Check if the property is already assigned to another agent during the specified period
        if (isPropertyAlreadyAssigned(propertyId, startDate, endDate)) {
            throw new FlowException("Property is already assigned to another agent during this period");
        }

        // Obtain a reference to the notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Create the output state
        TenantAgentPropertyContractState outputState = new TenantAgentPropertyContractState(
                tenantId, agentId, propertyId, startDate, endDate, contractDetails,
                status, new UniqueIdentifier(),
                Arrays.asList(getOurIdentity()));

        // Build the transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, TenantAgentPropertyContract.ID)
                .addCommand(new Command<>(new TenantAgentPropertyContract.Commands.Create(), getOurIdentity().getOwningKey()));

        // Verify and sign the transaction
        txBuilder.verify(getServiceHub());
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction
        subFlow(new FinalityFlow(signedTx, Arrays.asList()));

        // Return the linear ID of the created state
        return outputState.getLinearId();
    }

    private void validateRegisteredEntities(UniqueIdentifier tenantId, UniqueIdentifier propertyId) throws FlowException {
//        if (!isEntityRegistered(TenantState.class, tenantId)) {
//            throw new FlowException("Tenant not found or not registered");
//        }
//        if (!isEntityRegistered(PropertyState.class, propertyId)) {
//            throw new FlowException("Property not found or not registered");
//        }
    }

    private <T extends ContractState> boolean isEntityRegistered(Class<T> entityClass, UniqueIdentifier id) {
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(null,
                Arrays.asList(id),
                Vault.StateStatus.UNCONSUMED,
                null);
        List<StateAndRef<T>> results = getServiceHub().getVaultService().queryBy(entityClass, criteria).getStates();
        return !results.isEmpty();
    }

    private boolean isPropertyAlreadyAssigned(UniqueIdentifier propertyId, Date startDate, Date endDate) {
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(null, null, Vault.StateStatus.UNCONSUMED, null);
        List<StateAndRef<TenantAgentPropertyContractState>> contractStates = getServiceHub().getVaultService()
                .queryBy(TenantAgentPropertyContractState.class, criteria)
                .getStates();

        return contractStates.stream()
                .anyMatch(state -> {
                    TenantAgentPropertyContractState contract = state.getState().getData();
                    return contract.getPropertyId().equals(propertyId)
                            && !contract.getEndDate().before(startDate)
                            && !contract.getStartDate().after(endDate);
                });
    }
}
