# Apache Fineract Threat Model

## §1 Header

- **Project:** Apache Fineract
- **Version:** 1.15.0-SNAPSHOT (HEAD as of 2026-06-10)
- **Date:** 2026-06-10
- **Author(s):** Threat Model Producer (AI-assisted)
- **Status:** Draft — under maintainer review

**Project purpose:** Apache Fineract is an open-source core banking platform designed for microfinance institutions, fintechs, and digital lenders. It provides a REST API layer for back-office operations (loan origination, client onboarding, savings management, accounting). It is a Spring Boot application with a multi-tenant database model (PostgreSQL or MariaDB), optional message brokers (Kafka/ActiveMQ) for events and batch partitioning, and supports multiple authentication schemes (HTTP Basic, OAuth2, 2FA).

---

## §2 Scope and intended use

### Primary intended use cases

- **Back-office core banking:** Loan officers, tellers, and administrators manage clients, loans, savings, deposits, and accounting via REST APIs.
- **Batch end-of-day processing:** Close-of-Business (COB) jobs run via Spring Batch, optionally partitioned across worker nodes.
- **External business events:** Publishing events (e.g., ClientCreated) to downstream systems via Kafka or ActiveMQ.

### Deployment contexts

- **Server / daemon:** Spring Boot executable JAR with embedded Tomcat (default), or WAR deployed to external Tomcat.
- **Containerized:** Docker Compose and Kubernetes manifests ship in the repository.
- **Multi-instance topology:** Read-only instances, write-enabled instances, batch-manager instances, and batch-worker instances can be segregated.
- **Not an in-process library:** Fineract is a standalone service; it is not designed to be embedded as a library inside another JVM application.

### Caller roles

| Role | Trust level | Description |
|------|-------------|-------------|
| Back-office user | Authenticated, authorized | Loan officers, branch managers, system admins. Hold tenant-scoped credentials. |
| Operator / deployer | Trusted for the instance | Sysadmin who configures application.properties, SSL, DB credentials, OAuth issuer. |
| Peer / message broker | Authenticated | Kafka/ActiveMQ nodes or other Fineract instances in a partitioned batch topology. |
| External event consumer | Authenticated | Downstream systems reading business events from Kafka/ActiveMQ topics. |

### Component-family table

| Family | Representative API / Entry Point | In model? |
|--------|-------------------------------|-----------|
| Core REST | `/api/v1/clients`, `/api/v1/loans` | Yes |
| Batch Engine (COB) | Spring Batch job launcher, partitioned workers | Yes |
| Event Publisher | ClientCreated, LoanDisbursed to Kafka/ActiveMQ | Yes |
| Actuator / Health | `/actuator/health`, `/actuator/info` | Yes |
| Community Web App | (separate repo) | No — separate project; out of scope per §3 |
| Kubernetes manifests | `kubernetes/` directory | No — deployment artifact, not runtime code; out of scope per §3 |
| Docker files | `docker-compose*.yml` | No — deployment artifact; out of scope per §3 |
| Integration tests | `oauth2-tests/`, `fineract-e2e` | No — test-only; out of scope |

---

## §3 Out of scope (explicit non-goals)

### Use cases not supported

- **Direct internet-facing deployment without a reverse proxy / WAF:** The project documentation explicitly recommends running behind NGINX or a cloud-native load balancer with SSL termination.
- **Cryptocurrency or blockchain ledger integration:** Fineract is a traditional double-entry accounting system; it does not provide blockchain primitives.
- **Real-time payment rails (RTGS, SWIFT):** Fineract manages internal accounts; integration with external payment networks is a downstream responsibility.
- **Customer-facing UI:** The "Community App" is a separate repository (openMF/community-app) and is not part of this model.
- **Self-Service Plugin:** The "Self-Service Plugin" is a separate repository (openMF/selfservice-plugin) and is not part of this model.

### Threats the project does not attempt to defend against

- **Physical access to the database server:** If an attacker has OS-level access to the PostgreSQL/MariaDB host, the project assumes the game is already lost.
- **Network-level DDoS:** The embedded Tomcat has configurable connection limits, but volumetric DDoS mitigation is explicitly left to the reverse proxy / cloud provider.

### Code shipped but not covered

