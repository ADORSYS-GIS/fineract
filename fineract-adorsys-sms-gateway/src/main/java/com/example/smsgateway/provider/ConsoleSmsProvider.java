package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleSmsProvider.class);

    @Override
    public String name() {
        return "console";
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        logger.info("CONSOLE_SMS type={} to={} body={}", message.type(), message.to(), message.body());
        return SmsSendResult.success(name(), "console-message");
    }
}
