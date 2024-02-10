package com.riequation.property.webserver.Dto;

public class ChangePasswordRequest {

    private String ownerId;
    private String newPassword;
    private String confirmPassword;

    public String getOwnerId() {
        return ownerId;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }
}