| Code | Policy | Reason |
|------|--------|--------|
| `kubernetes/` | Out of scope | Deployment orchestration; threat-model separately as infrastructure. |
| `docker-compose*.yml` | Out of scope | Deployment orchestration; not runtime code. |
| `fineract-e2e/`, `oauth2-tests/` | Out of scope | Test-only code; not shipped in production artifacts. |

---

## §4 Trust boundaries and data flow

### Where the trust boundary sits

The **API surface (HTTP/S)** is the primary trust boundary. Once a request has passed authentication and tenant resolution, data inside the Spring Boot process is treated as trusted.

The **database connection pool (HikariCP)** is a secondary trust boundary: SQL queries generated by the application are trusted by the database.

The **message broker (Kafka/ActiveMQ)** is a tertiary trust boundary when used for batch partitioning: messages between manager and worker nodes are trusted once they enter the broker.

### Data flow

```
[Untrusted Internet] --(TLS)--> [Reverse Proxy/WAF] --(TLS)--> [Fineract API (Tomcat)]
                                                                         |
                                                                         | (JDBC, authenticated pool)
                                                                         v
                                                              [PostgreSQL/MariaDB]
                                                                         |
                                                                         | (tenant-scoped schema)
                                                                         v
                                                              [Tenant Data Schema]
```

**Trust transitions:**

1. **Internet → Reverse Proxy:** TLS-encrypted; certificate validation required. The project ships a self-signed cert for localhost only; production requires a CA-trusted cert or managed proxy.
2. **Reverse Proxy → Fineract API:** TLS-encrypted; X-Forwarded-For and X-Forwarded-Proto respected if proxy is present.
3. **Fineract API → Database:** JDBC over TCP; credentials from application.properties / env vars; connection pool authenticated per-tenant.
4. **Fineract API → Message Broker:** Kafka (PLAINTEXT or SASL/SSL) or ActiveMQ (OpenWire); authentication depends on broker configuration.

### Reachability preconditions per component family

| Component family | Reachability precondition |
|------------------|--------------------------|
| Core REST API | Finding is in-model only if reachable from an authenticated HTTP request to `/api/v1/...` or `/api/v2/...` with a valid `Fineract-Platform-TenantId` header |
| Batch Engine | Finding is in-model only if reachable from a batch job launch (manager) or partitioned step execution (worker). Batch jobs are reachable only from authenticated back-office users or scheduled triggers. |
| Event Publisher | Finding is in-model only if reachable from an internal business event (e.g., ClientCreated) that flows to the broker. The broker itself is out of model per §3, but the producer-side serialization is in-model. |
| Actuator | Finding is in-model only if reachable from `/actuator/health` or `/actuator/info`. |

---

## §5 Assumptions about the environment

### Operating system, runtime, hardware

- **Java 21+** (Azul Zulu recommended).
- **PostgreSQL >= 18.0** or MariaDB/MySQL as the relational database.
- **Spring Boot 3.x** with embedded Tomcat.
- The JVM provides standard memory safety and garbage collection.

### Concurrency assumptions

- Fineract uses Spring's transaction management with `@Transactional`. Database isolation is delegated to the underlying RDBMS.
- Multi-tenancy is implemented via schema-per-tenant or database-per-tenant; tenant resolution happens at request entry via `Fineract-Platform-TenantId` header.
- Spring Batch remote partitioning uses a manager-worker model.

### Memory and input size

- Input size bounds are enforced at the API layer (Tomcat `max-http-form-post-size` defaults to 2MB).

### Time/clock assumptions

- Fineract relies on the JVM system clock for business-date logic, COB scheduling, and audit timestamps.
- If the system clock is manipulated, loan interest accrual and COB batch behavior may be incorrect. This is treated as an environmental issue, not a Fineract bug.

### Filesystem and network

- The process reads `application.properties` (or env vars) at startup.
- The process writes logs to `build/fineract.log` or stdout, depending on `logback-spring.xml`.

---

## §5a Build-time and configuration variants

Fineract is a family of binaries/deployment modes determined by configuration flags. The model below describes the default production posture unless noted.

