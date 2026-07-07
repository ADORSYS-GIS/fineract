package com.example.smsgateway.model;

public enum MessageType {
    OTP,
    ALERT,
    MARKETING,
    FINERACT_EVENT,
    /** BFF-driven transactional SMS (e.g. P2P viral-loop claim links), not OTP-gated. */
    TRANSACTIONAL,
}
