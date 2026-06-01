# SMS Provider Configuration Guide

## Overview

The SMS Gateway supports multiple SMS providers through a unified `SmsProvider` interface. Currently supported providers:
- Twilio (existing)
- Orange SMS (new)
- Avlytext SMS (new)

## Provider Selection

Configure the primary and fallback providers in `application.yml`:

```yaml
sms:
  provider:
    primary: orange  # or twilio, avlytext
    fallback: twilio # optional
```

## Provider-Specific Configuration

### Twilio

```yaml
twilio:
  account:
    sid: ${TWILIO_ACCOUNT_SID:}
  auth:
    token: ${TWILIO_AUTH_TOKEN:}
  sender:
    number: ${TWILIO_SENDER_NUMBER:}
```

### Orange SMS

```yaml
orange:
  client:
    id: ${ORANGE_CLIENT_ID:}
    secret: ${ORANGE_CLIENT_SECRET:}
  token:
    url: ${ORANGE_TOKEN_URL:}
  sms:
    base:
      url: ${ORANGE_SMS_BASE_URL:}
  sender:
    address: ${ORANGE_SENDER_ADDRESS:}
    name: ${ORANGE_SENDER_NAME:}
  country:
    code: ${ORANGE_COUNTRY_CODE:237}
```

### Avlytext SMS

```yaml
avlytext:
  api:
    key: ${AVLYTEXT_API_KEY:}
  base:
    url: ${AVLYTEXT_BASE_URL:}
  sender:
    id: ${AVLYTEXT_SENDER_ID:}
```

## Security Best Practices

### Credential Management

1. **Environment Variables**: Store all sensitive credentials in environment variables
2. **Vault Integration**: Consider using HashiCorp Vault or AWS Secrets Manager for production
3. **Access Control**: Limit access to credentials to necessary service accounts only
4. **Rotation**: Implement regular credential rotation policies

### Example Environment Setup

```bash
# Twilio
export TWILIO_ACCOUNT_SID="your-account-sid"
export TWILIO_AUTH_TOKEN="your-auth-token"
export TWILIO_SENDER_NUMBER="+1234567890"

# Orange SMS
export ORANGE_CLIENT_ID="your-client-id"
export ORANGE_CLIENT_SECRET="your-client-secret"
export ORANGE_TOKEN_URL="https://api.orange.com/oauth/v2/token"
export ORANGE_SMS_BASE_URL="https://api.orange.com/smsmessaging/v1"
export ORANGE_SENDER_ADDRESS="tel:+237123456789"
export ORANGE_SENDER_NAME="YourApp"

# Avlytext SMS
export AVLYTEXT_API_KEY="your-api-key"
export AVLYTEXT_BASE_URL="https://api.avlytext.com"
export AVLYTEXT_SENDER_ID="YourSender"
```

## Deployment Configuration

### Docker Compose

```yaml
version: '3.8'
services:
  sms-gateway:
    image: sms-gateway:latest
    environment:
      # Provider selection
      - SMS_PROVIDER_PRIMARY=orange
      - SMS_PROVIDER_FALLBACK=twilio
      
      # Twilio
      - TWILIO_ACCOUNT_SID=${TWILIO_ACCOUNT_SID}
      - TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN}
      - TWILIO_SENDER_NUMBER=${TWILIO_SENDER_NUMBER}
      
      # Orange SMS
      - ORANGE_CLIENT_ID=${ORANGE_CLIENT_ID}
      - ORANGE_CLIENT_SECRET=${ORANGE_CLIENT_SECRET}
      - ORANGE_TOKEN_URL=${ORANGE_TOKEN_URL}
      - ORANGE_SMS_BASE_URL=${ORANGE_SMS_BASE_URL}
      - ORANGE_SENDER_ADDRESS=${ORANGE_SENDER_ADDRESS}
      - ORANGE_SENDER_NAME=${ORANGE_SENDER_NAME}
      
      # Avlytext SMS
      - AVLYTEXT_API_KEY=${AVLYTEXT_API_KEY}
      - AVLYTEXT_BASE_URL=${AVLYTEXT_BASE_URL}
      - AVLYTEXT_SENDER_ID=${AVLYTEXT_SENDER_ID}
```

### Kubernetes

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: sms-gateway-secrets
type: Opaque
stringData:
  TWILIO_ACCOUNT_SID: "your-account-sid"
  TWILIO_AUTH_TOKEN: "your-auth-token"
  ORANGE_CLIENT_ID: "your-client-id"
  ORANGE_CLIENT_SECRET: "your-client-secret"
  AVLYTEXT_API_KEY: "your-api-key"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sms-gateway
spec:
  template:
    spec:
      containers:
      - name: sms-gateway
        image: sms-gateway:latest
        envFrom:
        - secretRef:
            name: sms-gateway-secrets
        env:
        - name: SMS_PROVIDER_PRIMARY
          value: "orange"
        - name: SMS_PROVIDER_FALLBACK
          value: "twilio"
```

## Monitoring and Observability

### Metrics

All providers emit the following metrics:

- `sms_send_total` - Counter with tags:
  - `provider`: Provider name (twilio, orange, avlytext)
  - `status`: success, failure, provider_error, etc.

- `sms_send_latency` - Timer with tags:
  - `provider`: Provider name

### Logging

Configure logging levels for provider-specific debugging:

```yaml
logging:
  level:
    com.example.smsgateway.provider: DEBUG
    org.springframework.web.client: DEBUG
```

### Health Checks

Provider health can be monitored through:
- Application metrics
- Log analysis
- Custom health endpoints (if implemented)

## Testing

### Unit Tests

Run provider-specific tests:
```bash
# Orange SMS Provider
./mvnw test -Dtest=OrangeSmsProviderTest

# Avlytext SMS Provider
./mvnw test -Dtest=AvlytextSmsProviderTest

# Retry Helper
./mvnw test -Dtest=RetryHelperTest
```

### Integration Tests

Test provider connectivity:
```bash
# Test with actual credentials (use test environment)
./mvnw test -Dtest=ProviderIntegrationTest -Dspring.profiles.active=test
```

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Verify credentials are correct
   - Check token URLs and endpoints
   - Ensure proper permissions

2. **Rate Limiting**
   - Monitor API rate limits
   - Implement proper backoff strategies
   - Consider multiple providers for load distribution

3. **Network Issues**
   - Verify connectivity to provider endpoints
   - Check firewall rules
   - Monitor DNS resolution

4. **Configuration Errors**
   - Validate YAML syntax
   - Check environment variable names
   - Verify required configuration is present

### Debug Mode

Enable debug logging for detailed troubleshooting:
```yaml
logging:
  level:
    com.example.smsgateway.provider: TRACE
    org.springframework.web.client: TRACE
    org.springframework.http: TRACE
```

## Migration Guide

### Switching Providers

1. Update `SMS_PROVIDER_PRIMARY` configuration
2. Add new provider credentials
3. Test with fallback provider enabled
4. Monitor metrics during transition
5. Remove old provider credentials after validation

### Configuration Validation

The application will fail to start if required provider configuration is missing. Validate configuration before deployment:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"
```
