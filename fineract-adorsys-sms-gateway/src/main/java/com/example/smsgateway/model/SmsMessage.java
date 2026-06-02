package com.example.smsgateway.model;

import java.util.Map;

public record SmsMessage(
        String to,
        String body,
        MessageType type,
        String provider,
        Map<String, String> metadata
) {
}
