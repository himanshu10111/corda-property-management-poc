package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.AgentContract;
import com.riequation.property.states.AgentState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.contracts.Command;
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;

import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class ChangeAgentPasswordFlow extends FlowLogic<Void> {
    private final UniqueIdentifier agentId;
    private final String newPassword;
    private final String confirmPassword;

    public ChangeAgentPasswordFlow(String agentId, String newPassword, String confirmPassword) {
        // Assuming agentId is a UUID string
        this.agentId = UniqueIdentifier.Companion.fromString(agentId);
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new FlowException("New password and confirm password do not match.");
        }

        // Create criteria to find the AgentState by its linearId (UniqueIdentifier)
        QueryCriteria criteria = new LinearStateQueryCriteria(null, Collections.singletonList(agentId.getId()), null, Vault.StateStatus.UNCONSUMED);

        // Execute the query
        List<StateAndRef<AgentState>> results = getServiceHub().getVaultService().queryBy(AgentState.class, criteria).getStates();

        if (results.isEmpty()) {
            throw new FlowException("AgentState with ID " + agentId + " not found.");
        }

        StateAndRef<AgentState> stateRef = results.get(0);
        AgentState inputState = stateRef.getState().getData();

        // Create a new AgentState with the updated password
        AgentState updatedState = new AgentState(inputState.getName(), inputState.getEmail(), newPassword, inputState.getMobileNumber(), inputState.getAddress(), inputState.getHost(), agentId);

        // Building the transaction
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(stateRef)
                .addOutputState(updatedState, AgentContract.ID)
                .addCommand(new Command<>(new AgentContract.Commands.Update(), getOurIdentity().getOwningKey()));

        // Verify the transaction
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));

        return null;
    }
}
