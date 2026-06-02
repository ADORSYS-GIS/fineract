package com.example.smsgateway.model;

public record OtpGenerateRequest(
        String phoneNumber,
        String userId,
        String sessionId,
        String purpose,
        String provider
) {
}
