package com.example.smsgateway.model;

public record OtpGenerateResponse(
        String requestId,
        int expiresInSeconds,
        String status
) {
}
