package com.example.smsgateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the {@code X-KYC-Api-Key} header against the configured
 * {@code SMS_GATEWAY_API_KEY} for BFF-facing endpoints.
 *
 * <p>This closes an open SMS-spam/smishing relay: without it, any caller that can
 * reach the service (e.g. over the docker network) could send arbitrary text to any
 * phone number via {@code /sms/send}, billed to the operator's SMS provider account.
 *
 * <p>The Fineract webhook {@code POST /sms/} is exempt — Fineract invokes it as a
 * callback and does not send the API key header.
 *
 * <p>If {@code SMS_GATEWAY_API_KEY} is empty/unset, enforcement is disabled to keep
 * local dev and test setups working; the docker-compose stack always sets it.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(
        ApiKeyAuthFilter.class
    );

    static final String API_KEY_HEADER = "X-KYC-Api-Key";

    private final String expectedApiKey;

    public ApiKeyAuthFilter(
        @Value("${SMS_GATEWAY_API_KEY:}") String expectedApiKey
    ) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Paths exempt from API-key auth:
        //  - /sms/            : Fineract webhook callback (Fineract has no API key to send).
        //  - /actuator/...     : health/metrics probes by the orchestrator.
        //  - /swagger-ui, /v3/ : OpenAPI docs (read-only, no send capability).
        if (
            "/sms/".equals(path) ||
            path.startsWith("/actuator") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        // If no key is configured, skip enforcement (local dev / test compat).
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(API_KEY_HEADER);
        if (provided == null || !expectedApiKey.equals(provided)) {
            logger.warn(
                "Rejected request to {}: missing or invalid API key",
                path
            );
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response
                .getWriter()
                .write(
                    "{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid API key\"}"
                );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
