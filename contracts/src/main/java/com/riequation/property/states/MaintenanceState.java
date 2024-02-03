package com.riequation.property.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.contracts.UniqueIdentifier;
import com.riequation.property.contracts.MaintenanceContract;
import java.util.Date;
import java.util.List;

@BelongsToContract(MaintenanceContract.class)
public class MaintenanceState implements LinearState {
    private final UniqueIdentifier propertyId;
    private final UniqueIdentifier agentId;
    private final String maintenanceDetails;
    private final Date maintenanceDate;
    private final String status; // e.g., Scheduled, In Progress, Completed
    private final UniqueIdentifier contractId; // Link to the OwnerAgentPropertyContractState
    private final UniqueIdentifier linearId;
    private final List<AbstractParty> participants;
    private final Double estimatedCost;
    private final String priority; // e.g., Low, Medium, High
    private final String type; // e.g., Electrical, Plumbing, HVAC
    private final String workDescription;

    public MaintenanceState(UniqueIdentifier propertyId, UniqueIdentifier agentId, String maintenanceDetails,
                            Date maintenanceDate, String status, UniqueIdentifier contractId,
                            UniqueIdentifier linearId, List<AbstractParty> participants,
                            Double estimatedCost, String priority, String type, String workDescription) {
        this.propertyId = propertyId;
        this.agentId = agentId;
        this.maintenanceDetails = maintenanceDetails;
        this.maintenanceDate = maintenanceDate;
        this.status = status;
        this.contractId = contractId;
        this.linearId = linearId;
        this.participants = participants;
        this.estimatedCost = estimatedCost;
        this.priority = priority;
        this.type = type;
        this.workDescription = workDescription;
    }

    // Getters for all fields


    public UniqueIdentifier getPropertyId() {
        return propertyId;
    }

    public UniqueIdentifier getAgentId() {
        return agentId;
    }

    public String getMaintenanceDetails() {
        return maintenanceDetails;
    }

    public Date getMaintenanceDate() {
        return maintenanceDate;
    }

    public String getStatus() {
        return status;
    }

    public UniqueIdentifier getContractId() {
        return contractId;
    }

    public String getPriority() {
        return priority;
    }

    public String getType() {
        return type;
    }

    public String getWorkDescription() {
        return workDescription;
    }

    public Double getEstimatedCost() {
        return estimatedCost;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }

}
