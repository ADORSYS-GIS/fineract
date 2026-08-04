package com.example.smsgateway.service;

import com.example.smsgateway.model.MessageType;
import com.example.smsgateway.model.OtpGenerateRequest;
import com.example.smsgateway.model.OtpGenerateResponse;
import com.example.smsgateway.model.OtpValidateRequest;
import com.example.smsgateway.model.OtpValidateResponse;
import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private SmsService smsService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        smsService = mock(SmsService.class);
        meterRegistry = new SimpleMeterRegistry();
        when(smsService.normalizePhoneNumber(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testGenerateAndSendWithAppHash() {
        when(smsService.send(any(SmsMessage.class))).thenReturn(new SmsSendResult(true, "msg-123", "OK", null));

        OtpService otpService = new OtpService(
                smsService,
                meterRegistry,
                6,
                300,
                3,
                300,
                5,
                300,
                "Webank",
                "tRpk021s0UP"
        );

        OtpGenerateRequest request = new OtpGenerateRequest("+237690000000", "user-1", "session-1", "registration", "twilio");
        OtpGenerateResponse response = otpService.generateAndSend(request);

        assertNotNull(response.requestId());
        assertEquals("SENT", response.status());

        ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsService).send(captor.capture());
        SmsMessage sentMsg = captor.getValue();

        assertTrue(sentMsg.body().startsWith("Votre code Webank: "));
        assertTrue(sentMsg.body().endsWith("\ntRpk021s0UP"));
        assertEquals(MessageType.OTP, sentMsg.type());
    }

    @Test
    void testGenerateAndSendWithoutAppHash() {
        when(smsService.send(any(SmsMessage.class))).thenReturn(new SmsSendResult(true, "msg-123", "OK", null));

        OtpService otpService = new OtpService(
                smsService,
                meterRegistry,
                6,
                300,
                3,
                300,
                5,
                300,
                "Webank",
                ""
        );

        OtpGenerateRequest request = new OtpGenerateRequest("+237690000000", "user-1", "session-1", "registration", "twilio");
        OtpGenerateResponse response = otpService.generateAndSend(request);

        assertNotNull(response.requestId());

        ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsService).send(captor.capture());
        SmsMessage sentMsg = captor.getValue();

        assertTrue(sentMsg.body().startsWith("Votre code Webank: "));
        assertFalse(sentMsg.body().contains("\n"));
    }

    @Test
    void testInvalidAppHashLengthThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new OtpService(
                smsService,
                meterRegistry,
                6,
                300,
                3,
                300,
                5,
                300,
                "Webank",
                "ZWY1OTBkZmE3ZWM1NDUwYmMyZmJjNzk0MDIzZjNjMzRmZDFhZjg1ZGQ1M2E3OTQ0ZGRkODIwOGJjY2YxZGNiNA" // 86 chars
        ));
    }

    @Test
    void testValidateNullRequestIdWithUnknownPrincipalReturnsInvalidWithoutThrowing() {
        OtpService otpService = new OtpService(
                smsService,
                meterRegistry,
                6,
                300,
                3,
                300,
                5,
                300,
                "Webank",
                "tRpk021s0UP"
        );

        OtpValidateRequest request = new OtpValidateRequest(null, "+237690000000", "user-unknown", "session-1", "registration", "123456");
        OtpValidateResponse response = otpService.validate(request);

        assertFalse(response.valid());
        assertEquals("INVALID", response.status());
    }
}
