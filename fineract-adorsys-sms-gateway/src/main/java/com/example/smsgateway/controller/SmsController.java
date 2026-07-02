package com.example.smsgateway.controller;

import com.example.smsgateway.model.FineractHookPayload;
import com.example.smsgateway.model.OtpGenerateRequest;
import com.example.smsgateway.model.OtpGenerateResponse;
import com.example.smsgateway.model.OtpValidateRequest;
import com.example.smsgateway.model.OtpValidateResponse;
import com.example.smsgateway.service.MessageService;
import com.example.smsgateway.service.OtpService;
import com.example.smsgateway.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SmsController {

    private static final Logger logger = LoggerFactory.getLogger(SmsController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private SmsService smsService;

    @PostMapping("/sms/")
    public void receiveSmsRequest(@RequestBody String rawPayload) {
        logger.info("Received Fineract SMS webhook");

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            FineractHookPayload payload = objectMapper.readValue(rawPayload, FineractHookPayload.class);
            logger.info("Successfully deserialized Fineract SMS webhook for action {}", payload.getActionName());
            messageService.createAndSendMessage(payload);
        } catch (Exception e) {
            logger.error("Error deserializing payload", e);
        }
    }

    @PostMapping("/api/v1/otp/send")
    public ResponseEntity<OtpGenerateResponse> generateAndSendOtp(@RequestBody OtpGenerateRequest request) {
        OtpGenerateResponse response = otpService.generateAndSend(request);
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/api/v1/otp/validate")
    public ResponseEntity<OtpValidateResponse> validateOtp(@RequestBody OtpValidateRequest request) {
        return ResponseEntity.ok(otpService.validate(request));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, Object>> sendOtpCompatibility(@RequestBody Map<String, String> request) {
        OtpGenerateResponse response = otpService.generateAndSend(new OtpGenerateRequest(
                request.get("phone"),
                request.get("user_id"),
                null,
                request.get("context"),
                request.get("provider")
        ));
        return ResponseEntity.accepted().body(Map.of(
                "request_id", response.requestId(),
                "expires_in", response.expiresInSeconds(),
                "status", response.status()
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtpCompatibility(@RequestBody Map<String, String> request) {
        OtpValidateResponse response = otpService.validate(new OtpValidateRequest(
                request.get("request_id"),
                request.get("phone"),
                request.get("user_id"),
                null,
                request.get("context"),
                request.get("otp")
        ));
        return ResponseEntity.ok(Map.of(
                "verified", response.valid(),
                "status", response.status()
        ));
    }

    /**
     * Generic SMS send endpoint for transactional messages that are not OTP-gated.
     * Used by the BFF to send P2P viral-loop claim SMS to unregistered recipients
     * (money waiting in escrow until the recipient installs WeBank and verifies
     * their phone at KYC1).
     *
     * The BFF authenticates via the X-KYC-Api-Key header, the same key used for
     * the /otp/* endpoints. Validation and provider failure are surfaced through
     * the global ApiExceptionHandler (400 for invalid input, 429 for delivery
     * failure).
     */
    @PostMapping("/sms/send")
    public ResponseEntity<Void> sendGenericSms(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String message = request.get("message");
        logger.info("Sending generic SMS to {}", phone);
        smsService.sendSms(phone, message);
        return ResponseEntity.accepted().build();
    }
}
