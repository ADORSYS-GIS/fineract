package com.example.smsgateway.service;

import com.example.smsgateway.model.MessageType;
import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import com.example.smsgateway.provider.SmsProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final Map<String, SmsProvider> providers;
    private final String primaryProvider;
    private final String fallbackProvider;
    private final MeterRegistry meterRegistry;

    public SmsService(
            List<SmsProvider> providers,
            @Value("${sms.provider.primary:twilio}") String primaryProvider,
            @Value("${sms.provider.fallback:}") String fallbackProvider,
            MeterRegistry meterRegistry) {
        this.providers = providers.stream().collect(java.util.stream.Collectors.toMap(SmsProvider::name, provider -> provider));
        this.primaryProvider = primaryProvider;
        this.fallbackProvider = fallbackProvider;
        this.meterRegistry = meterRegistry;
    }

    public void sendSms(String to, String messageBody) {
        SmsSendResult result = send(new SmsMessage(normalizePhoneNumber(to), sanitizeMessage(messageBody), MessageType.FINERACT_EVENT, null, Map.of()));
        if (!result.success()) {
            throw new IllegalStateException("SMS delivery failed");
        }
    }

    public SmsSendResult send(SmsMessage message) {
        String requestedProvider = StringUtils.hasText(message.provider()) ? message.provider() : primaryProvider;
        SmsSendResult result = sendWithProvider(requestedProvider, message);
        if (!result.success() && StringUtils.hasText(fallbackProvider) && !fallbackProvider.equals(requestedProvider)) {
            logger.warn("SMS provider {} failed for message type {}, attempting fallback", requestedProvider, message.type());
            result = sendWithProvider(fallbackProvider, message);
        }
        return result;
    }

    public String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        String normalized = phoneNumber.replaceAll("[^0-9+]", "");
        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }
        if (!E164_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        return normalized;
    }

    public String sanitizeMessage(String messageBody) {
        if (!StringUtils.hasText(messageBody) || messageBody.length() > 1600) {
            throw new IllegalArgumentException("Invalid message body");
        }
        return messageBody.replaceAll("[\\u0000-\\u001F&&[^\\n\\r\\t]]", "").trim();
    }

    private SmsSendResult sendWithProvider(String providerName, SmsMessage message) {
        Optional<SmsProvider> provider = Optional.ofNullable(providers.get(providerName));
        if (provider.isEmpty()) {
            meterRegistry.counter("sms_send_total", "provider", providerName, "status", "failure").increment();
            return SmsSendResult.failure(providerName, "UNKNOWN_PROVIDER");
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            SmsSendResult result = provider.get().send(message);
            meterRegistry.counter("sms_send_total", "provider", providerName, "status", result.success() ? "success" : "failure").increment();
            return result;
        } catch (RuntimeException ex) {
            logger.warn("SMS provider {} failed for message type {}", providerName, message.type());
            meterRegistry.counter("sms_send_total", "provider", providerName, "status", "failure").increment();
            return SmsSendResult.failure(providerName, "PROVIDER_ERROR");
        } finally {
            sample.stop(meterRegistry.timer("sms_send_latency", "provider", providerName));
        }
    }
}
