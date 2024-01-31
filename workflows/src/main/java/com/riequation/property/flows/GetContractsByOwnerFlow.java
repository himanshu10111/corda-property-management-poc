package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.OwnerAgentPropertyContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow
@StartableByRPC
public class GetContractsByOwnerFlow extends FlowLogic<List<OwnerAgentPropertyContractState>> {
    private final UniqueIdentifier ownerId;

    public GetContractsByOwnerFlow(UniqueIdentifier ownerId) {
        this.ownerId = ownerId;
    }

    @Suspendable
    @Override
    public List<OwnerAgentPropertyContractState> call() throws FlowException {
        // Query for all unconsumed OwnerAgentPropertyContractState states
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        List<StateAndRef<OwnerAgentPropertyContractState>> states = getServiceHub().getVaultService()
                .queryBy(OwnerAgentPropertyContractState.class, generalCriteria)
                .getStates();

        // Filter the states to only include those where the ownerId matches
        return states.stream()
                .map(StateAndRef::getState)
                .map(TransactionState::getData)
                .filter(contractState -> contractState.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }
}
