package com.example.smsgateway.model;

public record OtpValidateResponse(
        boolean valid,
        String status
) {
}
