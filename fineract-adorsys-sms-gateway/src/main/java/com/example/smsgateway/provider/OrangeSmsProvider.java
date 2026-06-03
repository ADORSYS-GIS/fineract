package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class OrangeSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(OrangeSmsProvider.class);
    private static final String ORANGE_SMS_URL_TEMPLATE = "%s/outbound/%s/requests";
    private static final int MAX_SMS_PER_SECOND = 5;
    private static final long THROTTLE_INTERVAL_MS = 1000 / MAX_SMS_PER_SECOND; // 200ms
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 300; // 5 minutes

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final String smsBaseUrl;
    private final String senderAddress;
    private final String senderName;
    private final String countryCode;

    // Token cache
    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiry;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // Rate limiting
    private final Semaphore rateLimiter = new Semaphore(MAX_SMS_PER_SECOND);
    private volatile long lastRequestTime = 0;

    public OrangeSmsProvider(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${orange.client.id:}") String clientId,
            @Value("${orange.client.secret:}") String clientSecret,
            @Value("${orange.token.url:}") String tokenUrl,
            @Value("${orange.sms.base.url:}") String smsBaseUrl,
            @Value("${orange.sender.address:}") String senderAddress,
            @Value("${orange.sender.name:}") String senderName,
            @Value("${orange.country.code:237}") String countryCode) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.smsBaseUrl = smsBaseUrl;
        this.senderAddress = senderAddress;
        this.senderName = senderName;
        this.countryCode = countryCode;
    }

    @Override
    public String name() {
        return "orange";
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        if (!isConfigured()) {
            return SmsSendResult.failure(name(), "PROVIDER_NOT_CONFIGURED");
        }

        return RetryHelper.executeWithRetry(() -> {
            try {
                // Rate limiting
                throttleRequest();

                // Get valid access token
                String accessToken = getValidAccessToken();
                if (accessToken == null) {
                    return SmsSendResult.failure(name(), "TOKEN_ACQUISITION_FAILED");
                }

                // Normalize MSISDN to international format
                String normalizedMsisdn = normalizeMsisdn(message.to());

                // Build and send request
                // Orange API expects "tel:+237..." format for addresses (with + sign)
                String telFormattedMsisdn = "tel:+" + normalizedMsisdn;
                String senderAddressWithTel = senderAddress.startsWith("tel:") ? senderAddress : "tel:" + senderAddress;

                OutboundSMSMessageRequest outboundRequest = new OutboundSMSMessageRequest(
                        telFormattedMsisdn,
                        senderAddressWithTel,
                        new OutboundSMSTextMessage(message.body()),
                        StringUtils.hasText(senderName) ? senderName : null
                );
                OrangeSmsRequest request = new OrangeSmsRequest(outboundRequest);

                // Use sender address directly in path (Orange API expects raw format, not URL-encoded)
                String url = String.format(ORANGE_SMS_URL_TEMPLATE, smsBaseUrl, senderAddressWithTel);
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

                HttpEntity<OrangeSmsRequest> entity = new HttpEntity<>(request, headers);

                ResponseEntity<OrangeSmsResponse> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, OrangeSmsResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return SmsSendResult.success(name(), response.getBody().transactionId());
                } else {
                    return handleErrorResponse(response.getStatusCode());
                }

            } catch (Exception ex) {
                logger.error("Error sending SMS via Orange", ex);
                return SmsSendResult.failure(name(), mapExceptionToErrorCode(ex));
            }
        }, name());
    }

    private boolean isConfigured() {
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret)
                && StringUtils.hasText(tokenUrl) && StringUtils.hasText(smsBaseUrl)
                && StringUtils.hasText(senderAddress) && StringUtils.hasText(senderName);
    }

    private void throttleRequest() throws InterruptedException {
        rateLimiter.acquire();
        long now = System.currentTimeMillis();
        long timeSinceLastRequest = now - lastRequestTime;
        if (timeSinceLastRequest < THROTTLE_INTERVAL_MS) {
            Thread.sleep(THROTTLE_INTERVAL_MS - timeSinceLastRequest);
        }
        lastRequestTime = System.currentTimeMillis();
    }

    private String getValidAccessToken() {
        if (isTokenValid()) {
            return cachedAccessToken;
        }

        tokenLock.lock();
        try {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return cachedAccessToken;
            }

            return fetchNewToken();
        } finally {
            tokenLock.unlock();
        }
    }

    private boolean isTokenValid() {
        return cachedAccessToken != null && tokenExpiry != null
                && Instant.now().isBefore(tokenExpiry.minusSeconds(TOKEN_REFRESH_BUFFER_SECONDS));
    }

    private String fetchNewToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBasicAuth(clientId, clientSecret);

            String body = "grant_type=client_credentials";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<OrangeTokenResponse> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, entity, OrangeTokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                OrangeTokenResponse tokenResponse = response.getBody();
                cachedAccessToken = tokenResponse.accessToken();
                tokenExpiry = Instant.now().plusSeconds(tokenResponse.expiresIn());
                logger.info("Successfully obtained new Orange access token");
                return cachedAccessToken;
            } else {
                logger.error("Failed to obtain Orange access token: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception ex) {
            logger.error("Error fetching Orange access token", ex);
            return null;
        }
    }

    private String normalizeMsisdn(String msisdn) {
        if (!StringUtils.hasText(msisdn)) {
            throw new IllegalArgumentException("MSISDN cannot be null or empty");
        }

        // Remove "tel:" prefix if present for normalization
        String rawMsisdn = msisdn.replaceAll("^tel:", "");

        String normalized = rawMsisdn.replaceAll("[^0-9+]", "");

        if (normalized.startsWith("00")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("0")) {
            normalized = normalized.substring(1);
        }

        // Add country code if not present (for 9-digit numbers starting with 6 or 2)
        if (normalized.length() == 9 && (normalized.startsWith("6") || normalized.startsWith("2"))) {
            normalized = countryCode + normalized;
        }

        // Validate: should now be country code + national number (e.g., 237 + 9 digits = 12 chars)
        if (!normalized.matches("^[1-9]\\d{7,14}$")) {
            throw new IllegalArgumentException("Invalid MSISDN format: " + msisdn + " (normalized: " + normalized + ")");
        }

        return normalized;
    }

    private SmsSendResult handleErrorResponse(HttpStatusCode statusCode) {
        if (statusCode.value() == HttpStatus.UNAUTHORIZED.value()) {
            // Clear token cache and retry once
            tokenLock.lock();
            try {
                cachedAccessToken = null;
                tokenExpiry = null;
            } finally {
                tokenLock.unlock();
            }
            return SmsSendResult.failure(name(), "UNAUTHORIZED_TOKEN_CLEARED");
        } else if (statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return SmsSendResult.failure(name(), "RATE_LIMIT_EXCEEDED");
        } else if (statusCode.is5xxServerError()) {
            return SmsSendResult.failure(name(), "TRANSIENT_ERROR");
        } else {
            return SmsSendResult.failure(name(), "PERMANENT_ERROR");
        }
    }

    private String mapExceptionToErrorCode(Exception ex) {
        if (ex instanceof InterruptedException) {
            return "INTERRUPTED";
        } else if (ex instanceof IllegalArgumentException) {
            return "INVALID_INPUT";
        } else {
            return "PROVIDER_ERROR";
        }
    }

    // DTOs for Orange API - public for test access
    // The Orange API expects: {"outboundSMSMessageRequest": {"address": "...", "senderAddress": "...", "outboundSMSTextMessage": {"message": "..."}}}

    public record OrangeSmsRequest(
            @JsonProperty("outboundSMSMessageRequest") OutboundSMSMessageRequest outboundSMSMessageRequest
    ) {}

    public record OutboundSMSMessageRequest(
            @JsonProperty("address") String address,
            @JsonProperty("senderAddress") String senderAddress,
            @JsonProperty("outboundSMSTextMessage") OutboundSMSTextMessage outboundSMSTextMessage,
            @JsonProperty("senderName") @JsonInclude(JsonInclude.Include.NON_NULL) String senderName
    ) {}

    public record OutboundSMSTextMessage(
            @JsonProperty("message") String message
    ) {}

    public record OrangeTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {}

    public record OrangeSmsResponse(
            @JsonProperty("transactionId") String transactionId
    ) {}
}
