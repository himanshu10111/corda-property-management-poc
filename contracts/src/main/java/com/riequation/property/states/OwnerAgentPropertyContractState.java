package com.riequation.property.states;

import com.riequation.property.contracts.OwnerAgentPropertyContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import java.util.List;
import java.util.Date;

@BelongsToContract(OwnerAgentPropertyContract.class)
public class OwnerAgentPropertyContractState implements LinearState {
    private final UniqueIdentifier ownerId;
    private final UniqueIdentifier agentId;
    private final UniqueIdentifier propertyId;
    private final Date startDate;
    private final Date endDate;
    private final String contractDetails; // JSON or specific class
    private final String status;
    private final UniqueIdentifier linearId;
    private final List<AbstractParty> participants;

    public OwnerAgentPropertyContractState(UniqueIdentifier ownerId, UniqueIdentifier agentId, UniqueIdentifier propertyId,
                                           Date startDate, Date endDate, String contractDetails, String status,
                                           UniqueIdentifier linearId, List<AbstractParty> participants) {
        this.ownerId = ownerId;
        this.agentId = agentId;
        this.propertyId = propertyId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.contractDetails = contractDetails;
        this.status = status;
        this.linearId = linearId;
        this.participants = participants;
    }

    // Getters
    public UniqueIdentifier getOwnerId() { return ownerId; }
    public UniqueIdentifier getAgentId() { return agentId; }
    public UniqueIdentifier getPropertyId() { return propertyId; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public String getContractDetails() { return contractDetails; }
    public String getStatus() { return status; }
    @Override
    public UniqueIdentifier getLinearId() { return linearId; }
    @Override
    public List<AbstractParty> getParticipants() { return participants; }


}
