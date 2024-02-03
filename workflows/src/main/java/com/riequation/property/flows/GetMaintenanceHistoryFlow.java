package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.MaintenanceState;
import net.corda.core.contracts.StateAndRef;
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

@InitiatingFlow
@StartableByRPC
public class GetMaintenanceHistoryFlow extends FlowLogic<List<MaintenanceState>> {
    private final UniqueIdentifier propertyId;

    public GetMaintenanceHistoryFlow(UniqueIdentifier propertyId) {
        this.propertyId = propertyId;
    }

    @Suspendable
    @Override
    public List<MaintenanceState> call() throws FlowException {
        // Define criteria to filter MaintenanceState by propertyId
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        // Correctly using UniqueIdentifier for querying
        QueryCriteria customCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null, // participants
                Collections.singletonList(propertyId), // list of UniqueIdentifier
                null, // status is already specified in generalCriteria
                null); // contractStateTypes, null because we're querying for a specific state class below

        QueryCriteria criteria = generalCriteria.and(customCriteria);

        // Execute the query
        List<StateAndRef<MaintenanceState>> maintenanceStates = getServiceHub().getVaultService()
                .queryBy(MaintenanceState.class, criteria).getStates();

        // Map the results to a list of MaintenanceState
        return maintenanceStates.stream()
                .map(StateAndRef::getState)
                .map(state -> state.getData())
                .collect(Collectors.toList());
    }
}
