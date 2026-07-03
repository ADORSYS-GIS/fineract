package com.example.smsgateway.service;

import com.example.smsgateway.model.MessageType;
import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import com.example.smsgateway.provider.SmsProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(
        SmsService.class
    );
    private static final Pattern E164_PATTERN = Pattern.compile(
        "^\\+[1-9]\\d{7,14}$"
    );

    private final Map<String, SmsProvider> providers;
    private final String primaryProvider;
    private final List<String> fallbackProviders;
    private final MeterRegistry meterRegistry;

    public SmsService(
        List<SmsProvider> providers,
        @Value("${sms.provider.primary:twilio}") String primaryProvider,
        @Value("${sms.provider.fallback:}") String fallbackProvider,
        MeterRegistry meterRegistry
    ) {
        this.providers = providers
            .stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    SmsProvider::name,
                    provider -> provider
                )
            );
        this.primaryProvider = primaryProvider;
        this.fallbackProviders = StringUtils.hasText(fallbackProvidersRaw)
                ? java.util.Arrays.stream(fallbackProvidersRaw.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList()
                : java.util.Collections.emptyList();
        this.meterRegistry = meterRegistry;
    }

    /**
     * Convenience for Fineract-event SMS (deposit/withdrawal alerts). Tags the
     * message {@link MessageType#FINERACT_EVENT} so metrics/logs distinguish
     * Fineract webhook traffic from BFF-driven transactional SMS.
     */
    public void sendSms(String to, String messageBody) {
        sendSms(to, messageBody, MessageType.FINERACT_EVENT);
    }

    /**
     * Send a generic SMS tagged with an explicit message type so metrics and logs
     * separate traffic classes (e.g. BFF P2P claim links use {@link MessageType#TRANSACTIONAL},
     * not {@link MessageType#FINERACT_EVENT}). Throws {@link SmsDeliveryException}
     * on delivery failure so the BFF maps it to 502 rather than retry-storming 429.
     */
    public void sendSms(
        String to,
        String messageBody,
        MessageType messageType
    ) {
        SmsSendResult result = send(
            new SmsMessage(
                normalizePhoneNumber(to),
                sanitizeMessage(messageBody),
                messageType,
                null,
                Map.of()
            )
        );
        if (!result.success()) {
            throw new SmsDeliveryException(
                "SMS delivery failed",
                result.provider(),
                result.errorCode()
            );
        }
    }

    public SmsSendResult send(SmsMessage message) {
        String requestedProvider = StringUtils.hasText(message.provider())
            ? message.provider()
            : primaryProvider;
        SmsSendResult result = sendWithProvider(requestedProvider, message);
        if (
            !result.success() &&
            StringUtils.hasText(fallbackProvider) &&
            !fallbackProvider.equals(requestedProvider)
        ) {
            logger.warn(
                "SMS provider {} failed for message type {}, attempting fallback",
                requestedProvider,
                message.type()
            );
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
        return messageBody
            .replaceAll("[\\u0000-\\u001F&&[^\\n\\r\\t]]", "")
            .trim();
    }

    private SmsSendResult sendWithProvider(
        String providerName,
        SmsMessage message
    ) {
        Optional<SmsProvider> provider = Optional.ofNullable(
            providers.get(providerName)
        );
        if (provider.isEmpty()) {
            meterRegistry
                .counter(
                    "sms_send_total",
                    "provider",
                    providerName,
                    "status",
                    "failure"
                )
                .increment();
            return SmsSendResult.failure(providerName, "UNKNOWN_PROVIDER");
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            SmsSendResult result = provider.get().send(message);
            meterRegistry
                .counter(
                    "sms_send_total",
                    "provider",
                    providerName,
                    "status",
                    result.success() ? "success" : "failure"
                )
                .increment();
            return result;
        } catch (RuntimeException ex) {
            logger.warn(
                "SMS provider {} failed for message type {}",
                providerName,
                message.type()
            );
            meterRegistry
                .counter(
                    "sms_send_total",
                    "provider",
                    providerName,
                    "status",
                    "failure"
                )
                .increment();
            return SmsSendResult.failure(providerName, "PROVIDER_ERROR");
        } finally {
            sample.stop(
                meterRegistry.timer(
                    "sms_send_latency",
                    "provider",
                    providerName
                )
            );
        }
    }
}
