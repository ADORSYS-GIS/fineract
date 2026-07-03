package com.example.smsgateway.controller;

import com.example.smsgateway.model.FineractHookPayload;
import com.example.smsgateway.model.MessageType;
import com.example.smsgateway.model.OtpGenerateRequest;
import com.example.smsgateway.model.OtpGenerateResponse;
import com.example.smsgateway.model.OtpValidateRequest;
import com.example.smsgateway.model.OtpValidateResponse;
import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendRequest;
import com.example.smsgateway.model.SmsSendResult;
import com.example.smsgateway.service.MessageService;
import com.example.smsgateway.service.OtpService;
import com.example.smsgateway.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmsController {

    private static final Logger logger = LoggerFactory.getLogger(
        SmsController.class
    );

    @Autowired
    private MessageService messageService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private SmsService smsService;

    @Autowired
    @Qualifier("smsSendExecutor")
    private ThreadPoolTaskExecutor smsSendExecutor;

    @PostMapping("/sms/")
    public void receiveSmsRequest(@RequestBody String rawPayload) {
        logger.info("Received Fineract SMS webhook");

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            FineractHookPayload payload = objectMapper.readValue(
                rawPayload,
                FineractHookPayload.class
            );
            logger.info(
                "Successfully deserialized Fineract SMS webhook for action {}",
                payload.getActionName()
            );
            messageService.createAndSendMessage(payload);
        } catch (Exception e) {
            logger.error("Error deserializing payload", e);
        }
    }

    @PostMapping("/api/v1/otp/send")
    public ResponseEntity<OtpGenerateResponse> generateAndSendOtp(
        @RequestBody OtpGenerateRequest request
    ) {
        OtpGenerateResponse response = otpService.generateAndSend(request);
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/api/v1/otp/validate")
    public ResponseEntity<OtpValidateResponse> validateOtp(
        @RequestBody OtpValidateRequest request
    ) {
        return ResponseEntity.ok(otpService.validate(request));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, Object>> sendOtpCompatibility(
        @RequestBody Map<String, String> request
    ) {
        OtpGenerateResponse response = otpService.generateAndSend(
            new OtpGenerateRequest(
                request.get("phone"),
                request.get("user_id"),
                null,
                request.get("context"),
                request.get("provider")
            )
        );
        return ResponseEntity.accepted().body(
            Map.of(
                "request_id",
                response.requestId(),
                "expires_in",
                response.expiresInSeconds(),
                "status",
                response.status()
            )
        );
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtpCompatibility(
        @RequestBody Map<String, String> request
    ) {
        OtpValidateResponse response = otpService.validate(
            new OtpValidateRequest(
                request.get("request_id"),
                request.get("phone"),
                request.get("user_id"),
                null,
                request.get("context"),
                request.get("otp")
            )
        );
        return ResponseEntity.ok(
            Map.of("verified", response.valid(), "status", response.status())
        );
    }

    /**
     * Generic SMS send endpoint for transactional messages that are not OTP-gated.
     * Used by the BFF to send P2P viral-loop claim SMS to unregistered recipients
     * (money waiting in escrow until the recipient installs WeBank and verifies
     * their phone at KYC1).
     *
     * <p>Authenticates via the {@code X-KYC-Api-Key} header, the same key used for
     * the {@code /otp/*} endpoints. Inputs are validated synchronously (400 on bad
     * input); the actual provider send is dispatched asynchronously so a 202
     * Accepted is returned immediately — provider retries (up to ~15s plus fallback)
     * run on a dedicated thread pool that does not starve Tomcat workers serving
     * the OTP and Fineract webhook endpoints. Delivery failure (async) is logged;
     * for synchronous callers that still want a delivery result, the typed record
     * surfaces 400 for invalid input, and 502 for delivery failure when called
     * synchronously elsewhere.
     *
     * <p>Messages are tagged {@link MessageType#TRANSACTIONAL} so metrics and logs
     * separate BFF P2P traffic from Fineract-event SMS.
     */
    @PostMapping("/sms/send")
    public ResponseEntity<Void> sendGenericSms(
        @RequestBody SmsSendRequest request
    ) {
        // Validate synchronously so a bad request still returns 400, not 202.
        String phone = smsService.normalizePhoneNumber(request.phone());
        String message = smsService.sanitizeMessage(request.message());
        logger.info("Queuing generic SMS send to {}", phone);

        SmsMessage smsMessage = new SmsMessage(
            phone,
            message,
            MessageType.TRANSACTIONAL,
            null,
            Map.of()
        );
        smsSendExecutor.submit(() -> {
            try {
                SmsSendResult result = smsService.send(smsMessage);
                if (!result.success()) {
                    logger.error(
                        "Async SMS send failed: to={}, provider={}, errorCode={}",
                        phone,
                        result.provider(),
                        result.errorCode()
                    );
                }
            } catch (Exception e) {
                logger.error(
                    "Async SMS send encountered an error: to={}",
                    phone,
                    e
                );
            }
        });
        return ResponseEntity.accepted().build();
    }
}
