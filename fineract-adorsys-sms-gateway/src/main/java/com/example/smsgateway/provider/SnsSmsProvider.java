package com.example.smsgateway.provider;

import com.example.smsgateway.model.SmsMessage;
import com.example.smsgateway.model.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;


@Component
public class SnsSmsProvider implements SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(SnsSmsProvider.class);

    private final String region;
    private final String senderId;
    private final String maxPrice;

    private volatile SnsClient snsClient;
    private final Object clientLock = new Object();

    public SnsSmsProvider(
            @Value("${sms.sns.region:}") String region,
            @Value("${sms.sns.sender-id:Webank}") String senderId,
            @Value("${sms.sns.max-price:}") String maxPrice) {
        this.region = region;
        this.senderId = senderId;
        this.maxPrice = maxPrice;
    }

    @Override
    public String name() {
        return "sns";
    }

    private SnsClient getClient() {
        if (snsClient != null) {
            return snsClient;
        }
        synchronized (clientLock) {
            if (snsClient != null) {
                return snsClient;
            }
            if (!isConfigured()) {
                logger.info("SNS SMS provider not configured (missing region)");
                return null;
            }
            try {
                snsClient = SnsClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();
                logger.info("SNS SMS provider initialized for region {}", region);
            } catch (Exception e) {
                logger.error("Failed to initialize SNS client for region {}", region, e);
            }
            return snsClient;
        }
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        SnsClient client = getClient();
        if (client == null) {
            return SmsSendResult.failure(name(), "PROVIDER_NOT_CONFIGURED");
        }

        try {
            PublishRequest.Builder requestBuilder = PublishRequest.builder()
                    .phoneNumber(message.to())
                    .message(message.body());

            java.util.Map<String, MessageAttributeValue> attrs = new java.util.HashMap<>();
            if (StringUtils.hasText(senderId)) {
                attrs.put("AWS.SNS.SMS.SenderID", MessageAttributeValue.builder()
                        .stringValue(senderId).dataType("String").build());
                attrs.put("AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                        .stringValue("Transactional").dataType("String").build());
            }
            if (StringUtils.hasText(maxPrice)) {
                attrs.put("AWS.SNS.SMS.MaxPrice", MessageAttributeValue.builder()
                        .stringValue(maxPrice).dataType("Number").build());
            }
            if (!attrs.isEmpty()) {
                requestBuilder.messageAttributes(attrs);
            }

            PublishResponse response = client.publish(requestBuilder.build());
            logger.info("SNS SMS sent to {}: messageId={}", message.to(), response.messageId());
            return SmsSendResult.success(name(), response.messageId());

        } catch (Exception e) {
            logger.error("SNS SMS send failed for {}", message.to(), e);
            return SmsSendResult.failure(name(), "PROVIDER_ERROR");
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(region);
    }
}
