package com.example.smsgateway.service;

import com.example.smsgateway.model.MessageType;
import com.example.smsgateway.model.OtpGenerateRequest;
import com.example.smsgateway.model.OtpGenerateResponse;
import com.example.smsgateway.model.OtpValidateRequest;
import com.example.smsgateway.model.OtpValidateResponse;
import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SmsService smsService;
    private final MeterRegistry meterRegistry;
    private final int length;
    private final int ttlSeconds;
    private final int maxSendAttempts;
    private final int sendWindowSeconds;
    private final int maxVerifyAttempts;
    private final int verifyWindowSeconds;
    private final String issuer;
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();
    private final Map<String, String> principalIndex = new ConcurrentHashMap<>();
    private final Map<String, RateLimitRecord> sendRateLimits = new ConcurrentHashMap<>();
    private final Map<String, RateLimitRecord> verifyRateLimits = new ConcurrentHashMap<>();

    public OtpService(
            SmsService smsService,
            MeterRegistry meterRegistry,
            @Value("${otp.length:6}") int length,
            @Value("${otp.ttl-seconds:300}") int ttlSeconds,
            @Value("${otp.max-send-attempts:3}") int maxSendAttempts,
            @Value("${otp.send-window-seconds:300}") int sendWindowSeconds,
            @Value("${otp.max-verify-attempts:5}") int maxVerifyAttempts,
            @Value("${otp.verify-window-seconds:300}") int verifyWindowSeconds,
            @Value("${otp.issuer:Webank}") String issuer) {
        this.smsService = smsService;
        this.meterRegistry = meterRegistry;
        this.length = length;
        this.ttlSeconds = ttlSeconds;
        this.maxSendAttempts = maxSendAttempts;
        this.sendWindowSeconds = sendWindowSeconds;
        this.maxVerifyAttempts = maxVerifyAttempts;
        this.verifyWindowSeconds = verifyWindowSeconds;
        this.issuer = issuer;
    }

    public OtpGenerateResponse generateAndSend(OtpGenerateRequest request) {
        String phoneNumber = smsService.normalizePhoneNumber(request.phoneNumber());
        String purpose = sanitizePurpose(request.purpose());
        String principal = principal(request.userId(), request.sessionId(), phoneNumber, purpose);
        if (!allow(sendRateLimits, principal, maxSendAttempts, sendWindowSeconds)) {
            logger.warn("OTP send rate limit exceeded for purpose {}", purpose);
            meterRegistry.counter("otp_events_total", "event", "rate_limited").increment();
            throw new IllegalStateException("Unable to process OTP request");
        }

        String otp = generateOtp();
        String requestId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        otpStore.put(requestId, new OtpRecord(hashOtp(otp), phoneNumber, safe(request.userId()), safe(request.sessionId()), purpose, expiresAt, false));
        principalIndex.put(principal, requestId);

        String body = "Your " + issuer + " verification code is " + otp;
        SmsSendResult result = smsService.send(new SmsMessage(phoneNumber, body, MessageType.OTP, request.provider(), Map.of("purpose", purpose)));
        if (!result.success()) {
            otpStore.remove(requestId);
            principalIndex.remove(principal);
            meterRegistry.counter("otp_events_total", "event", "send_failed").increment();
            throw new IllegalStateException("Unable to process OTP request");
        }

        logger.info("OTP generated and sent for purpose {}", purpose);
        meterRegistry.counter("otp_events_total", "event", "sent").increment();
        return new OtpGenerateResponse(requestId, ttlSeconds, "SENT");
    }

    public OtpValidateResponse validate(OtpValidateRequest request) {
        String phoneNumber = smsService.normalizePhoneNumber(request.phoneNumber());
        String purpose = sanitizePurpose(request.purpose());
        String principal = principal(request.userId(), request.sessionId(), phoneNumber, purpose);
        String requestId = StringUtils.hasText(request.requestId()) ? safe(request.requestId()) : principalIndex.get(principal);
        if (!allow(verifyRateLimits, principal, maxVerifyAttempts, verifyWindowSeconds)) {
            logger.warn("OTP verify rate limit exceeded for purpose {}", purpose);
            meterRegistry.counter("otp_events_total", "event", "verify_rate_limited").increment();
            return new OtpValidateResponse(false, "INVALID");
        }

        OtpRecord record = otpStore.get(requestId);
        if (record == null || record.used() || record.expiresAt().isBefore(Instant.now())) {
            otpStore.remove(requestId);
            principalIndex.remove(principal);
            meterRegistry.counter("otp_events_total", "event", "invalid").increment();
            return new OtpValidateResponse(false, "INVALID");
        }

        boolean metadataMatches = record.phoneNumber().equals(phoneNumber)
                && record.userId().equals(safe(request.userId()))
                && record.sessionId().equals(safe(request.sessionId()))
                && record.purpose().equals(purpose);
        boolean otpMatches = MessageDigest.isEqual(record.otpHash().getBytes(StandardCharsets.UTF_8), hashOtp(safe(request.otp())).getBytes(StandardCharsets.UTF_8));
        if (!metadataMatches || !otpMatches) {
            meterRegistry.counter("otp_events_total", "event", "invalid").increment();
            return new OtpValidateResponse(false, "INVALID");
        }

        otpStore.put(requestId, record.markUsed());
        otpStore.remove(requestId);
        principalIndex.remove(principal);
        logger.info("OTP validated for purpose {}", purpose);
        meterRegistry.counter("otp_events_total", "event", "validated").increment();
        return new OtpValidateResponse(true, "VALID");
    }

    @Scheduled(fixedDelayString = "${otp.cleanup-interval-ms:60000}")
    public void cleanupExpiredOtps() {
        Instant now = Instant.now();
        otpStore.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now) || entry.getValue().used());
        principalIndex.entrySet().removeIf(entry -> !otpStore.containsKey(entry.getValue()));
        sendRateLimits.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        verifyRateLimits.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, length);
        int min = (int) Math.pow(10, length - 1);
        return String.valueOf(SECURE_RANDOM.nextInt(bound - min) + min);
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(otp.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("OTP hashing unavailable", e);
        }
    }

    private boolean allow(Map<String, RateLimitRecord> store, String key, int maxAttempts, int windowSeconds) {
        Instant now = Instant.now();
        RateLimitRecord current = store.get(key);
        if (current == null || current.expiresAt().isBefore(now)) {
            store.put(key, new RateLimitRecord(1, now.plusSeconds(windowSeconds)));
            return true;
        }
        if (current.count() >= maxAttempts) {
            return false;
        }
        store.put(key, new RateLimitRecord(current.count() + 1, current.expiresAt()));
        return true;
    }

    private String sanitizePurpose(String purpose) {
        String value = StringUtils.hasText(purpose) ? purpose.trim() : "default";
        if (!value.matches("^[A-Za-z0-9_-]{1,64}$")) {
            throw new IllegalArgumentException("Invalid purpose");
        }
        return value;
    }

    private String principal(String userId, String sessionId, String phoneNumber, String purpose) {
        return safe(userId) + ":" + safe(sessionId) + ":" + phoneNumber + ":" + purpose;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record OtpRecord(String otpHash, String phoneNumber, String userId, String sessionId, String purpose, Instant expiresAt, boolean used) {
        OtpRecord markUsed() {
            return new OtpRecord(otpHash, phoneNumber, userId, sessionId, purpose, expiresAt, true);
        }
    }

    private record RateLimitRecord(int count, Instant expiresAt) {
    }
}
