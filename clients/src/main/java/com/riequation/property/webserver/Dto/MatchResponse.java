package com.riequation.property.webserver.Dto;

public class MatchResponse {
    public String ownerId;
    public String message;

    public MatchResponse(String ownerId, String message) {
        this.ownerId = ownerId;
        this.message = message;
    }
}

