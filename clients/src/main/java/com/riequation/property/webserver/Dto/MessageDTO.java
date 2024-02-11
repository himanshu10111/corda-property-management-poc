package com.riequation.property.webserver.Dto;

public class MessageDTO {
    private String messageId;
    private String refId;
    private String messageTemplate;
    private String placeholder1;
    private String placeholder2;

    // Constructors
    public MessageDTO() {
    }

    public MessageDTO(String messageId, String refId, String messageTemplate, String placeholder1, String placeholder2) {
        this.messageId = messageId;
        this.refId = refId;
        this.messageTemplate = messageTemplate;
        this.placeholder1 = placeholder1;
        this.placeholder2 = placeholder2;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String getPlaceholder1() {
        return placeholder1;
    }

    public void setPlaceholder1(String placeholder1) {
        this.placeholder1 = placeholder1;
    }

    public String getPlaceholder2() {
        return placeholder2;
    }

    public void setPlaceholder2(String placeholder2) {
        this.placeholder2 = placeholder2;
    }
}
