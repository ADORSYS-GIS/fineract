package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class AvlytextSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(AvlytextSmsProvider.class);
    private static final String AVLYTEXT_SMS_ENDPOINT = "/v1/sms";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String senderId;

    public AvlytextSmsProvider(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${avlytext.api.key:}") String apiKey,
            @Value("${avlytext.base.url:}") String baseUrl,
            @Value("${avlytext.sender.id:}") String senderId) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.senderId = senderId;
    }

    @Override
    public String name() {
        return "avlytext";
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        if (!isConfigured()) {
            return SmsSendResult.failure(name(), "PROVIDER_NOT_CONFIGURED");
        }

        return RetryHelper.executeWithRetry(() -> {
            try {
                // Build request payload
                AvlytextSmsRequest request = new AvlytextSmsRequest(
                        senderId,
                        message.to(),
                        message.body()
                );

                // Build URL with API key
                String url = baseUrl + AVLYTEXT_SMS_ENDPOINT + "?api_key=" + apiKey;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<AvlytextSmsRequest> entity = new HttpEntity<>(request, headers);
                ResponseEntity<AvlytextSmsResponse> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, AvlytextSmsResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    if (response.getBody().success()) {
                        return SmsSendResult.success(name(), response.getBody().messageId());
                    } else {
                        return SmsSendResult.failure(name(), response.getBody().error() != null ? 
                                response.getBody().error().code() : "API_ERROR");
                    }
                } else {
                    return handleErrorResponse(response.getStatusCode());
                }

            } catch (Exception ex) {
                logger.error("Error sending SMS via Avlytext", ex);
                return SmsSendResult.failure(name(), mapExceptionToErrorCode(ex));
            }
        }, name());
    }

    private boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(baseUrl) && StringUtils.hasText(senderId);
    }

    private SmsSendResult handleErrorResponse(HttpStatusCode statusCode) {
        if (statusCode.is5xxServerError()) {
            return SmsSendResult.failure(name(), "TRANSIENT_ERROR");
        } else {
            return SmsSendResult.failure(name(), "PERMANENT_ERROR");
        }
    }

    private String mapExceptionToErrorCode(Exception ex) {
        if (ex instanceof IllegalArgumentException) {
            return "INVALID_INPUT";
        } else {
            return "PROVIDER_ERROR";
        }
    }

    // DTOs for Avlytext API - public for test access
    public record AvlytextSmsRequest(
            String sender,
            String recipient,
            @JsonProperty("text") String message
    ) {}

    public record AvlytextSmsResponse(
            boolean success,
            String messageId,
            AvlytextError error
    ) {}

    public record AvlytextError(
            String code,
            String message
    ) {}
}