| Knob | Default | Effect on security model |
|------|---------|--------------------------|
| `fineract.security.basicauth.enabled` | `true` | HTTP Basic Auth is the default scheme. |
| `fineract.security.oauth.enabled` | `false` | OAuth2/JWT is available but not default. Enabling it changes the auth boundary; see §6. |
| `fineract.security.2fa.enabled` | `false` | 2FA (SMS/Email OTP) is available but not default. Requires email/SMS gateway config. |
| `FINERACT_SERVER_SSL_ENABLED` | `true` | SSL/TLS is enforced; HTTP is not served. The project explicitly discourages setting this to false in production. |
| `FINERACT_SERVER_SSL_KEY_STORE` | `classpath:keystore.jks` | Self-signed dev cert embedded. Production must replace with CA-trusted cert or use a reverse proxy. |
| `fineract.mode.read-enabled` | `true` | Instance accepts read requests. Can be disabled to create a write-only or batch-only instance. |
| `fineract.mode.write-enabled` | `true` | Instance accepts write requests. Can be disabled to create a read-only instance. |
| `fineract.mode.batch-manager-enabled` | `true` | Instance can act as batch manager. In large deployments, batch managers should be segregated. |
| `fineract.mode.batch-worker-enabled` | `true` | Instance can act as batch worker. Workers should be segregated in production. |
| `FINERACT_REMOTE_JOB_MESSAGE_HANDLER_JMS_ENABLED` | `false` | ActiveMQ for batch partitioning is off. When enabled, the trust boundary expands to include the broker. |
| `FINERACT_REMOTE_JOB_MESSAGE_HANDLER_SPRING_EVENTS_ENABLED` | `true` | In-process Spring events for batch partitioning. Safe only when workers are in the same JVM/process. |
| `FINERACT_REMOTE_JOB_MESSAGE_HANDLER_KAFKA_ENABLED` | `false` | Kafka for batch partitioning is off. When enabled, the trust boundary expands to include Kafka. |
| `spring.liquibase.enabled` | `true` | Database migrations run on startup. In production, migrations should be run separately by an operator. |
| `server.tomcat.accesslog.enabled` | `false` | Tomcat access logging is off. Operators may enable for audit. |

**The insecure-default case:** A report against the embedded self-signed keystore in production is `OUT-OF-MODEL: non-default-build` because operators are documented as required to replace it.

---

## §6 Assumptions about inputs

### Input sources

Fineract accepts inputs via:

1. HTTP request bodies (JSON) to REST endpoints.
2. HTTP query parameters (`sqlSearch`, `orderBy`, `sortOrder`, `limit`, `offset`, etc.).
3. HTTP headers (`Fineract-Platform-TenantId`, `Authorization`, `Idempotency-Key`, etc.).
4. Database rows (tenant configuration, existing client/loan data) during batch processing.
5. Message broker messages (batch partitioning, external event subscriptions) when enabled.

### Key attacker-controllable parameters

| Endpoint family | Parameter | Notes |
|-----------------|-----------|-------|
| `/api/v1/clients` | `sqlSearch` | Historically a major SQL injection vector (CVE-2017-5663, CVE-2024-32838, etc.); SQL Validator now enforced. |
| `/api/v1/clients` | `orderBy`, `sortOrder` | SQL injection vectors (CVE-2018-1289, CVE-2018-1291); sanitized in current versions. |
| `/api/v1/loans` | `sqlSearch`, `orderBy`, `sortOrder` | Same as above. |
| `/api/v1/centers`, `/api/v1/groups`, `/api/v1/staff` | `sqlSearch` | CVE-2017-5663; SQL Validator enforced. |
| `/api/v1/reports` | `reportName` | SQL injection vector (CVE-2018-1292); parameterized. |
| `/api/v1/fieldconfiguration` | `validation_regex` | Admin-configured only; admin must validate regex safety. |
| `/api/v1/users` | `password` (body) | Weak password policy was CVE-2025-23408; now enforced server-side. |
| All endpoints | `Fineract-Platform-TenantId` (header) | Must resolve to a known tenant; enforced by TenantDetailsService. |
| All endpoints | `Authorization` | Authentication enforced by Spring Security. |

### Size and rate

- **HTTP POST body size:** Bounded by Tomcat `max-http-form-post-size` (default 2MB).
- **Rate limiting:** Not enforced by Fineract itself. Left to reverse proxy / API gateway.

---

## §7 Adversary model

