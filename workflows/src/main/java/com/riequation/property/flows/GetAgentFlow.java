package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.AgentState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.contracts.UniqueIdentifier;

import java.util.Collections;
import java.util.List;

@StartableByRPC
public class GetAgentFlow extends FlowLogic<AgentState> {
    private final UniqueIdentifier linearId;

    public GetAgentFlow(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    @Suspendable
    @Override
    public AgentState call() throws FlowException {
        VaultService vaultService = getServiceHub().getVaultService();

        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                Collections.singletonList(linearId.getId())
        );

        List<StateAndRef<AgentState>> agentStates = vaultService.queryBy(AgentState.class, criteria).getStates();

        if (agentStates.size() != 1) {
            throw new FlowException("Agent not found or ambiguous results for ID: " + linearId);
        }

        return agentStates.get(0).getState().getData();
    }
}
