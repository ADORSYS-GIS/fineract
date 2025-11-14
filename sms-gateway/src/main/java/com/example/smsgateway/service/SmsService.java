package com.example.smsgateway.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.sender.number}")
    private String senderNumber;

    public void sendSms(String to, String messageBody) {
        Twilio.init(accountSid, authToken);
        String recipientNumber = to.startsWith("+") ? to : "+" + to;

        Message message = Message.creator(
                new com.twilio.type.PhoneNumber(recipientNumber),
                new com.twilio.type.PhoneNumber(senderNumber),
                messageBody).create();
        System.out.println("Sent message with SID: " + message.getSid());
    }
}