### Who is in scope

| Adversary | Capability | What they are trying to do |
|-----------|------------|---------------------------|
| Network-based attacker (unauthenticated) | Can send HTTP requests to the API; can observe TLS-encrypted traffic but not decrypt it without cert compromise. | Gain authentication, extract data via injection, cause DoS via resource exhaustion. |
| Authenticated back-office user (low-privilege) | Has valid tenant credentials with limited permissions (e.g., loan officer). | Escalate privileges, access other tenants' data, modify loans/disbursements they do not own. |
| Authenticated back-office user (high-privilege) | Has admin / super-user role. | Arbitrary data modification, user management, configuration changes. |
| Compromised message broker peer | Can read/write to Kafka/ActiveMQ topics used for batch partitioning or external events. | Inject malicious batch partitions, intercept business events, cause COB corruption. |

### Who is explicitly out of scope

- **Physical attacker with datacenter access:** If the attacker has root on the DB server or the Fineract host, the model assumes they have already won.
- **JVM / Spring Boot supply-chain attacker:** The model assumes the JVM, Spring Boot, and PostgreSQL/MariaDB driver are not compromised at the binary level.
- **Side-channel observer:** Timing attacks and cache attacks are not modeled.
- **Reverse proxy / WAF bypass attacker:** If the reverse proxy is compromised, the attacker is out of scope for Fineract's model; that is infrastructure-layer.

---

## §8 Security properties the project provides

### Injection defenses

| Property | Conditions | Violation symptom | Severity |
|----------|------------|-------------------|----------|
| No SQL injection on `sqlSearch`, `orderBy`, `sortOrder` in core API endpoints | Valid authenticated request; SQL Validator enabled (default since 1.10.1). | Database error, unauthorized data extraction, privilege escalation. | Critical |
| No path traversal in file upload | Valid authenticated request; file upload component validates path. | Arbitrary file write, RCE (CVE-2022-44635). | Critical |
| No SSRF via report or webhook features | Valid authenticated request; outbound URLs are not attacker-controlled. | Server makes unexpected outbound connections, internal port scanning. | Critical |

### Authentication / authorization

| Property | Conditions | Violation symptom | Severity |
|----------|------------|-------------------|----------|
| Tenant isolation — users and data of tenant A are inaccessible from tenant B | Valid request with `Fineract-Platform-TenantId` header; tenant resolution is correct. | Data from tenant B visible in tenant A's response. | Critical |
| Role-based access control (RBAC) enforcement — low-privilege users cannot perform admin actions | Valid authenticated request; Spring Security filters are active. | Loan officer can create users, modify GL entries, or escalate to super-user (CVE-2024-23537). | Critical |
| Password strength enforcement | User creation/reset endpoint; password policy at default or stricter. | Weak passwords accepted, brute-forceable accounts (CVE-2025-23408). | High |
| OAuth2 JWT issuer validation | OAuth mode enabled; issuer URI set correctly. | Token from attacker-controlled issuer accepted as valid. | Critical |
| 2FA OTP validation | 2FA enabled; OTP gateway configured. | OTP bypass, replay, or prediction. | High |

### Data integrity

| Property | Conditions | Violation symptom | Severity |
|----------|------------|-------------------|----------|
| Double-entry accounting integrity — every debit has a matching credit | COB batch completes successfully; no manual GL manipulation bypassing business rules. | Unbalanced journal entries, incorrect financial statements. | High |

### Confidentiality

| Property | Conditions | Violation symptom | Severity |
|----------|------------|-------------------|----------|
| TLS encryption on all API traffic | `FINERACT_SERVER_SSL_ENABLED=true` (default); valid cert or reverse proxy terminates TLS. | Credentials or PII transmitted in plaintext; MITM attack. | Critical |
| Tenant database credentials encrypted at rest in tenant config | `fineract.tenant.master-password` is set; master password is set. | Plaintext DB credentials in `tenant_server_connections` table. | High |

---

## §9 Security properties the project does not provide

### Properties explicitly disclaimed

