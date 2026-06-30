package com.example.smsgateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-KYC-Api-Key";

    @Value("${sms.gateway.api-key:}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        // Only apply to /api/v1/otp/, /otp/ and /sms/ endpoints
        if (path.startsWith("/api/v1/otp/") || path.startsWith("/otp/") || path.startsWith("/sms/")) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (expectedApiKey != null && !expectedApiKey.isEmpty() && !expectedApiKey.equals(apiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
