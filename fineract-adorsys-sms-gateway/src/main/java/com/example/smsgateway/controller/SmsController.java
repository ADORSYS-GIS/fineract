package com.example.smsgateway.controller;

import com.example.smsgateway.model.FineractHookPayload;
import com.example.smsgateway.model.MessageType;
import com.example.smsgateway.model.OtpGenerateRequest;
import com.example.smsgateway.model.OtpGenerateResponse;
import com.example.smsgateway.model.OtpValidateRequest;
import com.example.smsgateway.model.OtpValidateResponse;
import com.example.smsgateway.model.SmsSendRequest;
import com.example.smsgateway.service.MessageService;
import com.example.smsgateway.service.OtpService;
import com.example.smsgateway.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
        String purpose = resolvePurpose(request);
        OtpGenerateResponse response = otpService.generateAndSend(
            new OtpGenerateRequest(
                request.get("phone"),
                request.get("user_id"),
                null,
                purpose,
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
        String purpose = resolvePurpose(request);
        OtpValidateResponse response = otpService.validate(
            new OtpValidateRequest(
                request.get("request_id"),
                request.get("phone"),
                request.get("user_id"),
                null,
                purpose,
                request.get("otp")
            )
        );
        return ResponseEntity.ok(
            Map.of("verified", response.valid(), "status", response.status())
        );
    }

    private static String resolvePurpose(Map<String, String> request) {
        String purpose = request.get("purpose");
        return (purpose == null || purpose.isBlank()) ? request.get("context") : purpose;
    }

    /**
     * Generic SMS send endpoint for transactional messages that are not OTP-gated.
     * Used by the BFF to send P2P viral-loop claim SMS to unregistered recipients
     * (money waiting in escrow until the recipient installs WeBank and verifies
     * their phone at KYC1).
     *
     * <p>Authenticates via the {@code X-SMS-Gateway-Api-Key} header, the same key used for
     * the {@code /otp/*} endpoints. Inputs are validated synchronously (400 on bad
     * input) inside {@link SmsService#sendAsync}; the actual provider send is then
     * dispatched asynchronously so a 202 Accepted is returned immediately — provider
     * retries (up to ~15s plus the full fallback cascade) run on a dedicated
     * {@code smsSendExecutor} that does not starve Tomcat workers serving the OTP
     * and Fineract webhook endpoints. Async delivery failure is logged and surfaced
     * via {@code sms_send_total{status="failure"}} — it is never returned to the
     * caller, so /sms/send only ever returns 202 (accepted), 400 (bad input), or
     * 429 (overload when the executor pool+queue are saturated). A 502 delivery
     * failure can only occur for synchronous in-process callers of
     * {@link SmsService#sendSms(String, String, MessageType)} (e.g. the OTP/Fineract
     * event path), not for this HTTP endpoint.
     *
     * <p>Messages are tagged {@link MessageType#TRANSACTIONAL} so metrics and logs
     * separate BFF P2P traffic from Fineract-event SMS.
     */
    @PostMapping("/sms/send")
    public ResponseEntity<Void> sendGenericSms(
        @RequestBody SmsSendRequest request
    ) {
        // Validation runs synchronously inside sendAsync (IllegalArgumentException -> 400),
        // then the provider send is queued on the dedicated executor. Under backpressure
        // (pool+queue full, AbortPolicy) submit throws RejectedExecutionException which
        // ApiExceptionHandler maps to 429 — never blocks the Tomcat request thread.
        smsService.sendAsync(
            request.phone(),
            request.message(),
            MessageType.TRANSACTIONAL
        );
        return ResponseEntity.accepted().build();
    }
}
