package com.example.smsgateway.model;

public record SmsSendResult(
        boolean success,
        String provider,
        String messageId,
        String errorCode
) {
    public static SmsSendResult success(String provider, String messageId) {
        return new SmsSendResult(true, provider, messageId, null);
    }

    public static SmsSendResult failure(String provider, String errorCode) {
        return new SmsSendResult(false, provider, null, errorCode);
    }
}
