package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsSendResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryHelperTest {

    @Test
    void testSuccessOnFirstAttempt() {
        SmsSendResult result = RetryHelper.executeWithRetry(() -> 
            SmsSendResult.success("test", "message-id"), "test"
        );
        
        assertTrue(result.success());
        assertEquals("test", result.provider());
        assertEquals("message-id", result.messageId());
    }

    @Test
    void testSuccessAfterRetries() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        SmsSendResult result = RetryHelper.executeWithRetry(() -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                return SmsSendResult.failure("test", "TRANSIENT_ERROR");
            }
            return SmsSendResult.success("test", "message-id");
        }, "test");
        
        assertTrue(result.success());
        assertEquals("test", result.provider());
        assertEquals("message-id", result.messageId());
        assertEquals(3, attemptCount.get());
    }

    @Test
    void testPermanentErrorNoRetry() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        SmsSendResult result = RetryHelper.executeWithRetry(() -> {
            attemptCount.incrementAndGet();
            return SmsSendResult.failure("test", "PERMANENT_ERROR");
        }, "test");
        
        assertFalse(result.success());
        assertEquals("test", result.provider());
        assertEquals("PERMANENT_ERROR", result.errorCode());
        assertEquals(1, attemptCount.get());
    }

    @Test
    void testMaxRetriesExceeded() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        SmsSendResult result = RetryHelper.executeWithRetry(() -> 
            SmsSendResult.failure("test", "TRANSIENT_ERROR"), "test"
        );
        
        assertFalse(result.success());
        assertEquals("test", result.provider());
        assertEquals("MAX_RETRIES_EXCEEDED", result.errorCode());
        assertEquals(5, attemptCount.get()); // Initial attempt + 4 retries
    }

    @Test
    void testExceptionHandling() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        SmsSendResult result = RetryHelper.executeWithRetry(() -> {
            attemptCount.incrementAndGet();
            if (attemptCount.get() < 3) {
                throw new RuntimeException("Transient exception");
            }
            return SmsSendResult.success("test", "message-id");
        }, "test");
        
        assertTrue(result.success());
        assertEquals("test", result.provider());
        assertEquals("message-id", result.messageId());
        assertEquals(3, attemptCount.get());
    }

    @Test
    void testInterruptedException() {
        SmsSendResult result = RetryHelper.executeWithRetry(() -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted");
        }, "test");
        
        assertFalse(result.success());
        assertEquals("test", result.provider());
        assertEquals("INTERRUPTED", result.errorCode());
        assertTrue(Thread.currentThread().isInterrupted());
    }
}
