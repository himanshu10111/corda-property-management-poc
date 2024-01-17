package com.riequation.property.states;

import com.riequation.property.contracts.PropertyContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.contracts.UniqueIdentifier;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(PropertyContract.class)
public class PropertyState implements LinearState {
    private final String propertyDetails;
    private final UniqueIdentifier ownerId; // References Owner's linearId
    private final UniqueIdentifier linearId; // Unique identifier for the property
    private final AbstractParty host; // The node hosting this property

    public PropertyState(String propertyDetails, UniqueIdentifier ownerId, AbstractParty host, UniqueIdentifier linearId) {
        this.propertyDetails = propertyDetails;
        this.ownerId = ownerId;
        this.host = host;
        this.linearId = linearId;
    }

    // Getters
    public String getPropertyDetails() { return propertyDetails; }
    public UniqueIdentifier getOwnerId() { return ownerId; }
    public AbstractParty getHost() { return host; }
    @Override
    public UniqueIdentifier getLinearId() { return linearId; }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(host);
    }
}
