package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.MaintenanceState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault.Page;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@StartableByRPC
public class QueryMaintenanceFlow extends FlowLogic<List<MaintenanceState>> {
    private final String propertyId;
    private final String contractId;
    private final String agentId;

    public QueryMaintenanceFlow(String propertyId, String contractId, String agentId) {
        this.propertyId = propertyId;
        this.contractId = contractId;
        this.agentId = agentId;
    }

    @Suspendable
    @Override
    public List<MaintenanceState> call() throws FlowException {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria();

        Page<MaintenanceState> results = getServiceHub().getVaultService().queryBy(MaintenanceState.class, generalCriteria);
        return results.getStates().stream()
                .map(StateAndRef::getState)
                .map(stateAndRef -> stateAndRef.getData())
                .filter(maintenanceState ->
                        (propertyId == null || maintenanceState.getPropertyId().getId().toString().equals(propertyId)) &&
                                (contractId == null || maintenanceState.getContractId().getId().toString().equals(contractId)) &&
                                (agentId == null || maintenanceState.getAgentId().getId().toString().equals(agentId))
                )
                .collect(Collectors.toList());
    }
}
