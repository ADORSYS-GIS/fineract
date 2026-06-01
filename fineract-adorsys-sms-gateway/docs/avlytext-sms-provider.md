# Avlytext SMS Provider

## Overview

The Avlytext SMS Provider enables sending SMS messages through the Avlytext SMS API using API key authentication.

## Configuration

Add the following configuration to your `application.yml`:

```yaml
avlytext:
  api:
    key: ${AVLYTEXT_API_KEY:}
  base:
    url: ${AVLYTEXT_BASE_URL:}
  sender:
    id: ${AVLYTEXT_SENDER_ID:}
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `AVLYTEXT_API_KEY` | API key for Avlytext authentication | Yes |
| `AVLYTEXT_BASE_URL` | Base URL for Avlytext API | Yes |
| `AVLYTEXT_SENDER_ID` | Sender ID for outgoing messages | Yes |

## Features

### API Authentication
- Uses API key passed as query parameter
- Simple authentication model without token management

### Message Format
- JSON payload with sender, recipient, and message fields
- POST request to `/v1/sms?api_key=...` endpoint

### Error Handling
- **2xx**: Success - checks response `success` field
- **5xx**: Transient error (retry with backoff)
- **4xx**: Permanent error (no retry)

### Response Handling
- Success response: `{"success": true, "messageId": "..."}`
- Error response: `{"success": false, "error": {"code": "...", "message": "..."}}`

### Retry Logic
- Exponential backoff for transient errors
- Up to 4 retry attempts
- Backoff: 1s, 2s, 4s, 8s
- Configurable via `RetryHelper`

## Security Considerations

- Store API key in environment variables or secure vault
- Avoid logging sensitive information (API keys)
- Use HTTPS for all API communications
- Implement proper access controls for configuration

## Usage

```java
@Autowired
private SmsService smsService;

// Send SMS using Avlytext provider
SmsMessage message = new SmsMessage(
    "+237698765432", 
    "Your verification code is 123456", 
    MessageType.OTP, 
    "avlytext", 
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
./mvnw test -Dtest=AvlytextSmsProviderTest
```

Tests cover:
- Successful SMS sending
- API error responses
- HTTP error handling
- Configuration validation
- URL construction
- Retry logic

## API Reference

### Send SMS Endpoint
- **URL**: `{baseUrl}/v1/sms?api_key={apiKey}`
- **Method**: POST
- **Content-Type**: application/json

### Request Body
```json
{
  "sender": "SENDER_ID",
  "recipient": "+237698765432",
  "message": "Your message content"
}
```

### Success Response
```json
{
  "success": true,
  "messageId": "msg-123456"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "INVALID_RECIPIENT",
    "message": "Invalid phone number format"
  }
}
```
