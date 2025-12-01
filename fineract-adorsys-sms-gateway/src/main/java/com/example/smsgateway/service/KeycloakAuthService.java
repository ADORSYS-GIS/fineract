package com.example.smsgateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class KeycloakAuthService {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakAuthService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fineract.api.user}")
    private String username;

    @Value("${fineract.api.password}")
    private String password;

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.grant-type}")
    private String grantType;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private String accessToken;

    public KeycloakAuthService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String getAccessToken() {
        if (accessToken == null && !authenticate()) {
            logger.error("Authentication failed. Unable to retrieve access token.");
            return null;
        }
        return accessToken;
    }

    private boolean authenticate() {
        logger.info("Authenticating with Keycloak...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    keycloakUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                this.accessToken = responseBody.get("access_token").asText();
                logger.info("Access Token retrieved successfully.");
                return true;
            } else {
                logger.error("Failed to authenticate with Keycloak. Status: {}", response.getStatusCode());
                return false;
            }
        } catch (IOException e) {
            logger.error("Error parsing Keycloak response: {}", e.getMessage());
            return false;
        } catch (HttpClientErrorException e) {
            logger.error("HTTP Client Error during authentication: {}", e.getMessage());
            logger.error("Response body: {}", e.getResponseBodyAsString());
            return false;
        }
    }
}