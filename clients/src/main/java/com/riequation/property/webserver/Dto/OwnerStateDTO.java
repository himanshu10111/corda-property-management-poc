package com.riequation.property.webserver.Dto;

import net.corda.core.identity.AbstractParty;
import net.corda.core.contracts.UniqueIdentifier;

public class OwnerStateDTO {
    private final String name;
    private final String email;
    private final String mobileNumber;
    private final String address;
    private final AbstractParty host;
    private final UniqueIdentifier linearId;

    public OwnerStateDTO(String name, String email, String mobileNumber, String address, AbstractParty host, UniqueIdentifier linearId) {
        this.name = name;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.address = address;
        this.host = host;
        this.linearId = linearId;
    }

    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getMobileNumber() { return mobileNumber; }
    public String getAddress() { return address; }
    public AbstractParty getHost() { return host; }
    public UniqueIdentifier getLinearId() { return linearId; }
}