| Property | Why it is not provided | What to do instead |
|----------|------------------------|-------------------|
| Rate limiting / brute-force protection | No built-in rate limiting on login, API calls, or batch job submission. | Deploy an API gateway or WAF (e.g., NGINX `limit_req`, cloud WAF) in front of Fineract. |
| Input validation for all free-text fields | Some fields accept arbitrary text that is stored and later rendered; XSS filtering is not guaranteed. | Sanitize output in the frontend (Community App or custom UI). |
| Audit log tamper-resistance | Audit logs are written to the database and filesystem; no append-only or cryptographic integrity guarantee. | Export logs to a SIEM or write-once storage. |
| Backup encryption | The project does not manage database backups. | Encrypt backups at the database or storage layer. |
| Real-time fraud detection | No anomaly detection on transactions, logins, or batch jobs. | Deploy a separate fraud detection layer. |

### False-friend properties

| Feature | What it looks like | What it actually is |
|---------|-------------------|---------------------|
| Tenant database password encryption (AES/CBC/PKCS5Padding) | Looks like the tenant DB password is securely encrypted. | It is symmetrically encrypted with a master password stored in the same database. If the `fineract_tenants` DB is compromised, the master password reveals all tenant credentials. Do not treat this as a security boundary against a DBA or DB server attacker. |
| SQL Validator | Looks like a guarantee against all SQL injection. | It is a configurable series of checks. It protects against nearly all potential SQL injection attacks, but novel injection patterns may bypass it. Do not expose `sqlSearch` directly to untrusted network peers without additional WAF rules. |
| Embedded SSL keystore | Looks like Fineract "supports SSL out of the box." | The embedded `keystore.jks` is self-signed, untrusted by browsers, and intended for localhost development only. Do not use in production. |
| Basic Auth default | Looks like a simple, secure default. | HTTP Basic Auth transmits credentials on every request. Without TLS, it is plaintext. With TLS, it is still vulnerable to brute-force if no rate limiting is deployed downstream. |

### Known attack classes this project cannot defend against

- **SQL injection in custom reports / ad-hoc queries:** Fineract allows administrators to define custom reports with SQL. If an attacker gains admin access, they can execute arbitrary SQL through the reporting module. This is by design (admin capability) and not a vulnerability.

---

## §10 Downstream responsibilities

What the operator / deployer must do for the assumptions in §5–§7 to hold:

1. **Replace the embedded SSL keystore** with a CA-trusted certificate or terminate TLS at a reverse proxy before exposing the service to any network.
2. **Deploy a reverse proxy / WAF / API gateway** in front of Fineract to enforce rate limiting, IP filtering, and DDoS mitigation. The project explicitly recommends not running directly on the internet.
3. **Configure a strong `fineract.tenant.master-password`** and rotate it on a schedule. This password encrypts all tenant DB credentials; its compromise is equivalent to compromising all tenants.
4. **Run database migrations (Liquibase) separately** from the application server in production, or ensure the app server has migration-only privileges that are dropped after startup.
5. **Enable and configure OAuth2** (instead of Basic Auth) for production deployments where the API is exposed beyond a trusted internal network.
6. **Enable 2FA** (`FINERACT_SECURITY_2FA_ENABLED=true`) and configure a secure SMS/email gateway for production admin accounts.
7. **Segregate instance types** in large deployments: run batch managers and workers on separate nodes from API-serving nodes; use read-only instances for reporting.
8. **Encrypt database backups** at the storage layer. Fineract does not manage backups.

---

## §11 Known misuse patterns

1. **Exposing the API directly to the internet without a reverse proxy.** The embedded Tomcat is not hardened for direct internet exposure; the self-signed cert is not trusted; and there is no built-in rate limiting.

2. **Using the embedded SSL keystore in production.** The `keystore.jks` is self-signed and the private key is public (shipped in the JAR). This leaves users vulnerable to MITM attacks.

3. **Enabling both Basic Auth and OAuth simultaneously.** The project checks this on startup and fails, but misconfigured environment variables can cause startup loops or fallback to Basic Auth in containerized deployments.

4. **Running batch jobs on the same instance as API requests in large deployments.** The COB batch can consume all CPU/memory, causing API unavailability. The model supports segregated instance types, but operators often ignore this.

5. **Using the `sqlSearch` parameter to build "custom reports" via the API.** Exposing `sqlSearch` to low-trust users creates a recurring SQL injection risk even with the SQL Validator. The validator is a defense-in-depth layer, not a guarantee.

