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
    private final UniqueIdentifier ownerId;
    private final UniqueIdentifier linearId;
    private final AbstractParty host;

    // New fields
    private final String address;
    private final String pincode;
    private final Double price;
    private final String ownerName;
    private final Double sqrtFeet;
    private final List<String> amenities;
    private final String propertyType;
    private final String bhkInfo;
    private final String description;

    // Updated constructor
    public PropertyState(String propertyDetails, UniqueIdentifier ownerId, AbstractParty host, UniqueIdentifier linearId,
                         String address, String pincode, Double price, String ownerName, Double sqrtFeet, List<String> amenities,
                         String propertyType, String bhkInfo, String description) {
        this.propertyDetails = propertyDetails;
        this.ownerId = ownerId;
        this.host = host;
        this.linearId = linearId;
        this.address = address;
        this.pincode = pincode;
        this.price = price;
        this.ownerName = ownerName;
        this.sqrtFeet = sqrtFeet;
        this.amenities = amenities;
        this.propertyType = propertyType;
        this.bhkInfo = bhkInfo;
        this.description = description;
    }

    // Getters for the new fields
    public String getAddress() { return address; }
    public String getPincode() { return pincode; }
    public Double getPrice() { return price; }
    public String getOwnerName() { return ownerName; }
    public Double getSqrtFeet() { return sqrtFeet; }
    public List<String> getAmenities() { return amenities; }
    public String getPropertyType() { return propertyType; }
    public String getBhkInfo() { return bhkInfo; }
    public String getDescription() { return description; }

    // Existing getters...
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
