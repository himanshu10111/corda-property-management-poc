package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Collections;
import java.util.List;

@StartableByRPC
public class GetPropertyFlow extends FlowLogic<PropertyState> {
    private final UniqueIdentifier linearId;

    public GetPropertyFlow(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    @Suspendable
    @Override
    public PropertyState call() throws FlowException {
        VaultService vaultService = getServiceHub().getVaultService();

        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                Collections.singletonList(linearId.getId())
        );

        List<StateAndRef<PropertyState>> propertyStates = vaultService.queryBy(PropertyState.class, criteria).getStates();

        if (propertyStates.size() != 1) {
            throw new FlowException("Property not found or ambiguous results for ID: " + linearId);
        }

        return propertyStates.get(0).getState().getData();
    }
}
