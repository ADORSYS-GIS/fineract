# Orange SMS Provider

## Overview

The Orange SMS Provider enables sending SMS messages through the Orange SMS API using OAuth2 client credentials flow for authentication.

## Configuration

Add the following configuration to your `application.yml`:

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

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `ORANGE_CLIENT_ID` | OAuth2 client ID for Orange API | Yes |
| `ORANGE_CLIENT_SECRET` | OAuth2 client secret for Orange API | Yes |
| `ORANGE_TOKEN_URL` | OAuth2 token endpoint URL | Yes |
| `ORANGE_SMS_BASE_URL` | Base URL for Orange SMS API | Yes |
| `ORANGE_SENDER_ADDRESS` | Sender address in tel: format | Yes |
| `ORANGE_SENDER_NAME` | Sender name displayed to recipients | Yes |
| `ORANGE_COUNTRY_CODE` | Default country code for MSISDN normalization (default: 237) | No |

## Features

### OAuth2 Token Management
- Automatic token acquisition using client credentials flow
- Token caching with 5-minute refresh buffer before expiry
- Thread-safe token refresh mechanism
- Automatic token cache clearing on 401 errors

### MSISDN Normalization
- Converts phone numbers to international format (tel:+237XXXXXXXXX)
- Supports various input formats:
  - `+237698765432` → `tel:+237698765432`
  - `237698765432` → `tel:+237698765432`
  - `698765432` → `tel:+237698765432`
  - `00237698765432` → `tel:+237698765432`

### Rate Limiting
- Throttles requests to maximum 5 SMS per second
- 200ms delay between requests
- Uses semaphore for concurrent request control

### Error Handling
- **2xx**: Success
- **401**: Clears token cache and retries once
- **429**: Rate limit exceeded (transient error)
- **5xx**: Transient error (retry with backoff)
- **Other 4xx**: Permanent error (no retry)

### Retry Logic
- Exponential backoff for transient errors
- Up to 4 retry attempts
- Backoff: 1s, 2s, 4s, 8s
- Configurable via `RetryHelper`

## Security Considerations

- Store credentials in environment variables or secure vault
- Avoid logging sensitive information (tokens, secrets)
- Use HTTPS for all API communications
- Implement proper access controls for configuration

## Usage

```java
@Autowired
private SmsService smsService;

// Send SMS using Orange provider
SmsMessage message = new SmsMessage(
    "+237698765432",
    "Your verification code is 123456",
    MessageType.OTP,
    "orange",
    Map.of()
);

SmsSendResult result = smsService.send(message);
```

## Monitoring

The provider emits metrics for:
- `sms_send_total` - Counter with provider and status tags
- `sms_send_latency` - Timer for send operation duration

## Testing

Run unit tests with:
```bash
./mvnw test -Dtest=OrangeSmsProviderTest
```

Tests cover:
- Successful SMS sending
- Token caching and refresh
- MSISDN normalization
- Error handling scenarios
- Rate limiting
- Retry logic
