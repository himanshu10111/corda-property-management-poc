package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.Vault.Page;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.Builder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;



@StartableByRPC
public class GetPropertiesOfOwnerFlow extends FlowLogic<List<PropertyState>> {
    private final UUID ownerAccountId;

    public GetPropertiesOfOwnerFlow(UUID ownerAccountId) {
        this.ownerAccountId = ownerAccountId;
    }

    @Suspendable
    @Override
    public List<PropertyState> call() throws FlowException {
        VaultService vaultService = getServiceHub().getVaultService();

        // Creating criteria to filter states based on the owner's unique identifier
        UniqueIdentifier linearId = new UniqueIdentifier(null, ownerAccountId);
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                Collections.singletonList(linearId.getId()),
                null,
                Vault.StateStatus.UNCONSUMED
        );

        // Fetch states from the vault that meet the criteria
        Page<PropertyState> results = vaultService.queryBy(PropertyState.class, criteria);

        // Return the list of PropertyState objects
        return results.getStates().stream()
                .map(stateAndRef -> stateAndRef.getState().getData())
                .collect(Collectors.toList());
    }
}
