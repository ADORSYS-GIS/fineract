package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RetryHelper {
    private static final Logger logger = LoggerFactory.getLogger(RetryHelper.class);
    private static final int MAX_RETRIES = 4;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second

    public static SmsSendResult executeWithRetry(Supplier<SmsSendResult> operation, String providerName) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempt <= MAX_RETRIES) {
            try {
                SmsSendResult result = operation.get();
                
                if (result.success()) {
                    return result;
                }

                // Check if error is retryable
                if (!isRetryableError(result.errorCode()) || attempt == MAX_RETRIES) {
                    return result;
                }

                logger.warn("Retryable error for provider {}: {} (attempt {}/{})", 
                        providerName, result.errorCode(), attempt + 1, MAX_RETRIES + 1);

            } catch (Exception ex) {
                if (attempt == MAX_RETRIES) {
                    logger.error("Max retries exceeded for provider {}", providerName, ex);
                    return SmsSendResult.failure(providerName, "MAX_RETRIES_EXCEEDED");
                }
                logger.warn("Transient exception for provider {} (attempt {}/{})", 
                        providerName, attempt + 1, MAX_RETRIES + 1, ex);
            }

            attempt++;
            if (attempt <= MAX_RETRIES) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return SmsSendResult.failure(providerName, "INTERRUPTED");
                }
                backoffMs *= 2; // Exponential backoff
            }
        }

        return SmsSendResult.failure(providerName, "MAX_RETRIES_EXCEEDED");
    }

    private static boolean isRetryableError(String errorCode) {
        if (errorCode == null) {
            return false;
        }
        return switch (errorCode) {
            case "TRANSIENT_ERROR", "RATE_LIMIT_EXCEEDED", "UNAUTHORIZED_TOKEN_CLEARED", 
                 "PROVIDER_ERROR", "MAX_RETRIES_EXCEEDED" -> true;
            default -> false;
        };
    }
}
