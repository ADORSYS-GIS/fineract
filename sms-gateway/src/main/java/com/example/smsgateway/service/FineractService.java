package com.example.smsgateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class FineractService {

    private static final Logger logger = LoggerFactory.getLogger(FineractService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FineractAuthService fineractAuthService;

    @Value("${fineract.api.url}")
    private String fineractApiUrl;

    @Value("${fineract.api.tenant}")
    private String tenantId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getClientPhoneNumber(Long clientId) {
        String url = fineractApiUrl + "/clients/" + clientId;
        logger.info("Fetching client phone number from {}", url);
        HttpEntity<String> request = new HttpEntity<>(createHeaders());
        String clientJson = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, request, String.class).getBody();
        logger.info("Received client data: {}", clientJson);
        return parsePhoneNumber(clientJson);
    }

    public String getSmsTemplate(Long templateId) {
        String url = fineractApiUrl + "/templates/" + templateId;
        logger.info("Fetching SMS template from {}", url);
        HttpEntity<String> request = new HttpEntity<>(createHeaders());
        String templateJson = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, request, String.class).getBody();
        logger.info("Received template data: {}", templateJson);
        return parseTemplate(templateJson);
    }

    public String getAccountBalance(Long savingsId) {
        String url = fineractApiUrl + "/savingsaccounts/" + savingsId;
        logger.info("Fetching account balance from {}", url);
        HttpEntity<String> request = new HttpEntity<>(createHeaders());
        String accountJson = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, request, String.class).getBody();
        logger.info("Received account data: {}", accountJson);
        return parseAccountBalance(accountJson);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if ("oauth".equalsIgnoreCase(fineractAuthService.getAuthType())) {
            headers.set("Authorization", "Bearer " + fineractAuthService.getAuthToken());
        } else {
            headers.set("Authorization", "Basic " + fineractAuthService.getAuthToken());
        }
        headers.set("Fineract-Platform-TenantId", tenantId);
        return headers;
    }

    private String parsePhoneNumber(String clientJson) {
        try {
            JsonNode root = objectMapper.readTree(clientJson);
            return root.path("mobileNo").asText();
        } catch (IOException e) {
            logger.error("Error parsing client phone number", e);
            return null;
        }
    }

    private String parseTemplate(String templateJson) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            return root.path("text").asText();
        } catch (IOException e) {
            logger.error("Error parsing SMS template", e);
            return null;
        }
    }

    private String parseAccountBalance(String accountJson) {
        try {
            JsonNode root = objectMapper.readTree(accountJson);
            return root.path("summary").path("accountBalance").asText();
        } catch (IOException e) {
            logger.error("Error parsing account balance", e);
            return null;
        }
    }
}

