# SMS Gateway — API Contracts

This file tracks the public HTTP contracts exposed by `fineract-adorsys-sms-gateway`
and the consumers that depend on them. Contract changes require updating this file
**and** every consumer listed below.

## Consumers

| Consumer | Repo | Endpoints used | Auth |
|---|---|---|---|
| Go BFF | `webank-mobile` | `POST /sms/send`, `POST /otp/send`, `POST /otp/verify` | `X-KYC-Api-Key` (`KYC_MANAGER_API_KEY`) |
| Apache Fineract | `fineract` | `POST /sms/` (event webhook) | none (exempt; network-restricted) |

## Authentication model

All BFF-facing endpoints require `X-KYC-Api-Key` matching `SMS_GATEWAY_API_KEY`
(fail-closed; 401 on missing/mismatched key). Exempt paths: `POST /sms/` (Fineract
webhook), `/actuator/*`, `/swagger-ui`, `/v3/api-docs`. Dev/test bypass via
`SMS_GATEWAY_AUTH_DISABLED=true` (never in production).

> The BFF's `KYC_MANAGER_API_KEY` and the gateway's `SMS_GATEWAY_API_KEY` are the
> same shared secret. The BFF config name is historical/misleading (`KYC_MANAGER_BASE_URL`
> points at this gateway) and kept for compatibility.

## Endpoints

### `POST /sms/send` — generic transactional SMS (BFF)

Request:
```json
{ "phone": "+237670000000", "message": "..." }
```

- **Auth:** `X-KYC-Api-Key` required.
- **Success:** `202 Accepted` — the send is dispatched asynchronously; input is
  validated synchronously so an invalid `phone`/`message` returns `400
  {"error":"Invalid request"}`.
- **Delivery failure (caller-visible only when called synchronously):** `502 Bad
  Gateway` `{"error":"SMS delivery failed","provider":"...","errorCode":"..."}`.
  Async failures are logged and surfaced via `sms_send_total{status="failure"}`.
- **Rate-limiting:** `429 Too Many Requests` (reserved for upstream rate-limit
  responses / OTP rate limits — not delivery failure).
- **Message type:** tagged `TRANSACTIONAL` (metrics: `sms_send_total`, latency:
  `sms_send_latency`), distinct from `FINERACT_EVENT` used by the webhook path.

### `POST /sms/` — Fineract event webhook (Fineract)

- **Auth:** exempt (Fineract sends no API-key header); network-restricted.
- Recipient derived from `clientId`; text templated from a Fineract SMS template.
- Tagged `FINERACT_EVENT`.

### `POST /api/v1/otp/send` · `POST /api/v1/otp/validate` — OTP

- **Auth:** `X-KYC-Api-Key` required.
- Typed request records (`OtpGenerateRequest`, `OtpValidateRequest`).
- See `README.md` for schemas. Rate limits → `429`.

### `POST /otp/send` · `POST /otp/verify` — OTP (compatibility shims)

- Same auth + behavior as `/api/v1/otp/*`; untyped `Map<String,String>` bodies for
  legacy callers. Typed records are the preferred API.

## Change log

| Date | Change | Consumers updated |
|---|---|---|
| 2025-07-03 | Added `POST /sms/send`; introduced `X-KYC-Api-Key` auth on all BFF-facing endpoints; delivery failure → 502. | Go BFF (P2P viral loop) |
