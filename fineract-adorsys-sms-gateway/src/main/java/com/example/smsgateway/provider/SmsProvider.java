package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;

public interface SmsProvider {
    String name();

    SmsSendResult send(SmsMessage message);
}
