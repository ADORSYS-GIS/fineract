package com.example.smsgateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;

@Service
public class FineractAuthService {

    private static final Logger logger = LoggerFactory.getLogger(FineractAuthService.class);
    private final RestTemplate restTemplate;
    private final String fineractApiUrl;
    private final String fineractUser;
    private final String fineractPassword;
    private final String tenantId;
    private String authToken;

    @Value("${fineract.auth.type}")
    private String authType;

    @Autowired(required = false)
    private KeycloakAuthService keycloakAuthService;

    public FineractAuthService(RestTemplate restTemplate,
                               @Value("${fineract.api.url}") String fineractApiUrl,
                               @Value("${fineract.api.user}") String fineractUser,
                               @Value("${fineract.api.password}") String fineractPassword,
                               @Value("${fineract.api.tenant}") String tenantId) {
        this.restTemplate = restTemplate;
        this.fineractApiUrl = fineractApiUrl;
        this.fineractUser = fineractUser;
        this.fineractPassword = fineractPassword;
        this.tenantId = tenantId;
    }

    public String getAuthToken() {
        if ("oauth".equalsIgnoreCase(authType)) {
            return keycloakAuthService.getAccessToken();
        } else {
            if (authToken == null) {
                login();
            }
            return authToken;
        }
    
    }
    
    public String getAuthType() {
        return authType;
    }

    private void login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Fineract-Platform-TenantId", tenantId);

        String loginUrl = fineractApiUrl + "/authentication";
        String requestBody = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", fineractUser, fineractPassword);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            logger.info("Attempting to log in to Fineract at {}", loginUrl);
            String response = restTemplate.postForObject(loginUrl, request, String.class);
            logger.info("Successfully logged in to Fineract");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            this.authToken = root.path("base64EncodedAuthenticationKey").asText();
        } catch (Exception e) {
            logger.error("Failed to log in to Fineract", e);
            throw new RuntimeException("Failed to parse Fineract auth response", e);
        }
    }
}
