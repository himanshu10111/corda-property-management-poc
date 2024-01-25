package com.riequation.property.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.riequation.property.states.TenantState;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.Vault.Page;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;

@StartableByRPC
public class GetAllTenantsFlow extends FlowLogic<List<TenantState>> {

    @Suspendable
    @Override
    public List<TenantState> call() {
        // Use the ServiceHub directly to access the VaultService
        VaultService vaultService = getServiceHub().getVaultService();

        // We create a query criteria to fetch all unconsumed TenantState's from the vault.
        QueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED);

        // We set the page specification to retrieve results in smaller chunks if needed.
        PageSpecification pageSpec = new PageSpecification(DEFAULT_PAGE_NUM, Integer.MAX_VALUE);

        // Fetch states from the vault using pagination
        Page<TenantState> results = vaultService.queryBy(TenantState.class, queryCriteria, pageSpec);

        // Map the list of StateAndRef to TenantState
        return results.getStates().stream().map(stateAndRef -> stateAndRef.getState().getData()).collect(Collectors.toList());
    }
}