6. **Storing the `fineract.tenant.master-password` in plaintext in environment variables or config maps.** If the orchestration layer is compromised, all tenant DB passwords are recoverable.

---

## §11a Known non-findings (recurring false positives)

| What the tool reports | Why it is safe under the model |
|-----------------------|-------------------------------|
| "Self-signed certificate in `keystore.jks`" | The embedded keystore is dev-only per §5a and §9. A production report against it is `OUT-OF-MODEL: non-default-build`. |
| "Hardcoded database password in `application.properties`" | The shipped `application.properties` contains placeholder/default values. The project warns: "Never commit application.properties with credentials to version control." Production deployments must override via env vars. |
| "Tomcat access log disabled (`server.tomcat.accesslog.enabled=false`)" | Access logging is an operational choice, not a security vulnerability. The project does not claim audit logging as a §8 property. |
| "Missing HttpOnly / Secure flags on cookies" | Fineract uses stateless HTTP Basic or OAuth2 JWT authentication; it does not rely on session cookies for security. |
| "Unchecked malloc / memory allocation" | Fineract is a Java/Spring Boot application; memory allocation is managed by the JVM. Reports of unchecked C-style allocations are false positives from generic scanners. |
| "SQL injection in `sqlSearch` — generic pattern match" | The SQL Validator (introduced in 1.10.1) is a claimed defense. A generic regex match that does not account for the validator is a false positive. Verify against the actual validator logic before reporting. |
| "Path traversal in file upload — old CVE pattern" | CVE-2022-44635 was fixed in 1.8.1. Scanners flagging the old pattern against current HEAD are false positives. |
| "Weak password policy — default allows short passwords" | CVE-2025-23408 was fixed in 1.11.0. Current versions enforce stronger policies. |
| "Privilege escalation — user can modify own role" | CVE-2024-23537 was fixed in 1.9.0. Current versions enforce RBAC checks. |
| "SSRF via report URL" | CVE-2023-25195 was fixed in 1.8.4/1.7.3. Current versions restrict outbound URLs. |

---

## §12 Conditions that would change this model

The following changes should trigger a revision of this threat model:

1. New public API surface (e.g., a new `/api/v3/...` major version, GraphQL endpoint, or gRPC service).
2. New input format accepted (e.g., XML batch imports, protobuf, Avro).
3. New network surface (e.g., native Kafka consumer instead of just producer, WebSocket support, gRPC).
4. New deployment context (e.g., serverless/FaaS, edge computing, mobile SDK embedding).
5. New authentication scheme (e.g., mTLS for clients, SAML, LDAP integration).
6. Promotion of a 3rd party component into core (e.g., if the self-service plugin moves from openMF/ into apache/fineract proper).
7. Change in default for a §5a build knob that changes the security envelope (e.g., OAuth2 becoming the default instead of Basic Auth).
8. New CVE that cannot be cleanly routed to one of the §13 dispositions — this indicates a `MODEL-GAP` and requires model revision, not an ad-hoc call.

---

## §13 Triage dispositions

| Disposition | Meaning | Licensed by |
|-------------|---------|-------------|
| `VALID` | Violates a property the project claims, via an in-scope adversary and input. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property is violated, but the API makes a §11 misuse easy enough that the project elects to harden it. Reported privately; fixed at maintainer discretion; typically no CVE. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of a parameter the model marks trusted (e.g., admin-defined `validation_regex`). | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires an attacker capability the model excludes (e.g., physical datacenter access, JVM compromise). | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in `kubernetes/`, `docker-compose*.yml`, `openMF/selfservice-plugin` (external repo), or other code placed out of scope. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only manifests under a discouraged or non-default §5a flag (e.g., `FINERACT_SERVER_SSL_ENABLED=false` in production, or embedded keystore used in production). | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a property the project explicitly does not provide (e.g., rate limiting, backup encryption). | §9 |
| `KNOWN-NON-FINDING` | Matches a documented recurring false positive from §11a. | §11a |
| `MODEL-GAP` | Cannot be cleanly routed to any of the above. Triggers §12 revision. | §12 |

---

## §15 Optional: machine-readable companion

A sidecar `threat-model.yaml` is recommended for automated triage pipelines. The prose document remains canonical; the sidecar is a derived index.
