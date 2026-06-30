package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class WhatsappSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappSmsProvider.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String deviceId;

    public WhatsappSmsProvider(
            RestTemplate restTemplate,
            @Value("${sms.whatsapp.base-url:}") String baseUrl,
            @Value("${sms.whatsapp.device-id:}") String deviceId) {
        this.restTemplate = restTemplate;
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
                String url = baseUrl.trimEnd('/') + "/send/message";
                if (StringUtils.hasText(deviceId)) {
                    url += "?device_id=" + deviceId;
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
                    logger.info("WhatsApp message sent to {}", message.to());
                    return SmsSendResult.success(name(), null);
                } else {
                    logger.warn("WhatsApp provider returned {} for {}", response.getStatusCode(), message.to());
                    return SmsSendResult.failure(name(), "PROVIDER_REJECTED");
                }

            } catch (Exception ex) {
                logger.error("Error sending WhatsApp message to {}", message.to(), ex);
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
}
