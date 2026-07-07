# ADR-001: Add `POST /sms/send` and gate BFF-facing endpoints with an API key

**Date:** 2025-07-03 · **Status:** Accepted · **Supersedes:** none

## Context

The WeBank P2P viral-loop feature needs the Go BFF to send a transactional SMS to an
**unregistered** recipient — a "claim your money" link — when a sender pushes money
into escrow. The SMS gateway (`fineract-adorsys-sms-gateway`) only exposed:

- `POST /sms/` — Fineract transaction webhook (deposit/withdrawal alerts), callback
  from Fineract, no caller-supplied auth.
- `POST /api/v1/otp/*` and `POST /otp/*` — OTP generation/validation.

None of these fit a BFF-initiated, non-OTP, arbitrary-recipient send. Adding an
unauthenticated send endpoint would re-open the exact open relay the module already
avoids for OTP (which at least rate-limits and gates by principal).

## Decision

1. **Add `POST /sms/send`** taking a typed body `{ phone, message }`, wrapping
   `SmsService`. Inputs are validated synchronously (400 on bad input); the provider
   send is dispatched **asynchronously** on a dedicated thread pool so the returned
   `202 Accepted` is truthful and provider retries (~15s + fallback) cannot starve
   Tomcat workers serving OTP and the Fineract webhook on the same container.
   Messages are tagged `MessageType.TRANSACTIONAL` so metrics/logs separate BFF P2P
   traffic from Fineract-event SMS.

2. **Gate all BFF-facing endpoints** (`/sms/send`, `/otp/*`, `/api/v1/otp/*`) behind
   an `ApiKeyAuthFilter` validating `X-SMS-Gateway-Api-Key` against `SMS_GATEWAY_API_KEY`,
   fail-closed by default. Auth is bypassed only for explicitly exempt paths
   (`/sms/` webhook, `/actuator`, OpenAPI docs) or when
   `SMS_GATEWAY_AUTH_DISABLED=true` (dev/test only, logs a warning).

3. **Map delivery failures to `502 Bad Gateway`** (new `SmsDeliveryException`),
   reserving `429` for genuine rate-limiting so a BFF's standard 429 backoff does
   not retry-storm a permanently broken provider config.

## Consumers

| Consumer | Calls | Auth header |
|---|---|---|
| Go BFF (`webank-mobile`) | `POST /sms/send`, `POST /otp/*` | `X-SMS-Gateway-Api-Key: <SMS_GATEWAY_API_KEY>` |
| Apache Fineract | `POST /sms/` (webhook) | none (exempt; network-restricted) |

The BFF's `SMS_GATEWAY_API_KEY` and the gateway's `SMS_GATEWAY_API_KEY` **must hold
the same value**. The BFF config name is historical/misleading (`KYC_MANAGER_BASE_URL`
actually points at this gateway), kept for compatibility.

## Consequences

- A missing `SMS_GATEWAY_API_KEY` fails closed (401 on all BFF-facing endpoints) —
  loud, not silent. `docker-compose-adorsys.yml` passes the key through from the host
  environment (defaulting to `dev-sms-gateway-key` for the dev stack).
- The Fineract webhook `/sms/` remains unauthenticated at this layer (recipient is
  derived from a real `clientId`; text is templated, not arbitrary). Network-level
  access control restricts reachability; a shared-secret HMAC on the webhook is a
  documented future hardening item, not a blocker for this ADR.
- Adding more BFF-facing endpoints inherits the auth filter automatically — no
  per-route opt-in needed.
