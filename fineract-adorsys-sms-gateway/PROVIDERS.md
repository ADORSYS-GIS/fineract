# SMS Gateway Provider Guide

This guide explains how to switch SMS providers, configure an existing provider, add a new provider, and implement SMS-related features in the SMS gateway.

## Provider model

The gateway sends all SMS messages through the `SmsProvider` interface:

```java
public interface SmsProvider {
    String name();
    SmsSendResult send(SmsMessage message);
}
```

The provider is selected by name at runtime. The selected provider receives a normalized `SmsMessage` containing:

- `to`: normalized phone number
- `body`: sanitized SMS body
- `type`: message category, for example `OTP`, `ALERT`, `MARKETING`, or `FINERACT_EVENT`
- `provider`: optional per-request provider override
- `metadata`: contextual fields such as OTP purpose

## Supported providers

| Provider name | Class | Purpose |
|---|---|---|
| `console` | `ConsoleSmsProvider` | Local development provider that logs the SMS body to application logs. Use this for local OTP testing. |
| `mock` | `MockSmsProvider` | Test provider for integration tests or environments where delivery is disabled. |
| `twilio` | `TwilioSmsProvider` | Twilio-backed provider for real SMS delivery. |
| `orange` | `OrangeSmsProvider` | Orange SMS API for Francophone Africa (Cameroon-ready with MSISDN normalization and OAuth2). |
| `avlytext` | `AvlytextSmsProvider` | Avlytext SMS aggregator for African markets. |
| `custom-http` | `CustomHttpSmsProvider` | Generic HTTPS provider integration for external SMS vendors. |
| `sns` | `SnsSmsProvider` | AWS SNS for global SMS delivery with configurable sender ID and max price. |
| `whatsapp` | `WhatsappSmsProvider` | WhatsApp message delivery via GOWA sidecar (aldinokemal2104/go-whatsapp-web-multidevice). |

## Switching providers

Set the primary provider with `SMS_PROVIDER_PRIMARY`.

```bash
export SMS_PROVIDER_PRIMARY=console
mvn spring-boot:run
```

Optional fallback delivery is controlled by `SMS_PROVIDER_FALLBACK`. Supports a **comma-separated chain** of providers tried in order.

```bash
export SMS_PROVIDER_PRIMARY=twilio
export SMS_PROVIDER_FALLBACK=orange,avlytext,console
mvn spring-boot:run
```

When a send attempt fails through the primary provider, the gateway tries each fallback provider in order until one succeeds. If all fail, the last error is returned.

## Local development: console provider

Use `console` when testing registration or recovery locally.

```bash
export SMS_PROVIDER_PRIMARY=console
export SMS_PROVIDER_FALLBACK=console
mvn spring-boot:run
```

The OTP appears in the gateway logs:

```text
CONSOLE_SMS type=OTP to=+237670000001 body=Webank verification code: 358344. It expires in 5 minutes.
```

## Mock provider

Use `mock` for automated integration tests that should not send real SMS.

```bash
export SMS_PROVIDER_PRIMARY=mock
export SMS_PROVIDER_FALLBACK=mock
export SMS_MOCK_ENABLED=true
mvn spring-boot:run
```

## Twilio provider

Use `twilio` for real Twilio delivery.

```bash
export SMS_PROVIDER_PRIMARY=twilio
export SMS_PROVIDER_FALLBACK=console
export TWILIO_ACCOUNT_SID=<account-sid>
export TWILIO_AUTH_TOKEN=<auth-token>
export TWILIO_SENDER_NUMBER=<sender-number>
mvn spring-boot:run
```

Keep Twilio credentials in environment variables or your deployment secret manager. Do not commit credentials to source control.

## Custom HTTP provider

Use `custom-http` to integrate a vendor that exposes an HTTPS SMS API.

```bash
export SMS_PROVIDER_PRIMARY=custom-http
export SMS_PROVIDER_FALLBACK=console
export SMS_CUSTOM_HTTP_URL=https://sms-vendor.example.com/messages
export SMS_CUSTOM_HTTP_API_KEY=<api-key>
export SMS_CUSTOM_HTTP_SENDER=Webank
mvn spring-boot:run
```

The custom HTTP provider requires HTTPS for external calls.

## AWS SNS provider

Use `sns` for delivery via AWS SNS.

```bash
export SMS_PROVIDER_PRIMARY=sns
export SMS_PROVIDER_FALLBACK=console
export SMS_SNS_REGION=eu-west-1
export SMS_SNS_SENDER_ID=Webank
export SMS_SNS_MAX_PRICE=1.00
mvn spring-boot:run
```

`SMS_SNS_REGION` is required. Credentials are resolved via the [default AWS credentials chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html) (environment variables, ~/.aws/credentials, IAM roles).

Optional attributes:
- `SMS_SNS_SENDER_ID` — displayed as the SMS sender (default: `Webank`)
- `SMS_SNS_MAX_PRICE` — max price per SMS in USD (no default)

