package com.example.smsgateway.service;

import com.example.smsgateway.model.FineractHookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private FineractService fineractService;

    @Autowired
    private SmsService smsService;

    public void createAndSendMessage(FineractHookPayload payload) {
        String actionName = payload.getActionName();
        Long templateId = getTemplateIdForAction(actionName);

        if (templateId != -1) {
            try {
                String template = fineractService.getSmsTemplate(templateId);
                logger.info("Using template: {}", template);
                String phoneNumber = fineractService.getClientPhoneNumber(payload.getClientId());
                String message = buildMessage(template, payload);

                logger.info("Sending SMS to {}: \"{}\"", phoneNumber, message);
                smsService.sendSms(phoneNumber, message);
            } catch (Exception e) {
                logger.error("Error processing SMS request", e);
            }
        } else {
            logger.warn("No template found for action: {}", actionName);
        }
    }

    private Long getTemplateIdForAction(String actionName) {
        if ("DEPOSIT".equals(actionName)) {
            return 10L;
        } else if ("WITHDRAWAL".equals(actionName)) {
            return 11L;
        }
        return -1L;
    }

    private String buildMessage(String template, FineractHookPayload payload) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String transactionDate = LocalDate.now().format(formatter);
        String accountBalance = fineractService.getAccountBalance(payload.getResponse().getSavingsId());

        String message = template;
        message = message.replace("{depositAmount}", payload.getRequest().getTransactionAmount().toString());
        message = message.replace("{withdrawalAmount}", payload.getRequest().getTransactionAmount().toString());
        message = message.replace("{savingsAccountNumber}", payload.getResponse().getSavingsId().toString());
        message = message.replace("{accountBalance}", accountBalance);
        message = message.replace("{transactionDate}", transactionDate);

        return message;
    }
}
