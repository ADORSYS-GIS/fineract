package com.example.smsgateway.model;

/**
 * Request body for {@code POST /sms/send} — generic (non-OTP) transactional SMS.
 *
 * @param phone   recipient phone number in E.164 format (e.g. {@code +237670000000})
 * @param message message body (max 1600 chars; control chars stripped)
 */
public record SmsSendRequest(String phone, String message) {
}
