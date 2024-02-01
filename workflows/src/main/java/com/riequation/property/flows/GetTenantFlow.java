package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.TenantState;
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
public class GetTenantFlow extends FlowLogic<TenantState> {
    private final UniqueIdentifier linearId;

    public GetTenantFlow(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    @Suspendable
    @Override
    public TenantState call() throws FlowException {
        VaultService vaultService = getServiceHub().getVaultService();

        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                Collections.singletonList(linearId.getId())
        );

        List<StateAndRef<TenantState>> tenantStates = vaultService.queryBy(TenantState.class, criteria).getStates();

        if (tenantStates.size() != 1) {
            throw new FlowException("Tenant not found or ambiguous results for ID: " + linearId);
        }

        return tenantStates.get(0).getState().getData();
    }
}
