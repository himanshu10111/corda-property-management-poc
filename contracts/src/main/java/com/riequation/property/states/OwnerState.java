package com.riequation.property.states;


import com.riequation.property.contracts.OwnerContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.contracts.UniqueIdentifier;



import java.util.Arrays;
import java.util.List;

@BelongsToContract(OwnerContract.class)
public class OwnerState implements LinearState {
    private final String name;
    private final String email;
    private final String password; // Should be a hashed password for security
    private final String mobileNumber;
    private final String address;
    private final AbstractParty host; // The node hosting this owner account
    private final UniqueIdentifier linearId; // Unique identifier for the state

    public OwnerState(String name, String email, String password, String mobileNumber, String address, AbstractParty host, UniqueIdentifier linearId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.mobileNumber = mobileNumber;
        this.address = address;
        this.host = host;
        this.linearId = linearId;
    }

    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getMobileNumber() { return mobileNumber; }
    public String getAddress() { return address; }
    public AbstractParty getHost() { return host; }
    @Override
    public UniqueIdentifier getLinearId() { return linearId; }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(host);
    }
}
