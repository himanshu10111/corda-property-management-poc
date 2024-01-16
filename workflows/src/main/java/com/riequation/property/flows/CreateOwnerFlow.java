package com.riequation.property.flows;


import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.contracts.OwnerContract;
import com.riequation.property.states.OwnerState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.flows.FlowException;
import net.corda.core.contracts.CommandData;



import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow
@StartableByRPC
public class CreateOwnerFlow extends FlowLogic<UniqueIdentifier> {
    private final String name;
    private final String email;
    private final String password; // Hashed password
    private final String mobileNumber;
    private final String address;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public CreateOwnerFlow(String name, String email, String password, String mobileNumber, String address) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.mobileNumber = mobileNumber;
        this.address = address;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public UniqueIdentifier call() throws FlowException {

        validateUniqueEmailAndMobile();
        // Obtain the notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Generate a unique identifier for the state (consider using a more robust mechanism)
        UniqueIdentifier linearId = new UniqueIdentifier();

        // Create the output state
        OwnerState outputState = new OwnerState(name, email, password, mobileNumber, address, getOurIdentity(), linearId);

        // Build the transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, OwnerContract.ID)
                .addCommand(new Command<>(new OwnerContract.Commands.Register(), getOurIdentity().getOwningKey()));

        // Verify the transaction
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalize the transaction and return the linearId
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));
        return linearId;
    }
    private void validateUniqueEmailAndMobile() throws FlowException {
        VaultService vaultService = getServiceHub().getVaultService();
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria();

        // Fetch all unconsumed OwnerState instances from the vault
        List<StateAndRef<OwnerState>> ownerStates = vaultService.queryBy(OwnerState.class, criteria).getStates();

        // Check if any state has the same email or mobile number
        for (StateAndRef<OwnerState> stateRef : ownerStates) {
            OwnerState state = stateRef.getState().getData();
            if (email.equals(state.getEmail())) {
                throw new FlowException("An owner with the email " + email + " already exists.");
            }
            if (mobileNumber.equals(state.getMobileNumber())) {
                throw new FlowException("An owner with the mobile number " + mobileNumber + " already exists.");
            }
        }
    }


}
