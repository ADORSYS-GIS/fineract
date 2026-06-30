package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
public class WhatsappSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappSmsProvider.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String deviceId;

    public WhatsappSmsProvider(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${sms.whatsapp.base-url:}") String baseUrl,
            @Value("${sms.whatsapp.device-id:}") String deviceId) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.baseUrl = baseUrl;
        this.deviceId = deviceId;
    }

    @Override
    public String name() {
        return "whatsapp";
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        if (!isConfigured()) {
            return SmsSendResult.failure(name(), "PROVIDER_NOT_CONFIGURED");
        }

        return RetryHelper.executeWithRetry(() -> {
            try {
                String url = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + "/send/message";
                if (StringUtils.hasText(deviceId)) {
                    url += "?device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, String> payload = Map.of(
                        "phone", message.to(),
                        "message", message.body()
                );

                HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("WhatsApp message sent to {}", maskPhone(message.to()));
                    return SmsSendResult.success(name(), null);
                } else {
                    logger.warn("WhatsApp provider returned {} for {}", response.getStatusCode(), maskPhone(message.to()));
                    return SmsSendResult.failure(name(), "PROVIDER_REJECTED");
                }

            } catch (Exception ex) {
                logger.error("Error sending WhatsApp message to {}", maskPhone(message.to()), ex);
                return SmsSendResult.failure(name(), mapExceptionToErrorCode(ex));
            }
        }, name());
    }

    private boolean isConfigured() {
        return StringUtils.hasText(baseUrl);
    }

    private String mapExceptionToErrorCode(Exception ex) {
        if (ex instanceof IllegalArgumentException) {
            return "INVALID_INPUT";
        }
        return "PROVIDER_ERROR";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 2);
    }
}
