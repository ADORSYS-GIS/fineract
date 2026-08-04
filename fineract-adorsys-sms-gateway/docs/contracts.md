# SMS Gateway — API Contracts

This file tracks the public HTTP contracts exposed by `fineract-adorsys-sms-gateway`
and the consumers that depend on them. Contract changes require updating this file
**and** every consumer listed below.

## Consumers

| Consumer | Repo | Endpoints used | Auth |
|---|---|---|---|
| Go BFF | `webank-mobile` | `POST /sms/send`, `POST /otp/send`, `POST /otp/verify` | `X-SMS-Gateway-Api-Key` (`SMS_GATEWAY_API_KEY`) |
| Apache Fineract | `fineract` | `POST /sms/` (event webhook) | none (exempt; network-restricted) |

## Authentication model

All BFF-facing endpoints require `X-SMS-Gateway-Api-Key` matching `SMS_GATEWAY_API_KEY`
(fail-closed; 401 on missing/mismatched key). Exempt paths: `POST /sms/` (Fineract
webhook), `/actuator/*`, `/swagger-ui`, `/v3/api-docs`. Dev/test bypass via
`SMS_GATEWAY_AUTH_DISABLED=true` (never in production).

> The BFF's `SMS_GATEWAY_API_KEY` and the gateway's `SMS_GATEWAY_API_KEY` are the
> same shared secret. The BFF config name is historical/misleading (`KYC_MANAGER_BASE_URL`
> points at this gateway) and kept for compatibility.

## Endpoints

### `POST /sms/send` — generic transactional SMS (BFF)

Request:
```json
{ "phone": "+237670000000", "message": "..." }
```

- **Auth:** `X-SMS-Gateway-Api-Key` required.
- **Success:** `202 Accepted` — the send is dispatched asynchronously; input is
  validated synchronously so an invalid `phone`/`message` returns `400
  {"error":"Invalid request"}`.
- **Overload:** `429 Too Many Requests` `{"error":"SMS dispatch overloaded"}` —
  returned synchronously when the `smsSendExecutor` pool+queue are saturated
  (`AbortPolicy`); the send is **not** queued and the BFF should back off. (This is
  distinct from delivery failure, which is never 429.)
- **Delivery failure is not returned over HTTP.** Async provider failures are
  logged and surfaced via `sms_send_total{status="failure"}`; they are **not**
  returned to the /sms/send caller. A `502 Bad Gateway`
  `{"error":"SMS delivery failed","provider":"...","errorCode":"..."}` can only
  occur for **synchronous in-process** callers of
  `SmsService.sendSms(String, String, MessageType)` (e.g. the OTP / Fineract-event
  path), not from `POST /sms/send`, which is async-only (202/400/429).
- **Message type:** tagged `TRANSACTIONAL` (metrics: `sms_send_total`, latency:
  `sms_send_latency`), distinct from `FINERACT_EVENT` used by the webhook path.

### `POST /sms/` — Fineract event webhook (Fineract)

- **Auth:** exempt (Fineract sends no API-key header); network-restricted.
- Recipient derived from `clientId`; text templated from a Fineract SMS template.
- Tagged `FINERACT_EVENT`.

### `POST /api/v1/otp/send` · `POST /api/v1/otp/validate` — OTP

- **Auth:** `X-SMS-Gateway-Api-Key` required.
- Typed request records (`OtpGenerateRequest`, `OtpValidateRequest`).
- See `README.md` for schemas. Rate limits → `429`.

### `POST /otp/send` · `POST /otp/verify` — OTP (compatibility shims)

- Same auth + behavior as `/api/v1/otp/*`; untyped `Map<String,String>` bodies for
  legacy callers. Typed records are the preferred API.

## OTP SMS Body Contract (cross-repo with webank-mobile)

The SMS body produced by `OtpService.generateAndSend` is a contract with the
`OtpExtractor` in `webank-mobile` (see `mobile/lib/shared/utils/otp_extractor.dart`).

### Format

```
Votre code <issuer>: <6-digit-otp>
<11-char-android-app-hash>
```

Example:
```
Votre code WeBank: 482910
tRpk021s0UP
```

### Requirements

1. **Trailing hash**: The 11-character Android SMS Retriever app hash MUST be the
   last line of the SMS, on its own line. The hash is derived from the app's
   package name and signing certificate (see `mobile/scripts/generate_android_sms_hash.sh`).
2. **Issuer**: The issuer string (default "WeBank") is configurable via `otp.issuer`.
3. **OTP length**: Exactly 6 digits. The mobile extractor rejects longer codes
   (anchored regex with `(?<!\d)(\d{6})(?!\d)`) to prevent truncation bugs.
4. **Hash validation**: `OtpService` validates on startup that `ANDROID_OTP_HASH`
   is exactly 11 characters (or blank). An invalid hash causes the service to fail
   loudly at boot time.

### Consumers

| Consumer | Repo | Parsing code |
|---|---|---|
| `OtpExtractor` | `webank-mobile` | `mobile/lib/shared/utils/otp_extractor.dart` |
| `AndroidSmsAutofillService` | `webank-mobile` | `mobile/lib/shared/services/android_sms_autofill_service.dart` |

## Change log

| Date | Change | Consumers updated |
|---|---|---|
| 2025-07-03 | Added `POST /sms/send`; introduced `X-SMS-Gateway-Api-Key` auth on all BFF-facing endpoints; delivery failure → 502. | Go BFF (P2P viral loop) |
| 2026-07-06 | Review fixes: deleted legacy `ApiKeyFilter`+`FilterConfig` so `ApiKeyAuthFilter` is the single auth layer (its `/sms/` exemption now takes effect); restored the full multi-provider fallback cascade in `SmsService.send`; moved async dispatch into `SmsService.sendAsync`; `AbortPolicy`+429 on executor saturation instead of `CallerRunsPolicy`. Clarified that `POST /sms/send` returns 202/400/429 (never 502 over HTTP); 502 applies only to synchronous in-process `SmsService.sendSms(...)` callers. | Go BFF (no contract break) |
| 2026-08-04 | Added Android SMS Retriever support: `OtpService` appends `ANDROID_OTP_HASH` (11-character app hash) to OTP SMS bodies for zero-permission autofill in `webank-mobile`. Added `OtpServiceTest` with coverage for hash validation and SMS body construction. Fixed `requestId == null` guard in `validate()` to prevent NPE on unknown principals. | webank-mobile (ADR-0009) |
