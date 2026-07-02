package com.example.smsgateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthFilterTest {

    @Test
    void allowsRequestWithValidApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/sms/send");
        req.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "secret-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(req, res);
    }

    @Test
    void rejectsRequestWithMissingApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/sms/send");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void rejectsRequestWithWrongApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/otp/send");
        req.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "wrong");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void exemptsFineractWebhookFromAuth() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key");
        // Fineract webhook callback path — no API key header on that side.
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/sms/");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(req, res);
    }

    @Test
    void disablesEnforcementWhenKeyUnconfigured() throws ServletException, IOException {
        // Empty key = enforcement disabled (local dev / test compat).
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/sms/send");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(req, res);
    }
}
