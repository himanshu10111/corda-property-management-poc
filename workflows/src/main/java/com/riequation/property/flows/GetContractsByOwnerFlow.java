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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

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
        UUID ownerUUID = ownerId.getId();
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(null, Collections.singletonList(ownerUUID))
                .and(generalCriteria);

        return getServiceHub().getVaultService()
                .queryBy(OwnerAgentPropertyContractState.class, criteria)
                .getStates()
                .stream()
                .map(StateAndRef::getState)
                .map(TransactionState::getData)
                .collect(Collectors.toList());
    }
}
