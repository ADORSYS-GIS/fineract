package com.example.smsgateway.model;

public record OtpValidateRequest(
        String requestId,
        String phoneNumber,
        String userId,
        String sessionId,
        String purpose,
        String otp
) {
}
