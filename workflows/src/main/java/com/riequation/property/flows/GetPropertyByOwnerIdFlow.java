package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import java.util.List;
import java.util.stream.Collectors;

@StartableByRPC
public class GetPropertyByOwnerIdFlow extends FlowLogic<List<StateAndRef<PropertyState>>> {
    private final UniqueIdentifier ownerId;

    public GetPropertyByOwnerIdFlow(UniqueIdentifier ownerId) {
        this.ownerId = ownerId;
    }

    @Suspendable
    @Override
    public List<StateAndRef<PropertyState>> call() throws FlowException {
        // Fetch all unconsumed PropertyState instances from the vault
        List<StateAndRef<PropertyState>> allProperties = getServiceHub().getVaultService().queryBy(PropertyState.class).getStates();

        // Filter the properties by the provided owner ID
        return allProperties.stream()
                .filter(stateAndRef -> stateAndRef.getState().getData().getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }
}
