package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.TenantContract;
import com.riequation.property.states.TenantState;
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
public class ChangeTenantPasswordFlow extends FlowLogic<Void> {
    private final UniqueIdentifier tenantId;
    private final String newPassword;
    private final String confirmPassword;

    public ChangeTenantPasswordFlow(String tenantId, String newPassword, String confirmPassword) {
        // Assuming tenantId is a UUID string
        this.tenantId = UniqueIdentifier.Companion.fromString(tenantId);
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

        // Create criteria to find the TenantState by its linearId (UniqueIdentifier)
        QueryCriteria criteria = new LinearStateQueryCriteria(null, Collections.singletonList(tenantId.getId()), null, Vault.StateStatus.UNCONSUMED);

        // Execute the query
        List<StateAndRef<TenantState>> results = getServiceHub().getVaultService().queryBy(TenantState.class, criteria).getStates();

        if (results.isEmpty()) {
            throw new FlowException("TenantState with ID " + tenantId + " not found.");
        }

        StateAndRef<TenantState> stateRef = results.get(0);
        TenantState inputState = stateRef.getState().getData();

        // Create a new TenantState with the updated password
        TenantState updatedState = new TenantState(inputState.getName(), inputState.getEmail(), newPassword, inputState.getMobileNumber(), inputState.getAddress(), inputState.getHost(), tenantId);

        // Building the transaction
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(stateRef)
                .addOutputState(updatedState, TenantContract.ID)
                .addCommand(new Command<>(new TenantContract.Commands.Update(), getOurIdentity().getOwningKey()));

        // Verify the transaction
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));

        return null;
    }
}
