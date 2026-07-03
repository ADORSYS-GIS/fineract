package com.example.smsgateway.controller;

import com.example.smsgateway.service.SmsDeliveryException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(
        ApiExceptionHandler.class
    );

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
        IllegalArgumentException ex
    ) {
        logger.warn("Rejected invalid request");
        return ResponseEntity.badRequest().body(
            Map.of("error", "Invalid request")
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleProcessingError(
        IllegalStateException ex
    ) {
        logger.warn("Unable to process request");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
            Map.of("error", "Unable to process request")
        );
    }

    /**
     * Downstream SMS delivery failure (provider exhausted, unknown provider, auth
     * failure). 502, not 429, so a BFF doing backoff-and-retry on 429 does not
     * retry-storm a permanently broken provider config.
     */
    @ExceptionHandler(SmsDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleDeliveryFailure(
        SmsDeliveryException ex
    ) {
        logger.warn(
            "SMS delivery failed (provider={}, errorCode={})",
            ex.getProvider(),
            ex.getErrorCode()
        );
        Map<String, String> body = new HashMap<>();
        body.put("error", "SMS delivery failed");
        body.put(
            "provider",
            ex.getProvider() != null ? ex.getProvider() : "unknown"
        );
        body.put(
            "errorCode",
            ex.getErrorCode() != null ? ex.getErrorCode() : "unknown"
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedError(
        Exception ex
    ) {
        logger.error("Unexpected API error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            Map.of("error", "Unable to process request")
        );
    }
}
