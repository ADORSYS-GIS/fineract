package com.example.smsgateway.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyAuthFilterTest {

    @Test
    void allowsRequestWithValidApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key", false);
        MockHttpServletRequest req = newMockRequest("POST", "/sms/send");
        req.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "secret-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(req, res);
    }

    @Test
    void rejectsRequestWithMissingApiKey()
        throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key", false);
        MockHttpServletRequest req = newMockRequest("POST", "/sms/send");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void rejectsRequestWithWrongApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key", false);
        MockHttpServletRequest req = newMockRequest("POST", "/otp/send");
        req.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "wrong");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void exemptsFineractWebhookFromAuth() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key", false);
        // Fineract webhook callback path — no API key header on that side.
        MockHttpServletRequest req = newMockRequest("POST", "/sms/");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(req, res);
    }

    @Test
    void rejectsWhenKeyUnconfiguredAndAuthEnabled()
        throws ServletException, IOException {
        // Empty key + auth enabled (default) → fail closed (401), not open.
        // A missing secret must be loud, not invisible.
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("", false);
        MockHttpServletRequest req = newMockRequest("POST", "/sms/send");
        req.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "anything");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void allowsAllRequestsWhenAuthExplicitlyDisabled()
        throws ServletException, IOException {
        // SMS_GATEWAY_AUTH_DISABLED=true → dev/test bypass, regardless of key.
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("", true);
        MockHttpServletRequest req = newMockRequest("POST", "/sms/send");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(req, res);
    }

    @Test
    void doesNotExemptNonActuatorPathWithActuatorPrefix()
        throws ServletException, IOException {
        // /actuator-foo must NOT inherit the /actuator exemption (segment boundary).
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key", false);
        MockHttpServletRequest req = newMockRequest("POST", "/actuator-foo");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void exemptsActuatorHealth() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key", false);
        MockHttpServletRequest req = newMockRequest("GET", "/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(req, res);
    }

    /**
     * Builds a mock request whose {@code getServletPath()} matches the request URI,
     * since {@code MockHttpServletRequest} does not derive one from the constructor
     * URI (and the filter reads {@code getServletPath()}, not {@code getRequestURI()}).
     */
    private static MockHttpServletRequest newMockRequest(
        String method,
        String uri
    ) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setServletPath(uri);
        return req;
    }
}
