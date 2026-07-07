package com.example.smsgateway.service;

/**
 * Raised when an SMS cannot be delivered by any configured/fallback provider.
 *
 * <p>Distinct from rate-limiting (which keeps {@code IllegalStateException} → 429):
 * downstream delivery failures, unknown providers, and exhausted retries map to
 * {@code 502 Bad Gateway} via {@code ApiExceptionHandler}, so a BFF doing standard
 * backoff-and-retry on 429 does not retry-storm a permanently broken config.
 */
public class SmsDeliveryException extends RuntimeException {

    private final String provider;
    private final String errorCode;

    public SmsDeliveryException(String message, String provider, String errorCode) {
        super(message);
        this.provider = provider;
        this.errorCode = errorCode;
    }

    public String getProvider() {
        return provider;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
