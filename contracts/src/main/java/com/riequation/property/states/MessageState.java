package com.riequation.property.states;

import com.riequation.property.contracts.MessageContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.ConstructorForDeserialization;
import java.util.List;

@BelongsToContract(MessageContract.class)
public class MessageState implements ContractState {

    private final String messageId; // System-generated unique ID for the message
    private final String refId; // New field to group messages
    private final String messageTemplate;
    private final String placeholder1;
    private final String placeholder2;
    private final List<AbstractParty> participants;

    @ConstructorForDeserialization
    public MessageState(String messageId, String refId, String messageTemplate, String placeholder1, String placeholder2, List<AbstractParty> participants) {
        this.messageId = messageId;
        this.refId = refId;
        this.messageTemplate = messageTemplate;
        this.placeholder1 = placeholder1;
        this.placeholder2 = placeholder2;
        this.participants = participants;
    }

    // Getters
    public String getMessageId() {
        return messageId;
    }

    public String getRefId() {
        return refId;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public String getPlaceholder1() {
        return placeholder1;
    }

    public String getPlaceholder2() {
        return placeholder2;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}
