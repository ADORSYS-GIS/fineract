package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockSmsProvider.class);
    private final boolean enabled;

    public MockSmsProvider(@Value("${sms.mock.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        if (!enabled) {
            return SmsSendResult.failure(name(), "PROVIDER_NOT_ENABLED");
        }
        logger.info("Mock SMS accepted for type {}", message.type());
        return SmsSendResult.success(name(), "mock-message");
    }
}
