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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvlytextSmsProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private AvlytextSmsProvider provider;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        provider = new AvlytextSmsProvider(
                restTemplate,
                objectMapper,
                "test-api-key",
                "https://api.avlytext.com",
                "TestSender"
        );
    }

    @Test
    void testSendSuccess() {
        // Mock successful response
        AvlytextSmsProvider.AvlytextSmsResponse response = new AvlytextSmsProvider.AvlytextSmsResponse(
                true, "test-message-id", null
        );
        ResponseEntity<AvlytextSmsProvider.AvlytextSmsResponse> responseEntity = 
                new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(AvlytextSmsProvider.AvlytextSmsResponse.class)))
                .thenReturn(responseEntity);

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "avlytext", Map.of());
        SmsSendResult result = provider.send(message);

        assertTrue(result.success());
        assertEquals("avlytext", result.provider());
        assertEquals("test-message-id", result.messageId());
    }

    @Test
    void testSendApiError() {
        // Mock API error response
        AvlytextSmsProvider.AvlytextError error = new AvlytextSmsProvider.AvlytextError(
                "INVALID_RECIPIENT", "Invalid phone number"
        );
        AvlytextSmsProvider.AvlytextSmsResponse response = new AvlytextSmsProvider.AvlytextSmsResponse(
                false, null, error
        );
        ResponseEntity<AvlytextSmsProvider.AvlytextSmsResponse> responseEntity = 
                new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(AvlytextSmsProvider.AvlytextSmsResponse.class)))
                .thenReturn(responseEntity);

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "avlytext", Map.of());
        SmsSendResult result = provider.send(message);

        assertFalse(result.success());
        assertEquals("avlytext", result.provider());
        assertEquals("INVALID_RECIPIENT", result.errorCode());
    }

    @Test
    void testSendTransientError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(AvlytextSmsProvider.AvlytextSmsResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "avlytext", Map.of());
        SmsSendResult result = provider.send(message);

        assertFalse(result.success());
        assertEquals("avlytext", result.provider());
        assertEquals("TRANSIENT_ERROR", result.errorCode());
    }

    @Test
    void testSendPermanentError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(AvlytextSmsProvider.AvlytextSmsResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "avlytext", Map.of());
        SmsSendResult result = provider.send(message);

        assertFalse(result.success());
        assertEquals("avlytext", result.provider());
        assertEquals("PERMANENT_ERROR", result.errorCode());
    }

    @Test
    void testSendNotConfigured() {
        AvlytextSmsProvider unconfiguredProvider = new AvlytextSmsProvider(
                restTemplate, objectMapper, "", "", ""
        );

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "avlytext", Map.of());
        SmsSendResult result = unconfiguredProvider.send(message);

        assertFalse(result.success());
        assertEquals("avlytext", result.provider());
        assertEquals("PROVIDER_NOT_CONFIGURED", result.errorCode());
    }

    @Test
    void testUrlConstruction() {
        AvlytextSmsProvider.AvlytextSmsResponse response = new AvlytextSmsProvider.AvlytextSmsResponse(
                true, "test-message-id", null
        );
        ResponseEntity<AvlytextSmsProvider.AvlytextSmsResponse> responseEntity = 
                new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(AvlytextSmsProvider.AvlytextSmsResponse.class)))
                .thenReturn(responseEntity);

        SmsMessage message = new SmsMessage("+237698765432", "Test message", MessageType.OTP, "avlytext", Map.of());
        provider.send(message);

        // Verify the URL was constructed correctly
        verify(restTemplate).exchange(
                eq("https://api.avlytext.com/v1/sms?api_key=test-api-key"),
                eq(HttpMethod.POST),
                any(),
                eq(AvlytextSmsProvider.AvlytextSmsResponse.class)
        );
    }
}
