package com.example.smsgateway.controller;

import com.example.smsgateway.model.FineractHookPayload;
import com.example.smsgateway.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmsController {

    private static final Logger logger = LoggerFactory.getLogger(SmsController.class);

    @Autowired
    private MessageService messageService;

    @PostMapping("/sms/")
    public void receiveSmsRequest(@RequestBody String rawPayload) {
        logger.info("Received raw payload: {}", rawPayload);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            FineractHookPayload payload = objectMapper.readValue(rawPayload, FineractHookPayload.class);
            logger.info("Successfully deserialized payload: {}", payload);
            messageService.createAndSendMessage(payload);
        } catch (Exception e) {
            logger.error("Error deserializing payload", e);
        }
    }
}
