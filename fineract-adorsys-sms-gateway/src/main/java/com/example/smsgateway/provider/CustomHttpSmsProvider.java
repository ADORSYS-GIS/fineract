package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@Component
public class CustomHttpSmsProvider implements SmsProvider {

    private final RestTemplate restTemplate;
    private final String url;
    private final String apiKey;
    private final String sender;

    public CustomHttpSmsProvider(
            RestTemplate restTemplate,
            @Value("${sms.custom-http.url:}") String url,
            @Value("${sms.custom-http.api-key:}") String apiKey,
            @Value("${sms.custom-http.sender:Webank}") String sender) {
        this.restTemplate = restTemplate;
        this.url = url;
        this.apiKey = apiKey;
        this.sender = sender;
    }

    @Override
    public String name() {
        return "custom-http";
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        if (!StringUtils.hasText(url) || !StringUtils.hasText(apiKey)) {
            return SmsSendResult.failure(name(), "PROVIDER_NOT_CONFIGURED");
        }
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return SmsSendResult.failure(name(), "HTTPS_REQUIRED");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        Map<String, String> payload = Map.of(
                "to", message.to(),
                "from", sender,
                "body", message.body(),
                "type", message.type().name()
        );
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return SmsSendResult.success(name(), null);
        }
        return SmsSendResult.failure(name(), "PROVIDER_REJECTED");
    }
}
