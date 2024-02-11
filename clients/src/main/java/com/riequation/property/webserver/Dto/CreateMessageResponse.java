package com.riequation.property.webserver.Dto;



public class CreateMessageResponse {
    private final String message;
    private final String messageId;

    public CreateMessageResponse(String message, String messageId) {
        this.message = message;
        this.messageId = messageId;
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public String getMessageId() {
        return messageId;
    }
}
