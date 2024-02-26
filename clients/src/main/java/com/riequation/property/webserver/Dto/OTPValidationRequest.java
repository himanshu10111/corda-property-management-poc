package com.riequation.property.webserver.Dto;

public class OTPValidationRequest {

    private String email;
    private String otp;

    // Default constructor
    public OTPValidationRequest() {
    }

    // Constructor with fields
    public OTPValidationRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
