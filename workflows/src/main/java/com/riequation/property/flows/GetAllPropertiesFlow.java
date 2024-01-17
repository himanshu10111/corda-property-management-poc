package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.PropertyState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import java.util.List;

@StartableByRPC
public class GetAllPropertiesFlow extends FlowLogic<List<StateAndRef<PropertyState>>> {

    @Suspendable
    @Override
    public List<StateAndRef<PropertyState>> call() throws FlowException {
        // Fetch all unconsumed PropertyState instances from the vault
        return getServiceHub().getVaultService().queryBy(PropertyState.class).getStates();
    }
}
