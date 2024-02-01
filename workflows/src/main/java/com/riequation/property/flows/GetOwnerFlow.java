package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.OwnerState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@StartableByRPC
public class GetOwnerFlow extends FlowLogic<OwnerState> {
    private final UniqueIdentifier linearId;

    public GetOwnerFlow(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    @Suspendable
    @Override
    public OwnerState call() throws FlowException {
        VaultService vaultService = getServiceHub().getVaultService();

        // Extract UUID from UniqueIdentifier
        UUID linearIdUUID = linearId.getId();

        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                Collections.singletonList(linearIdUUID) // Pass UUID instead of UniqueIdentifier
        );

        List<StateAndRef<OwnerState>> ownerStates = vaultService.queryBy(OwnerState.class, criteria).getStates();

        if (ownerStates.size() != 1) {
            throw new FlowException("Owner not found or ambiguous results for ID: " + linearId);
        }

        return ownerStates.get(0).getState().getData();
    }
}
