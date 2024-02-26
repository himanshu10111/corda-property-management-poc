package com.riequation.property.config;

import java.util.Random;

public class OTPGenerator {

    public static String generateOTP() {
        String numbers = "0123456789";

        Random random = new Random();

        StringBuilder otp = new StringBuilder();

        for(int i = 0; i < 6; i++) {
            int index = random.nextInt(numbers.length());
            otp.append(numbers.charAt(index));
        }

        return otp.toString();
    }
}

