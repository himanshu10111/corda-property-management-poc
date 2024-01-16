package com.riequation.property.states;

import com.riequation.property.contracts.PropertyContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;

@BelongsToContract(PropertyContract.class)
public class PropertyState implements LinearState {
    private final String address;
    private final String propertyType;
    private final UUID ownerAccountId; // Account identifier
    private final UniqueIdentifier linearId;
    private final Party host;

    public PropertyState(String address, String propertyType, UUID ownerAccountId, UniqueIdentifier linearId, Party host) {
        this.address = address;
        this.propertyType = propertyType;
        this.ownerAccountId = ownerAccountId;
        this.linearId = linearId;
        this.host = host;
    }

    // Getters
    public String getAddress() {
        return address;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public UUID getOwnerAccountId() {
        return ownerAccountId;
    }

    public Party getHost() {
        return host;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        // The host node is the participant in the transaction
        return Collections.singletonList(host);
    }
}