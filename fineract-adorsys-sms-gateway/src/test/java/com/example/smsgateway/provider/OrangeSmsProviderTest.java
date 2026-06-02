package com.example.smsgateway.provider;

import com.example.smsgateway.model.MessageType;
import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrangeSmsProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private OrangeSmsProvider provider;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        provider = new OrangeSmsProvider(
                restTemplate,
                objectMapper,
                "test-client-id",
                "test-client-secret",
                "https://api.orange.com/token",
                "https://api.orange.com/smsmessaging/v1",
                "tel:+237123456789",
                "TestSender",
                "237"
        );
    }

    @Test
    void testSendSuccess() {
        // Mock token response
        OrangeSmsProvider.OrangeTokenResponse tokenResponse = new OrangeSmsProvider.OrangeTokenResponse(
                "test-access-token", "bearer", 3600
        );
        ResponseEntity<OrangeSmsProvider.OrangeTokenResponse> tokenResponseEntity = 
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        // Mock SMS response
        OrangeSmsProvider.OrangeSmsResponse smsResponse = new OrangeSmsProvider.OrangeSmsResponse(
                "test-transaction-id"
        );
        ResponseEntity<OrangeSmsProvider.OrangeSmsResponse> smsResponseEntity = 
                new ResponseEntity<>(smsResponse, HttpStatus.OK);

        when(restTemplate.exchange(eq("https://api.orange.com/token"), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeTokenResponse.class)))
                .thenReturn(tokenResponseEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeSmsResponse.class)))
                .thenReturn(smsResponseEntity);

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "orange", Map.of());
        SmsSendResult result = provider.send(message);

        assertTrue(result.success());
        assertEquals("orange", result.provider());
        assertEquals("test-transaction-id", result.messageId());
    }

    @Test
    void testSendUnauthorizedTokenCleared() {
        // Mock token response
        OrangeSmsProvider.OrangeTokenResponse tokenResponse = new OrangeSmsProvider.OrangeTokenResponse(
                "test-access-token", "bearer", 3600
        );
        ResponseEntity<OrangeSmsProvider.OrangeTokenResponse> tokenResponseEntity = 
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        // Mock unauthorized response
        when(restTemplate.exchange(eq("https://api.orange.com/token"), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeTokenResponse.class)))
                .thenReturn(tokenResponseEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeSmsResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "orange", Map.of());
        SmsSendResult result = provider.send(message);

        assertFalse(result.success());
        assertEquals("orange", result.provider());
        assertEquals("UNAUTHORIZED_TOKEN_CLEARED", result.errorCode());
    }

    @Test
    void testSendRateLimitExceeded() {
        // Mock token response
        OrangeSmsProvider.OrangeTokenResponse tokenResponse = new OrangeSmsProvider.OrangeTokenResponse(
                "test-access-token", "bearer", 3600
        );
        ResponseEntity<OrangeSmsProvider.OrangeTokenResponse> tokenResponseEntity = 
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(eq("https://api.orange.com/token"), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeTokenResponse.class)))
                .thenReturn(tokenResponseEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeSmsResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "orange", Map.of());
        SmsSendResult result = provider.send(message);

        assertFalse(result.success());
        assertEquals("orange", result.provider());
        assertEquals("RATE_LIMIT_EXCEEDED", result.errorCode());
    }

    @Test
    void testSendTransientError() {
        // Mock token response
        OrangeSmsProvider.OrangeTokenResponse tokenResponse = new OrangeSmsProvider.OrangeTokenResponse(
                "test-access-token", "bearer", 3600
        );
        ResponseEntity<OrangeSmsProvider.OrangeTokenResponse> tokenResponseEntity = 
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(eq("https://api.orange.com/token"), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeTokenResponse.class)))
                .thenReturn(tokenResponseEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeSmsResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "orange", Map.of());
        SmsSendResult result = provider.send(message);

        assertFalse(result.success());
        assertEquals("orange", result.provider());
        assertEquals("TRANSIENT_ERROR", result.errorCode());
    }

    @Test
    void testSendNotConfigured() {
        OrangeSmsProvider unconfiguredProvider = new OrangeSmsProvider(
                restTemplate, objectMapper, "", "", "", "", "", "", "", ""
        );

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "orange", Map.of());
        SmsSendResult result = unconfiguredProvider.send(message);

        assertFalse(result.success());
        assertEquals("orange", result.provider());
        assertEquals("PROVIDER_NOT_CONFIGURED", result.errorCode());
    }

    @Test
    void testTokenCaching() {
        // Mock token response
        OrangeSmsProvider.OrangeTokenResponse tokenResponse = new OrangeSmsProvider.OrangeTokenResponse(
                "test-access-token", "bearer", 3600
        );
        ResponseEntity<OrangeSmsProvider.OrangeTokenResponse> tokenResponseEntity = 
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        // Mock SMS response
        OrangeSmsProvider.OrangeSmsResponse smsResponse = new OrangeSmsProvider.OrangeSmsResponse(
                "test-transaction-id"
        );
        ResponseEntity<OrangeSmsProvider.OrangeSmsResponse> smsResponseEntity = 
                new ResponseEntity<>(smsResponse, HttpStatus.OK);

        when(restTemplate.exchange(eq("https://api.orange.com/token"), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeTokenResponse.class)))
                .thenReturn(tokenResponseEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeSmsResponse.class)))
                .thenReturn(smsResponseEntity);

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "orange", Map.of());

        // Send first message
        SmsSendResult result1 = provider.send(message);
        assertTrue(result1.success());

        // Send second message - should reuse token
        SmsSendResult result2 = provider.send(message);
        assertTrue(result2.success());

        // Verify token was requested only once
        verify(restTemplate, times(1)).exchange(eq("https://api.orange.com/token"), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeTokenResponse.class));
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(OrangeSmsProvider.OrangeSmsResponse.class));
    }

}
