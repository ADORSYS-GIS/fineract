# SMS Gateway

This Spring Boot service provides a generic SMS gateway for Fineract webhooks and BFF-driven OTP flows. It supports provider-agnostic SMS delivery, configurable provider selection/fallback, secure OTP generation/validation, audit-safe logging, and Prometheus metrics.

## Setup

### Environment Variables

Before running the application, you need to set the following environment variables:

| Variable | Description |
|---|---|
| `SMS_PROVIDER_PRIMARY` | Primary provider name. Defaults to `twilio`. |
| `SMS_PROVIDER_FALLBACK` | Optional fallback provider. Defaults to `mock`. |
| `SMS_MOCK_ENABLED` | Enable the mock provider for local/integration tests. Defaults to `false`. |
| `TWILIO_ACCOUNT_SID` | Twilio account SID. |
| `TWILIO_AUTH_TOKEN` | Twilio auth token. |
| `TWILIO_SENDER_NUMBER` | Twilio sender phone number. |
| `ORANGE_CLIENT_ID` | Orange OAuth2 client ID. |
| `ORANGE_CLIENT_SECRET` | Orange OAuth2 client secret. |
| `ORANGE_TOKEN_URL` | Orange OAuth2 token endpoint URL. |
| `ORANGE_SMS_BASE_URL` | Orange SMS API base URL. |
| `ORANGE_SENDER_ADDRESS` | Orange sender address in tel: format. |
| `ORANGE_SENDER_NAME` | Orange sender name displayed to recipients. |
| `ORANGE_COUNTRY_CODE` | Default country code for MSISDN normalization (default: 237). |
| `AVLYTEXT_API_KEY` | Avlytext API key. |
| `AVLYTEXT_BASE_URL` | Avlytext API base URL. |
| `AVLYTEXT_SENDER_ID` | Avlytext sender ID. |
| `SMS_CUSTOM_HTTP_URL` | HTTPS endpoint for a custom HTTP provider. |
| `SMS_CUSTOM_HTTP_API_KEY` | API key for the custom HTTP provider. |
| `SMS_CUSTOM_HTTP_SENDER` | Sender identifier for the custom HTTP provider. Defaults to `Webank`. |
| `OTP_LENGTH` | OTP digit length. Defaults to `6`. |
| `OTP_TTL_SECONDS` | OTP expiry in seconds. Defaults to `300`. |
| `OTP_MAX_SEND_ATTEMPTS` | Max OTP send attempts per user/session/phone/purpose window. Defaults to `3`. |
| `OTP_SEND_WINDOW_SECONDS` | OTP send rate-limit window. Defaults to `300`. |
| `OTP_MAX_VERIFY_ATTEMPTS` | Max OTP validation attempts per user/session/phone/purpose window. Defaults to `5`. |
| `OTP_VERIFY_WINDOW_SECONDS` | OTP validation rate-limit window. Defaults to `300`. |
| `OTP_ISSUER` | OTP message issuer. Defaults to `Webank`. |
| `FINERACT_API_URL` | The URL of your Fineract instance. |
| `FINERACT_API_USER` | The username for your Fineract instance. |
| `FINERACT_API_PASSWORD` | The password for your Fineract instance. |

### Running the Application

You can run the application using the following command:

```bash
mvn spring-boot:run
```

For local OTP testing without sending real SMS:

```bash
export SMS_PROVIDER_PRIMARY=mock
export SMS_MOCK_ENABLED=true
mvn spring-boot:run
```

## Flow of the Messaging System

1. **Fineract Webhook:** Fineract is configured to send a webhook notification to the `/sms/` endpoint of this application when a transaction (deposit or withdrawal) occurs.
2. **SmsController:** The `SmsController` receives the webhook payload, which is a JSON object containing information about the transaction.
3. **MessageService:** The `SmsController` passes the payload to the `MessageService`, which is responsible for creating and sending the SMS message.
4. **FineractService:** The `MessageService` uses the `FineractService` to fetch the SMS template and the client's phone number from the Fineract API.
5. **SmsService:** The `MessageService` uses the `SmsService` to send the SMS message to the client using the Twilio API.

The existing Fineract webhook endpoint remains:

```http
POST /sms/
```

## OTP API

The Go BFF should call the SMS gateway; Flutter should call the BFF rather than this service directly.

### Generate and send OTP

```http
POST /api/v1/otp/send
Content-Type: application/json
```

```json
{
  "phoneNumber": "+237670000000",
  "userId": "user-123",
  "sessionId": "session-123",
  "purpose": "registration",
  "provider": "twilio"
}
```

Response:

```json
{
  "requestId": "generated-request-id",
  "expiresInSeconds": 300,
  "status": "SENT"
}
```

### Validate OTP

```http
POST /api/v1/otp/validate
Content-Type: application/json
```

```json
{
  "requestId": "generated-request-id",
  "phoneNumber": "+237670000000",
  "userId": "user-123",
  "sessionId": "session-123",
  "purpose": "registration",
  "otp": "123456"
}
```

Response:

```json
{
  "valid": true,
  "status": "VALID"
}
```

Failed validation returns a generic response:

```json
{
  "valid": false,
  "status": "INVALID"
}
```

## Providers

Supported provider names:

- `twilio` - Twilio SMS API
- `orange` - Orange SMS API with OAuth2 authentication
- `avlytext` - Avlytext SMS API with API key authentication
- `custom-http` - Generic HTTP SMS provider
- `console` - Logs SMS to console (for local development/testing)
- `mock` - Mock provider for testing

Provider credentials are configured externally through environment variables. OTP values and sensitive payloads are not logged. Custom HTTP providers must use HTTPS.

### Provider Features

**Orange SMS:**
- OAuth2 client credentials flow with token caching
- MSISDN normalization to international format
- Rate limiting (5 SMS/sec max)
- Automatic retry with exponential backoff
- Comprehensive error handling

**Avlytext SMS:**
- Simple API key authentication
- JSON payload support
- Automatic retry with exponential backoff
- Error response handling

**Console Provider (Local Testing):**
- Logs all SMS messages to container logs
- Displays OTP codes and message content for easy debugging
- No external SMS service required
- Perfect for local development and registration testing

For detailed configuration and usage instructions, see:
- [Orange SMS Provider Documentation](docs/orange-sms-provider.md)
- [Avlytext SMS Provider Documentation](docs/avlytext-sms-provider.md)
- [Provider Configuration Guide](docs/provider-configuration.md)

### Local Development with Console Provider

For local development, use the `console` provider to log SMS messages instead of sending real SMS:

```bash
# Set console as primary provider
export SMS_PROVIDER_PRIMARY=console
export SMS_PROVIDER_FALLBACK=console
export SMS_MOCK_ENABLED=false

# Run the application
mvn spring-boot:run
```

View SMS logs (including OTP codes):
```bash
# Docker logs
docker logs <sms-gateway-container>

# Or if running locally, check console output
```

Example log output:
```
CONSOLE_SMS type=OTP to=+237698765432 body=Your Webank verification code is 123456
```

## Metrics and API Docs

- `GET /actuator/health`
- `GET /actuator/prometheus`
- `GET /swagger-ui.html`
- `GET /v3/api-docs`

Metrics include SMS send counts/latency and OTP event counts.

## How it was Setup

The application is a standard Spring Boot application. It uses the following dependencies:

* **spring-boot-starter-web:** For creating the REST endpoint.
* **spring-boot-starter-test:** For testing.
* **twilio:** For sending SMS messages.
* **jackson-databind:** For parsing JSON.

The application is configured in the `application.properties` file. The Twilio and Fineract credentials are read from environment variables to avoid storing them in the codebase.
