package com.example.smsgateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 * callback and does not send the API key header. Actuator and OpenAPI docs paths are
 * also exempt (read-only, no send capability).
 *
 * <p>Fail-closed by default: a missing {@code SMS_GATEWAY_API_KEY} rejects every
 * BFF-facing request with 401 so a misconfiguration is loud, not invisible. To
 * disable auth entirely for local dev/test, set {@code SMS_GATEWAY_AUTH_DISABLED=true}
 * (this logs a startup warning and is never the production default).
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    static final String API_KEY_HEADER = "X-KYC-Api-Key";

    private final String expectedApiKey;
    private final boolean authDisabled;

    public ApiKeyAuthFilter(
            @Value("${SMS_GATEWAY_API_KEY:}") String expectedApiKey,
            @Value("${SMS_GATEWAY_AUTH_DISABLED:false}") boolean authDisabled) {
        this.expectedApiKey = expectedApiKey;
        this.authDisabled = authDisabled;
        if (authDisabled) {
            logger.warn(
                    "SMS gateway API-key auth is DISABLED (SMS_GATEWAY_AUTH_DISABLED=true) "
                            + "— this must never be used in production");
        } else if (expectedApiKey == null || expectedApiKey.isBlank()) {
            logger.error(
                    "SMS_GATEWAY_API_KEY is not set and auth is not disabled — all "
                            + "BFF-facing requests will be rejected with 401 until a key "
                            + "is configured");
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        // Paths exempt from API-key auth:
        //  - /sms/            : Fineract webhook callback (Fineract has no API key to send).
        //  - /actuator/...     : health/metrics probes by the orchestrator.
        //  - /swagger-ui, /v3/ : OpenAPI docs (read-only, no send capability).
        if ("/sms/".equals(path)
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Explicit dev/test bypass — never the production default. A missing secret
        // does NOT take this path; it fails closed below so misconfig is loud.
        if (authDisabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Auth enabled but no key configured → fail closed on every request so the
        // operator notices the missing secret instead of silently running open.
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            logger.warn(
                    "Rejecting request to {}: no API key configured "
                            + "(set SMS_GATEWAY_API_KEY or set SMS_GATEWAY_AUTH_DISABLED=true for dev)",
                    path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"UNAUTHORIZED\",\"message\":\"API key not configured\"}");
            return;
        }

        String provided = request.getHeader(API_KEY_HEADER);
        // Constant-time comparison to avoid timing-leaking the shared secret.
        if (provided == null
                || !MessageDigest.isEqual(
                        expectedApiKey.getBytes(StandardCharsets.UTF_8),
                        provided.getBytes(StandardCharsets.UTF_8))) {
            logger.warn("Rejected request to {}: missing or invalid API key", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid API key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