## WhatsApp provider

Use `whatsapp` for delivery via WhatsApp through a GOWA sidecar instance.

```bash
export SMS_PROVIDER_PRIMARY=whatsapp
export SMS_PROVIDER_FALLBACK=console
export SMS_WHATSAPP_BASE_URL=http://gowa:8080
export SMS_WHATSAPP_DEVICE_ID=device-1
mvn spring-boot:run
```

`SMS_WHATSAPP_BASE_URL` is required. The sidecar must expose a `POST /send/message` endpoint accepting `{"phone": "...", "message": "..."}`.

The recommended sidecar is [aldinokemal2104/go-whatsapp-web-multidevice](https://github.com/aldinokemal2104/go-whatsapp-web-multidevice).

## Per-request provider override

OTP generation supports an optional provider field. If provided, it overrides the configured primary provider for that request.

```json
{
  "phoneNumber": "+237670000001",
  "purpose": "onboarding",
  "provider": "console"
}
```

For BFF compatibility, the `/otp/send` endpoint also accepts `provider`.

```json
{
  "phone": "+237670000001",
  "context": "onboarding",
  "provider": "console"
}
```

## OTP configuration

| Variable | Default | Description |
|---|---:|---|
| `OTP_LENGTH` | `6` | Number of OTP digits. |
| `OTP_TTL_SECONDS` | `300` | OTP validity period. |
| `OTP_MAX_SEND_ATTEMPTS` | `3` | Max send requests per principal/window. |
| `OTP_SEND_WINDOW_SECONDS` | `300` | Send rate-limit window. |
| `OTP_MAX_VERIFY_ATTEMPTS` | `5` | Max verification attempts per principal/window. |
| `OTP_VERIFY_WINDOW_SECONDS` | `300` | Verification rate-limit window. |
| `OTP_ISSUER` | `Webank` | Name shown in the OTP message body. |

The OTP service stores only hashed OTP values in memory, enforces TTL, supports one-time use, rate limits send and verify attempts, and removes expired OTPs on a scheduled cleanup.

## Adding a new provider

1. Create a new class under:

```text
src/main/java/com/example/smsgateway/provider/
```

2. Implement `SmsProvider`.

```java
@Component
public class ExampleSmsProvider implements SmsProvider {

    @Override
    public String name() {
        return "example";
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        // Call the provider API here.
        return SmsSendResult.success(name(), "provider-message-id");
    }
}
```

3. Add configuration properties to `application.yml` using environment-variable defaults.

```yaml
sms:
  example:
    url: ${SMS_EXAMPLE_URL:}
    api-key: ${SMS_EXAMPLE_API_KEY:}
```

4. Inject the config into your provider using `@Value` or a typed configuration properties class.

5. Return structured results:

- success: `SmsSendResult.success(providerName, messageId)`
- failure: `SmsSendResult.failure(providerName, errorCode)`

6. Run the gateway with the new provider name.

```bash
export SMS_PROVIDER_PRIMARY=example
mvn spring-boot:run
```

## Implementing new SMS features

Use the existing model instead of creating provider-specific flow logic.

### New message type

Add a value to `MessageType` when the gateway needs to distinguish a new category of SMS.

Examples:

- `OTP`
- `ALERT`
- `MARKETING`
- `FINERACT_EVENT`

Then create messages with that type:

```java
smsService.send(new SmsMessage(phoneNumber, body, MessageType.ALERT, null, metadata));
```

### New endpoint

For new REST APIs:

1. Add request/response records under `model`.
2. Add endpoint methods in the controller.
3. Keep validation strict and errors generic.
4. Delegate delivery to `SmsService`.
5. Add metadata needed for auditing or metrics, but do not log secrets or full sensitive payloads.

### New notification from Fineract events

For Fineract webhook-driven SMS:

1. Add or update the action mapping in `MessageService`.
2. Fetch only the required Fineract data in `FineractService`.
3. Build a sanitized message body.
4. Send through `SmsService` with `MessageType.FINERACT_EVENT`.

### Security checklist

- Do not log OTPs except in `console` provider for local development.
- Do not log API keys, provider credentials, raw Fineract payloads, or customer PII.
- Use HTTPS for external provider APIs.
- Keep credentials in environment variables or secret managers.
- Return generic API errors to clients.
- Enforce TTL, one-time use, and rate limits for OTP flows.
- Add metrics for send failures, fallback usage, rate limiting, and validation failures.

## Useful commands

Run tests:

```bash
mvn test
```

Run locally with console provider:

```bash
SMS_PROVIDER_PRIMARY=console SMS_PROVIDER_FALLBACK=console mvn spring-boot:run
```

Build the Docker image from the fullstack compose:

```bash
docker compose -f ../../webank-mobile/docker-compose.fullstack.yml --env-file ../../webank-mobile/.env.fullstack build sms-gateway
```
