package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.MessageState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import java.util.List;
import java.util.stream.Collectors;

@StartableByRPC
public class GetMessageByIdFlow extends FlowLogic<List<MessageState>> {
    private final String refId;

    public GetMessageByIdFlow(String refId) {
        this.refId = refId;
    }

    @Override
    @Suspendable
    public List<MessageState> call() throws FlowException {
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<MessageState>> allStates = getServiceHub().getVaultService().queryBy(MessageState.class, criteria).getStates();

        List<MessageState> filteredStates = allStates.stream()
                .map(StateAndRef::getState) // No need to call .getData() here, it's implied.
                .map(transactionState -> transactionState.getData())
                .filter(messageState -> refId.equals(messageState.getRefId()))
                .collect(Collectors.toList());

        if (filteredStates.isEmpty()) {
            throw new FlowException("No messages found for refId: " + refId);
        }

        return filteredStates;
    }
}
