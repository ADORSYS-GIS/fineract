package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TwilioSmsProvider implements SmsProvider {

    private final String accountSid;
    private final String authToken;
    private final String senderNumber;

    public TwilioSmsProvider(
            @Value("${twilio.account.sid:}") String accountSid,
            @Value("${twilio.auth.token:}") String authToken,
            @Value("${twilio.sender.number:}") String senderNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.senderNumber = senderNumber;
    }

    @Override
    public String name() {
        return "twilio";
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken) || !StringUtils.hasText(senderNumber)) {
            return SmsSendResult.failure(name(), "PROVIDER_NOT_CONFIGURED");
        }

        Twilio.init(accountSid, authToken);
        Message twilioMessage = Message.creator(
                new PhoneNumber(message.to()),
                new PhoneNumber(senderNumber),
                message.body()).create();
        return SmsSendResult.success(name(), twilioMessage.getSid());
    }
}
