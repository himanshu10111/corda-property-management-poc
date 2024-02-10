package com.riequation.property.webserver.Dto;

public class AgentPasswordRequest {
    private String id; // This is assumed to be the unique identifier for the agent
    private String newPassword;
    private String confirmPassword;

    // Constructor
    public AgentPasswordRequest() {
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
