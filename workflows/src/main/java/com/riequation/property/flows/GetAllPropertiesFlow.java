package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.PropertyState;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.List;
import java.util.stream.Collectors;

@StartableByRPC
public class GetAllPropertiesFlow extends FlowLogic<List<PropertyState>> {

    @Suspendable
    @Override
    public List<PropertyState> call() throws FlowException {
        VaultService vaultService = getServiceHub().getVaultService();

        // Create a general criteria to fetch all unconsumed PropertyState's
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

        // Fetch all states that meet the criteria
        Vault.Page<PropertyState> results = vaultService.queryBy(PropertyState.class, criteria);

        // Return the list of PropertyState objects
        return results.getStates().stream()
                .map(stateAndRef -> stateAndRef.getState().getData())
                .collect(Collectors.toList());
    }
}
