# Apache Fineract Threat Model

## §1 Header

- **Project:** Apache Fineract
- **Version:** 1.15.0-SNAPSHOT (HEAD as of 2026-06-10)
- **Date:** 2026-06-10
- **Author(s):** Threat Model Producer (AI-assisted)
- **Status:** Draft — under maintainer review
- **Version binding:** This model is written against Fineract develop branch circa 1.12.x. A report against release N is triaged against the model as it stood at N, not at HEAD.
- **Reporting cross-reference:** Findings that fall under §8 (claimed properties) should be reported privately to security@fineract.apache.org per the project's SECURITY.md; findings that fall under §3 or §9 will be closed citing this document.

### Provenance legend

- **(documented)** — Stated in the project's own docs (README, application.properties, fineract.apache.org/security.html, API docs, header comments). Cited inline.
- **(maintainer)** — Stated by a maintainer in response to a question from this process. (None yet in this draft.)
- **(inferred)** — Reasoned from code structure, CVE history, absence of a feature, or general domain knowledge — not yet confirmed. Must have a matching entry in §14.

**Draft confidence:** 18 documented / 0 maintainer / 42 inferred

**Project purpose:** Apache Fineract is an open-source core banking platform designed for microfinance institutions, fintechs, and digital lenders. It provides a REST API layer for back-office operations (loan origination, client onboarding, savings management, accounting) and, via plugins, self-service customer-facing banking. It is a Spring Boot application with a multi-tenant database model (PostgreSQL or MariaDB), optional message brokers (Kafka/ActiveMQ) for events and batch partitioning, and supports multiple authentication schemes (HTTP Basic, OAuth2, 2FA). _(documented)_

---

## §2 Scope and intended use

### Primary intended use cases

- **Back-office core banking:** Loan officers, tellers, and administrators manage clients, loans, savings, deposits, and accounting via REST APIs.
- **Batch end-of-day processing:** Close-of-Business (COB) jobs run via Spring Batch, optionally partitioned across worker nodes.
- **External business events:** Publishing events (e.g., ClientCreated) to downstream systems via Kafka or ActiveMQ.
- **Self-service banking (via plugin):** Customer-facing mobile/web backends for account viewing, transfers, and loan applications. _(documented: openMF/selfservice-plugin README)_

### Deployment contexts

- **Server / daemon:** Spring Boot executable JAR with embedded Tomcat (default), or WAR deployed to external Tomcat.
- **Containerized:** Docker Compose and Kubernetes manifests ship in the repository. _(documented: README, kubernetes/ directory)_
- **Multi-instance topology:** Read-only instances, write-enabled instances, batch-manager instances, and batch-worker instances can be segregated. _(documented: fineract.apache.org/docs)_
- **Not an in-process library:** Fineract is a standalone service; it is not designed to be embedded as a library inside another JVM application. _(documented)_

### Caller expectations and trust roles

For a network service, the "caller" splits into distinct roles:

| Role | Trust level | Description |
|------|-------------|-------------|
| Back-office user | Authenticated, authorized | Loan officers, branch managers, system admins. Hold tenant-scoped credentials. |
| Self-service customer | Authenticated, limited | End customers using `/v1/self/...` APIs. Lower privilege than back-office. _(documented: selfservice-plugin)_ |
| Operator / deployer | Trusted for the instance | Sysadmin who configures application.properties, SSL, DB credentials, OAuth issuer. |
| Peer / message broker | Authenticated but adversarial | Kafka/ActiveMQ nodes or other Fineract instances in a partitioned batch topology. |
| External event consumer | Authenticated | Downstream systems reading business events from Kafka/ActiveMQ topics. |

### Component-family table

| Family | Representative API / Entry Point | Touches outside process? | In model? |
|--------|-------------------------------|------------------------|-----------|
| Core REST | `/api/v1/clients`, `/api/v1/loans` | Network (HTTP/S), Database | Yes |
| Self-Service API | `/v1/self/...` (plugin) | Network (HTTP/S), Database | Yes |
| Batch Engine (COB) | Spring Batch job launcher, partitioned workers | Database, Message broker (optional) | Yes |
| Event Publisher | ClientCreated, LoanDisbursed to Kafka/ActiveMQ | Message broker | Yes |
| Actuator / Health | `/actuator/health`, `/actuator/info` | Network (HTTP/S) | Yes |
| Community Web App | (separate repo) | Network (HTTP/S) | No — separate project; out of scope per §3 |
| Kubernetes manifests | `kubernetes/` directory | Infrastructure orchestration | No — artifact, not runtime code; out of scope per §3 |
| Docker files | `docker-compose*.yml` | Container orchestration | No — artifact; out of scope per §3 |
| Integration tests | `oauth2-tests/`, `fineract-e2e` | Test harnesses | No — test-only; out of scope |

---

## §3 Out of scope (explicit non-goals)

### Use cases not supported

- **Direct internet-facing deployment without a reverse proxy / WAF:** The project documentation explicitly recommends running behind NGINX or a cloud-native load balancer with SSL termination. _(documented: fineract.apache.org/docs)_
- **Cryptocurrency or blockchain ledger integration:** Fineract is a traditional double-entry accounting system; it does not provide blockchain primitives.
- **Real-time payment rails (RTGS, SWIFT):** Fineract manages internal accounts; integration with external payment networks is a downstream responsibility.
- **Customer-facing UI:** The "Community App" is a separate repository (openMF/community-app) and is not part of this model.

### Threats the project does not attempt to defend against

- **Physical access to the database server:** If an attacker has OS-level access to the PostgreSQL/MariaDB host, the project assumes the game is already lost. _(inferred)_
- **Side-channel attacks (timing, power, cache):** No constant-time guarantees are made for any cryptographic or comparison operations. _(inferred)_
- **Network-level DDoS:** The embedded Tomcat has configurable connection limits, but volumetric DDoS mitigation is explicitly left to the reverse proxy / cloud provider. _(documented: fineract.apache.org/docs)_
- **Supply-chain compromise of the JVM or Spring Boot:** The project does not validate the runtime integrity of the JVM, container base image, or Gradle wrapper. _(inferred)_

### Code shipped but not covered

| Code | Policy | Reason |
|------|--------|--------|
| `kubernetes/` | Out of scope | Deployment orchestration; separately authored; threat-model separately as infrastructure. |
| `docker-compose*.yml` | Out of scope | Deployment orchestration; not runtime code. |
| `fineract-e2e/`, `oauth2-tests/` | Out of scope | Test-only code; not shipped in production artifacts. |
| `selfservice-plugin` (external repo) | Out of scope | Separate repository (openMF/selfservice-plugin); maintained independently; threat-model separately. |
| `examples/`, `contrib/` | Out of scope | Not present in current repo, but if added, default to out-of-scope. |

---

## §4 Trust boundaries and data flow

### Where the trust boundary sits

The **API surface (HTTP/S)** is the primary trust boundary. Once a request has passed authentication and tenant resolution, data inside the Spring Boot process is treated as authenticated-by-virtue-of-the-caller. _(documented: Spring Security configuration)_

The **database connection pool (HikariCP)** is a secondary trust boundary: SQL queries generated by the application are trusted by the database; the DB does not re-validate parameterization. _(inferred)_

The **message broker (Kafka/ActiveMQ)** is a tertiary trust boundary when used for batch partitioning: messages between manager and worker nodes are trusted once they enter the broker. _(documented: docker-compose configs)_

### Data flow and trust transitions (ArchiMate Data Flow View)

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

1. **Internet → Reverse Proxy:** TLS-encrypted; certificate validation required. The project ships a self-signed cert for localhost only; production requires a CA-trusted cert or managed proxy. _(documented)_
2. **Reverse Proxy → Fineract API:** TLS-encrypted; X-Forwarded-For and X-Forwarded-Proto respected if proxy is present. _(documented: FINERACT-914 fix notes)_
3. **Fineract API → Database:** JDBC over TCP; credentials from application.properties / env vars; connection pool authenticated per-tenant. _(documented: application.properties)_
4. **Fineract API → Message Broker:** Kafka (PLAINTEXT or SASL/SSL) or ActiveMQ (OpenWire); authentication depends on broker configuration. _(documented: docker-compose configs)_

### Reachability preconditions per component family

| Component family | Reachability precondition |
|------------------|--------------------------|
| Core REST API | Finding is in-model only if reachable from an authenticated HTTP request to `/api/v1/...` or `/api/v2/...` with a valid `Fineract-Platform-TenantId` header |
| Self-Service API | Finding is in-model only if reachable from an authenticated HTTP request to `/v1/self/...` via the self-service plugin. (Note: plugin is out of model per §3, but if the vulnerability is in Fineract core code invoked by the plugin, it is in-model.) |
| Batch Engine | Finding is in-model only if reachable from a batch job launch (manager) or partitioned step execution (worker). Batch jobs are reachable only from authenticated back-office users or scheduled triggers. |
| Event Publisher | Finding is in-model only if reachable from an internal business event (e.g., ClientCreated) that flows to the broker. The broker itself is out of model per §3, but the producer-side serialization is in-model. |
| Actuator | Finding is in-model only if reachable from `/actuator/health` or `/actuator/info`. These are unauthenticated by default in Spring Boot; Fineract's posture on actuator security is _(inferred)_ — see §14. |

---

## §5 Assumptions about the environment

### Operating system, runtime, hardware

- **Java 21+** (Azul Zulu recommended). _(documented: README)_
- **PostgreSQL >= 18.0** or MariaDB/MySQL as the relational database. _(documented: README, docs)_
- **Spring Boot 3.x** with embedded Tomcat. _(documented: build.gradle)_
- The JVM provides standard memory safety, garbage collection, and thread scheduling. _(inferred)_

### Concurrency assumptions

- Fineract uses Spring's transaction management with `@Transactional`. Database isolation is delegated to the underlying RDBMS. _(documented: source code patterns)_
- Multi-tenancy is implemented via schema-per-tenant or database-per-tenant; tenant resolution happens at request entry via `Fineract-Platform-TenantId` header. _(documented: fineract.apache.org/docs)_
- Spring Batch remote partitioning uses a manager-worker model; workers are stateless and idempotent. _(documented: docs)_

### Memory model assumptions

- Standard JVM heap behavior; no `sun.misc.Unsafe` usage for security-critical operations. _(inferred)_
- Input size bounds are enforced at the API layer (Tomcat `max-http-form-post-size` defaults to 2MB). _(documented)_

### Time/clock assumptions

- Fineract relies on the JVM system clock for business-date logic, COB scheduling, and audit timestamps. No NTP client is embedded. _(inferred)_
- **Condition:** If the system clock jumps backwards or is manipulated, loan interest accrual and COB batch behavior may be incorrect. This is treated as an environmental issue, not a Fineract bug. _(inferred)_

### Filesystem, network, peripheral assumptions

- The process reads `application.properties` (or env vars) at startup. _(documented: Spring Boot docs)_
- The process writes logs to `build/fineract.log` or stdout, depending on `logback-spring.xml`. _(documented: dev guide)_
- The process does not spawn child processes, open raw sockets, or install signal handlers. _(inferred)_
- The process does not read arbitrary environment variables beyond the `FINERACT_*` and `SPRING_*` prefixes used for configuration. _(inferred)_
- The process does not mutate global locale or FPU state. _(inferred)_

### What the project does not do to its host (negative claims)

| Action | Does it? | Confidence |
|--------|----------|------------|
| Spawn child processes | No | _(documented)_ |
| Open listening sockets beyond the configured HTTP(S) port | No | _(inferred)_ |
| Install signal handlers | No | _(inferred)_ |
| Read arbitrary environment variables | No — only `FINERACT_*`, `SPRING_*` | _(inferred)_ |
| Write to stdout/stderr | Yes — logs | _(documented)_ |
| Touch global locale | No | _(inferred)_ |
| Mutate process-wide state outside its own Spring context | No | _(inferred)_ |

---

## §5a Build-time and configuration variants

Fineract is a family of binaries/deployment modes determined by configuration flags. The model below describes the default production posture unless noted.

| Knob | Default | Effect on security model | Maintainer stance |
|------|---------|--------------------------|-------------------|
| `fineract.security.basicauth.enabled` | `true` | HTTP Basic Auth is the default scheme. | Supported production posture. _(documented)_ |
| `fineract.security.oauth.enabled` | `false` | OAuth2/JWT is available but not default. | Supported production posture. Enabling it changes the auth boundary; see §6. _(documented)_ |
| `fineract.security.2fa.enabled` | `false` | 2FA (SMS/Email OTP) is available but not default. | Supported production posture. Requires email/SMS gateway config. _(documented)_ |
| `FINERACT_SERVER_SSL_ENABLED` | `true` | SSL/TLS is enforced; HTTP is not served. | Supported production posture. The project explicitly discourages setting this to false in production. _(documented)_ |
| `FINERACT_SERVER_SSL_KEY_STORE` | `classpath:keystore.jks` | Self-signed dev cert embedded. | Dev-only. Production must replace with CA-trusted cert or use a reverse proxy. _(documented)_ |
| `fineract.mode.read-enabled` | `true` | Instance accepts read requests. | Supported. Can be disabled to create a write-only or batch-only instance. _(documented)_ |
| `fineract.mode.write-enabled` | `true` | Instance accepts write requests. | Supported. Can be disabled to create a read-only instance. _(documented)_ |
| `fineract.mode.batch-manager-enabled` | `true` | Instance can act as batch manager. | Supported. In large deployments, batch managers should be segregated. _(documented)_ |
| `fineract.mode.batch-worker-enabled` | `true` | Instance can act as batch worker. | Supported. Workers should be segregated in production. _(documented)_ |
| `FINERACT_REMOTE_JOB_MESSAGE_HANDLER_JMS_ENABLED` | `false` | ActiveMQ for batch partitioning is off. | Supported when enabled. Changes the trust boundary to include the broker. _(documented)_ |
| `FINERACT_REMOTE_JOB_MESSAGE_HANDLER_SPRING_EVENTS_ENABLED` | `true` | In-process Spring events for batch partitioning. | Default for single-node deployments. Safe only when workers are in the same JVM/process. _(documented)_ |
| `FINERACT_REMOTE_JOB_MESSAGE_HANDLER_KAFKA_ENABLED` | `false` | Kafka for batch partitioning is off. | Supported when enabled. Changes the trust boundary to include Kafka. _(documented)_ |
| `spring.liquibase.enabled` | `true` | Database migrations run on startup. | Supported. In production, migrations should be run separately by an operator, not by the app server. _(inferred)_ |
| `server.tomcat.accesslog.enabled` | `false` | Tomcat access logging is off. | Supported. Operators may enable for audit. _(documented)_ |

**The insecure-default case:** None of the above defaults void a §8 property in a way that would make a report VALID against default settings. However, `FINERACT_SERVER_SSL_KEY_STORE` using the embedded self-signed keystore is a dev-convenience; a report against it in production is `OUT-OF-MODEL: non-default-build` because operators are documented as required to replace it. _(documented: fineract.apache.org/docs)_

---

## §6 Assumptions about inputs

### Input sources

Fineract accepts inputs via:

1. HTTP request bodies (JSON) to REST endpoints.
2. HTTP query parameters (`sqlSearch`, `orderBy`, `sortOrder`, `limit`, `offset`, etc.).
3. HTTP headers (`Fineract-Platform-TenantId`, `Authorization`, `Idempotency-Key`, etc.).
4. Database rows (tenant configuration, existing client/loan data) during batch processing.
5. Message broker messages (batch partitioning, external event subscriptions) when enabled.

### Per-parameter trust table (Core REST API)

| Endpoint family | Parameter | Attacker-controllable? | Caller must enforce |
|-----------------|-----------|------------------------|---------------------|
| `/api/v1/clients` | `sqlSearch` | Yes — historically a major SQLi vector (CVE-2017-5663, CVE-2024-32838, etc.) | SQL Validator now enforced _(documented)_ |
| `/api/v1/clients` | `orderBy` | Yes — SQLi vector (CVE-2018-1289, CVE-2018-1291) | Sanitized in current versions _(documented)_ |
| `/api/v1/clients` | `sortOrder` | Yes — SQLi vector (CVE-2018-1289) | Sanitized _(documented)_ |
| `/api/v1/loans` | `sqlSearch` | Yes | SQL Validator _(documented)_ |
| `/api/v1/loans` | `orderBy` | Yes | Sanitized _(documented)_ |
| `/api/v1/loans` | `sortOrder` | Yes | Sanitized _(documented)_ |
| `/api/v1/centers` | `sqlSearch` | Yes — CVE-2017-5663 | SQL Validator _(documented)_ |
| `/api/v1/groups` | `sqlSearch` | Yes — CVE-2017-5663 | SQL Validator _(documented)_ |
| `/api/v1/staff` | `sqlSearch` | Yes — CVE-2017-5663 | SQL Validator _(documented)_ |
| `/api/v1/reports` | `reportName` | Yes — SQLi vector (CVE-2018-1292) | Parameterized _(documented)_ |
| `/api/v1/fieldconfiguration` | `validation_regex` | No — admin-configured | Admin must validate regex safety |
| `/api/v1/clients` | `clientId` (path) | Yes — must resolve to a valid ID | Access control enforced by Fineract |
| `/api/v1/loans` | `loanId` (path) | Yes | Access control enforced by Fineract |
| `/api/v1/accounttransfers` | `fromAccountId`, `toAccountId` | Yes | Access control + balance validation enforced |
| `/api/v1/users` | `password` (body) | Yes | Weak password policy was a CVE (CVE-2025-23408); now enforced server-side |
| `/api/v1/users` | `repeatPassword` | Yes | Must match password |
| `/api/v1/loans` | `principal` | Yes | Business-rule validation enforced |
| `/api/v1/savingsaccounts` | `transactionAmount` | Yes | Business-rule validation enforced |
| All endpoints | `Fineract-Platform-TenantId` (header) | Yes — but must resolve to a known tenant | Tenant resolution enforced by TenantDetailsService |
| All endpoints | `Authorization` (Basic/OAuth token) | Yes | Authentication enforced by Spring Security |

### Per-parameter trust table (Self-Service API — via plugin)

| Endpoint family | Parameter | Attacker-controllable? | Caller must enforce |
|-----------------|-----------|------------------------|---------------------|
| `/v1/self/registration` | `mobileNo`, `email` | Yes | Format validation; uniqueness check |
| `/v1/self/authentication` | `username`, `password` | Yes | Auth enforced by core; rate limiting is downstream responsibility _(inferred)_ |
| `/v1/self/accounttransfers` | `fromAccountId`, `toAccountId`, `transferAmount` | Yes | Account ownership + balance validation enforced by core |
| `/v1/self/loans` | `productId`, `principal` | Yes | Product eligibility + limit checks enforced |

### Size, shape, and rate assumptions

- **HTTP POST body size:** Bounded by Tomcat `max-http-form-post-size` (default 2MB). _(documented: application.properties)_
- **JSON payload depth:** Bounded by Spring Boot Jackson defaults (no explicit custom depth limit documented). _(inferred)_
- **Rate limiting:** Not enforced by Fineract itself. Left to reverse proxy / API gateway. _(documented: docs recommend NGINX/cloud proxy)_
- **Streaming inputs:** Fineract does not accept streaming/chunked inputs for API requests; it expects complete JSON bodies. _(inferred)_

---

## §7 Adversary model

### Who is in scope

| Adversary | Capability | What they are trying to do | OpenFAIR TEF (annual) |
|-----------|------------|---------------------------|----------------------|
| Network-based attacker (unauthenticated) | Can send HTTP requests to the API; can observe TLS-encrypted traffic but not decrypt it without cert compromise. | Gain authentication, extract data via injection, cause DoS via resource exhaustion. | High (automated scanning) |
| Authenticated back-office user (low-privilege) | Has valid tenant credentials with limited permissions (e.g., loan officer). | Escalate privileges, access other tenants' data, modify loans/disbursements they do not own. | Medium |
| Authenticated back-office user (high-privilege) | Has admin / super-user role. | Arbitrary data modification, user management, configuration changes. | Low (but high impact) |
| Self-service customer | Has valid customer credentials via self-service plugin. | Access other customers' accounts, escalate to back-office privileges, manipulate transfers. | Medium |
| Compromised message broker peer | Can read/write to Kafka/ActiveMQ topics used for batch partitioning or external events. | Inject malicious batch partitions, intercept business events, cause COB corruption. | Low (when broker is enabled) |

### Who is explicitly out of scope

- **Physical attacker with datacenter access:** If the attacker has root on the DB server or the Fineract host, the model assumes they have already won. _(inferred)_
- **JVM / Spring Boot supply-chain attacker:** The model assumes the JVM, Spring Boot, and PostgreSQL/MariaDB driver are not compromised at the binary level. _(inferred)_
- **Side-channel observer:** Timing attacks, cache attacks, and power analysis are not modeled. _(inferred)_
- **Reverse proxy / WAF bypass attacker:** If the reverse proxy is compromised, the attacker is out of scope for Fineract's model; that is infrastructure-layer. _(inferred)_

### Byzantine / authenticated-but-adversarial participant

In a multi-instance deployment with batch partitioning, a compromised batch worker is an authenticated peer that behaves arbitrarily. The model assumes:

- The batch manager trusts the worker to execute partition steps idempotently.
- The worker does not have direct database access beyond the JDBC connection pool (which is tenant-scoped).
- A malicious worker could report false completion status, causing COB inconsistency. _(inferred)_
- **Honest-fraction threshold:** The model assumes >50% of workers are honest; if a majority of workers are compromised, COB integrity is not guaranteed. _(inferred)_

---

## §8 Security properties the project provides

For each property: what it is, conditions, violation symptom, severity tier, and provenance.

### Memory / safety properties

| Property | Conditions | Violation symptom | Severity | Provenance |
|----------|------------|-------------------|----------|------------|
| No SQL injection on `sqlSearch`, `orderBy`, `sortOrder` in core API endpoints | Valid authenticated request; SQL Validator enabled (default since 1.10.1); parameter is in the allow-list. | Database error, unauthorized data extraction, privilege escalation. | Critical | _(documented: CVE-2024-32838 fix, SQL Validator implementation)_ |
| No path traversal in file upload | Valid authenticated request; file upload component validates path. | Arbitrary file write, RCE (as seen in CVE-2022-44635). | Critical | _(documented: CVE-2022-44635 fix)_ |
| No SSRF via report or webhook features | Valid authenticated request; outbound URLs are not attacker-controlled. | Server makes unexpected outbound connections, internal port scanning. | Critical | _(documented: CVE-2023-25195 fix)_ |

### Authentication / authorization properties

| Property | Conditions | Violation symptom | Severity | Provenance |
|----------|------------|-------------------|----------|------------|
| Tenant isolation — users and data of tenant A are inaccessible from tenant B | Valid request with `Fineract-Platform-TenantId` header; tenant resolution is correct; no cross-tenant IDOR. | Data from tenant B visible in tenant A's response; user from tenant A can act on tenant B. | Critical | _(documented: multi-tenant architecture docs)_ |
| Role-based access control (RBAC) enforcement — low-privilege users cannot perform admin actions | Valid authenticated request; Spring Security filters are active; permissions are checked. | Loan officer can create users, modify GL entries, or escalate to super-user. | Critical | _(documented: CVE-2024-23537 fix — privilege escalation)_ |
| Password strength enforcement | User creation/reset endpoint; `fineract.security.password-policy` is at default or stricter. | Weak passwords accepted (e.g., "123456"), brute-forceable accounts. | High | _(documented: CVE-2025-23408 fix)_ |
| OAuth2 JWT issuer validation | OAuth mode enabled; `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set correctly. | Token from attacker-controlled issuer accepted as valid. | Critical | _(documented: OAuth config docs)_ |
| 2FA OTP validation | 2FA enabled; OTP gateway (SMS/email) configured. | OTP bypass, replay, or prediction. | High | _(documented: 2FA docs)_ |

### Data integrity / correctness properties

| Property | Conditions | Violation symptom | Severity | Provenance |
|----------|------------|-------------------|----------|------------|
| Double-entry accounting integrity — every debit has a matching credit | COB batch completes successfully; no manual GL manipulation bypassing business rules. | Unbalanced journal entries, incorrect financial statements. | High | _(inferred: core banking requirement)_ |
| Business date correctness — loan interest accrual advances correctly | No clock manipulation; COB runs in order. | Incorrect interest charged, double accrual, missed accrual. | High | _(inferred)_ |
| Idempotency of batch steps | Remote partitioning uses unique step identifiers; no duplicate worker execution. | Duplicate transactions, double disbursement, duplicate charges. | High | _(inferred)_ |

### Resource properties

| Property | Conditions | Violation symptom | Severity | Provenance |
|----------|------------|-------------------|----------|------------|
| Bounded HTTP request processing time | Request size <= 2MB; Tomcat thread pool <= 200 threads; no unbounded recursion in business rules. | Request hangs, thread pool exhaustion, OOM. | High | _(documented: Tomcat properties)_ |
| Bounded batch job memory | Input data volume within configured partition size; heap size within JVM `-Xmx`. | OOM during COB, batch job failure. | High | _(inferred)_ |

**Resource threshold clarification:** The project makes no categorical guarantee that CPU/memory is linear in input size. A request that triggers complex business rules (e.g., a loan with 1000 disbursement schedules) may consume super-linear CPU. A hang on pathological input is considered a bug if it can be triggered by a single authenticated request within the 2MB body limit. _(inferred)_

### Confidentiality / integrity / availability properties

| Property | Conditions | Violation symptom | Severity | Provenance |
|----------|------------|-------------------|----------|------------|
| TLS encryption on all API traffic | `FINERACT_SERVER_SSL_ENABLED=true` (default); valid cert or reverse proxy terminates TLS. | Credentials or PII transmitted in plaintext; MITM attack. | Critical | _(documented: docs strongly recommend HTTPS)_ |
| Tenant database credentials encrypted at rest in tenant config | `fineract.tenant.master-password` is set (default AES/CBC/PKCS5Padding); master password is set. | Plaintext DB credentials in `tenant_server_connections` table; recoverable by DBAs. | High | _(documented: application.properties)_ |

---

## §9 Security properties the project does not provide

### Properties explicitly disclaimed

| Property | Why it is not provided | What to do instead |
|----------|------------------------|-------------------|
| Constant-time comparison for passwords or tokens | No side-channel resistance is implemented. | Do not rely on Fineract for timing-safe secret comparison; use a separate crypto library if needed. _(inferred)_ |
| Compression-bomb defense | No limit on decompressed output size if a compressed upload is accepted. | Limit upload size at the reverse proxy (2MB is the default body limit, but compressed archives could expand beyond that). _(inferred)_ |
| Rate limiting / brute-force protection | No built-in rate limiting on login, API calls, or batch job submission. | Deploy an API gateway or WAF (e.g., NGINX `limit_req`, cloud WAF) in front of Fineract. _(documented: docs recommend proxy)_ |
| Input validation for all free-text fields | Some fields accept arbitrary text that is stored and later rendered; XSS filtering is not guaranteed. | Sanitize output in the frontend (Community App or custom UI). _(inferred)_ |
| Audit log tamper-resistance | Audit logs are written to the database and filesystem; no append-only or cryptographic integrity guarantee. | Export logs to a SIEM or WORM storage immediately. _(inferred)_ |
| Backup encryption | The project does not manage database backups. | Encrypt backups at the database or storage layer. _(inferred)_ |
| Real-time fraud detection | No anomaly detection on transactions, logins, or batch jobs. | Deploy a separate fraud detection layer. _(inferred)_ |

### False-friend properties

| Feature | What it looks like | What it actually is | Why it matters |
|---------|-------------------|---------------------|----------------|
| Tenant database password encryption (AES/CBC/PKCS5Padding) | Looks like the tenant DB password is securely encrypted. | It is symmetrically encrypted with a master password stored in the same database (`fineract_tenants` — `fineract.tenant.master-password`). If the `fineract_tenants` DB is compromised, the master password reveals all tenant credentials. | Do not treat this as a security boundary against a DBA or DB server attacker. It is obfuscation, not cryptographic protection. _(documented: application.properties)_ |
| SQL Validator | Looks like a guarantee against all SQL injection. | It is a configurable series of checks against SQL queries. It protects against nearly all potential SQLi attacks, but the word "nearly" is intentional. Novel injection patterns may bypass it. | Do not expose the `sqlSearch` parameter directly to untrusted network peers without additional WAF rules. _(documented: CVE-2024-32838 description)_ |
| Self-Service API (`/v1/self/...`) | Looks like a customer-facing secure banking API. | It is a plugin (separate repo) that extends Fineract. It runs in the same JVM and database context as the back-office API. A vulnerability in the self-service plugin can compromise the entire tenant. | Do not assume the self-service surface is "sandboxed" from back-office data. _(documented: selfservice-plugin README)_ |
| Embedded SSL keystore | Looks like Fineract "supports SSL out of the box." | The embedded `keystore.jks` is self-signed, untrusted by browsers, and intended for localhost development only. | Do not use the embedded keystore in production. _(documented: fineract.apache.org/docs)_ |
| Basic Auth default | Looks like a simple, secure default. | HTTP Basic Auth transmits credentials on every request. Without TLS, it is plaintext. With TLS, it is still vulnerable to brute-force if no rate limiting is deployed downstream. | Do not expose Basic Auth endpoints directly to the internet without a WAF or rate limiter. Consider OAuth2 for production. _(documented: docs)_ |

### Well-known attack classes this category of project cannot defend against

- **SQL injection in custom reports / ad-hoc queries:** Fineract allows administrators to define custom reports with SQL. If an attacker gains admin access, they can execute arbitrary SQL through the reporting module. This is by design (admin capability) and not a vulnerability. _(inferred)_
- **Decompression bombs / zip bombs:** If file upload is enabled, compressed archives could exhaust memory. Fineract does not inspect compression ratios. _(inferred)_
- **ReDoS via regex in field configuration:** The `validation_regex` field in entity configuration accepts arbitrary regex. A poorly written regex can cause catastrophic backtracking. Fineract does not validate regex safety. _(inferred)_
- **Billion-laughs / XML external entity (XXE) attacks:** If XML input is accepted (e.g., via batch import), XXE is a risk. The model assumes JSON is the primary input format; XML handling is not a claimed property. _(inferred)_

---

## §10 Downstream responsibilities

What the operator / deployer must do for the assumptions in §5–§7 to hold:

1. **Replace the embedded SSL keystore** with a CA-trusted certificate or terminate TLS at a reverse proxy before exposing the service to any network. _(documented)_
2. **Deploy a reverse proxy / WAF / API gateway** in front of Fineract to enforce rate limiting, IP filtering, and DDoS mitigation. The project explicitly recommends not running directly on the internet. _(documented: fineract.apache.org/security.html)_
3. **Configure a strong `fineract.tenant.master-password`** and rotate it on a schedule. This password encrypts all tenant DB credentials; its compromise is equivalent to compromising all tenants. _(documented: application.properties)_
4. **Run database migrations (Liquibase) separately** from the application server in production, or ensure the app server has migration-only privileges that are dropped after startup. _(inferred)_
5. **Enable and configure OAuth2** (instead of Basic Auth) for production deployments where the API is exposed beyond a trusted internal network. _(documented: docs)_
6. **Enable 2FA** (`FINERACT_SECURITY_2FA_ENABLED=true`) and configure a secure SMS/email gateway for production admin accounts. _(documented: docs)_
7. **Segregate instance types** in large deployments: run batch managers and workers on separate nodes from API-serving nodes; use read-only instances for reporting. _(documented: docs)_
8. **Encrypt database backups** at the storage layer. Fineract does not manage backups. _(inferred)_
9. **Monitor and rotate** the `FINERACT_HIKARI_PASSWORD` and all tenant DB credentials on a schedule appropriate to the data lifetime. _(inferred)_
10. **Do not expose the Self-Service API directly to the internet** without an additional API gateway that enforces customer-specific rate limits and fraud rules. _(inferred)_
11. **Validate that the `Fineract-Platform-TenantId` header is not spoofed** at the reverse proxy layer if multi-tenant endpoints are exposed to customers. _(inferred)_

---

## §11 Known misuse patterns

1. **Exposing the API directly to the internet without a reverse proxy.** Fineract's documentation repeatedly warns against this, but it is a common misuse. The embedded Tomcat is not hardened for direct internet exposure; the self-signed cert is not trusted; and there is no built-in rate limiting. _(documented: fineract.apache.org/security.html)_

2. **Using the embedded SSL keystore in production.** Developers sometimes deploy with the default `keystore.jks` because "it works." This leaves users vulnerable to MITM attacks because the cert is self-signed and the private key is public (shipped in the JAR). _(documented: docs)_

3. **Enabling both Basic Auth and OAuth simultaneously.** The project checks this on startup and fails, but misconfigured environment variables can cause startup loops or fallback to Basic Auth in containerized deployments. _(documented: application.properties, README)_

4. **Running batch jobs on the same instance as API requests in large deployments.** The COB batch can consume all CPU/memory, causing API unavailability. The model supports segregated instance types, but operators often ignore this. _(documented: docs)_

5. **Using the `sqlSearch` parameter to build "custom reports" via the API.** While `sqlSearch` is a powerful query tool, exposing it to low-trust users (or the self-service plugin) creates a recurring SQL injection risk even with the SQL Validator. The validator is a defense-in-depth layer, not a guarantee. _(documented: CVE history, SQL Validator description)_

6. **Storing the `fineract.tenant.master-password` in plaintext in environment variables or config maps.** In containerized deployments, it is common to inject this via Kubernetes secrets or Docker env vars. If the orchestration layer is compromised, all tenant DB passwords are recoverable. _(inferred)_

7. **Treating the self-service plugin as a security boundary.** The self-service plugin runs in the same JVM and database as the back-office API. A vulnerability in `/v1/self/registration` or `/v1/self/accounttransfers` can lead to back-office data compromise because there is no process-level isolation. _(documented: selfservice-plugin README)_

---

## §11a Known non-findings (recurring false positives)

| What the tool reports | Why it is safe under the model | Citation |
|-----------------------|-------------------------------|----------|
| "Self-signed certificate in `keystore.jks`" | The embedded keystore is dev-only per §5a and §9. A production report against it is `OUT-OF-MODEL: non-default-build`. | §5a, §9 |
| "Weak cipher `TLS_RSA_WITH_AES_128_CBC_SHA256` in `server.ssl.ciphers`" | The default cipher suite is configurable via `FINERACT_SERVER_SSL_CIPHERS`. If the operator has not changed it, that is a deployment issue, not a code vulnerability. | §5a, §10 |
| "Hardcoded database password in `application.properties`" | The shipped `application.properties` contains placeholder/default values. The project explicitly warns: "Never commit application.properties with credentials to version control." Production deployments must override via env vars. | §5a, §10, _(documented: dev guide)_ |
| "Tomcat access log disabled (`server.tomcat.accesslog.enabled=false`)" | Access logging is an operational choice, not a security vulnerability. The project does not claim audit logging as a §8 property. | §8, §9 |
| "Missing HttpOnly / Secure flags on cookies" | Fineract uses stateless HTTP Basic or OAuth2 JWT authentication; it does not rely on session cookies for security. Missing cookie flags are not a security property claimed by the project. | §8, §9 |
| "Unchecked malloc / memory allocation" | Fineract is a Java/Spring Boot application; memory allocation is managed by the JVM. Reports of unchecked C-style allocations are false positives from generic scanners. | §5 |
| "SQL injection in `sqlSearch` — generic pattern match" | The SQL Validator (introduced in 1.10.1) is a claimed defense. A generic regex match that does not account for the validator is a false positive. Verify against the actual validator logic before reporting. | §8, _(documented: CVE-2024-32838 fix)_ |
| "Path traversal in file upload — old CVE pattern" | CVE-2022-44635 was fixed in 1.8.1. Scanners flagging the old pattern against current HEAD are false positives. | §8, _(documented: CVE-2022-44635)_ |
| "Weak password policy — default allows short passwords" | CVE-2025-23408 was fixed in 1.11.0. Current versions enforce stronger policies. | §8, _(documented: CVE-2025-23408)_ |
| "Privilege escalation — user can modify own role" | CVE-2024-23537 was fixed in 1.9.0. Current versions enforce RBAC checks. | §8, _(documented: CVE-2024-23537)_ |
| "SSRF via report URL" | CVE-2023-25195 was fixed in 1.8.4/1.7.3. Current versions restrict outbound URLs. | §8, _(documented: CVE-2023-25195)_ |

---

## §12 Conditions that would change this model

The following changes should trigger a revision of this threat model:

1. New public API surface (e.g., a new `/api/v3/...` major version, GraphQL endpoint, or gRPC service).
2. New input format accepted (e.g., XML batch imports, protobuf, Avro).
3. New network surface (e.g., native Kafka consumer instead of just producer, WebSocket support, gRPC).
4. New deployment context (e.g., serverless/FaaS, edge computing, mobile SDK embedding).
5. New authentication scheme (e.g., mTLS for clients, SAML, LDAP integration).
6. Promotion of a shipped-but-unsupported component into core (e.g., if the self-service plugin moves from openMF/ into apache/fineract proper).
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
| `OUT-OF-MODEL: unsupported-component` | Lands in `kubernetes/`, `docker-compose*.yml`, `selfservice-plugin` (external repo), or other code placed out of scope. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only manifests under a discouraged or non-default §5a flag (e.g., `FINERACT_SERVER_SSL_ENABLED=false` in production, or embedded keystore used in production). | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a property the project explicitly does not provide (e.g., rate limiting, constant-time comparison, backup encryption). | §9 |
| `KNOWN-NON-FINDING` | Matches a documented recurring false positive from §11a. | §11a |
| `MODEL-GAP` | Cannot be cleanly routed to any of the above. Triggers §12 revision. | §12 |

---

## §14 Open questions for the maintainers

Grouped in waves. All questions route to one or more _(inferred)_ tags in the body.

### Wave 1 — Scope, Authentication, and Actuator Security

1. **Actuator security posture.** The Spring Boot actuator endpoints (`/actuator/health`, `/actuator/info`) are unauthenticated by default in Spring Boot. Does Fineract explicitly secure or expose these? We believe `/actuator/health` is exposed for liveness/readiness probes (as shown in README curl examples), but we are unsure about `/actuator/info`, `/actuator/env`, or `/actuator/loggers`.
   - *Proposed answer:* Only `/actuator/health` and `/actuator/info` are exposed; `/actuator/env` and sensitive endpoints are disabled or secured.
   - *Lands in:* §5, §8. *(Tags: inferred — actuator reachability)*

2. **Self-service plugin canonical status.** The self-service plugin lives in openMF/selfservice-plugin, not apache/fineract. Is it an officially supported extension, or a community plugin? We treat it as out-of-model per §3, but if the PMC considers it a de-facto part of the Fineract ecosystem, we should reference it differently.
   - *Proposed answer:* It is a community plugin maintained by the Mifos Initiative, not an Apache Fineract release artifact.
   - *Lands in:* §3. *(Tags: inferred — plugin status)*

3. **Liquibase in production.** The default `spring.liquibase.enabled=true` means the app server runs migrations on startup. Is this the recommended production posture, or should operators run migrations separately?
   - *Proposed answer:* It is supported for convenience, but operators should run migrations separately in production.
   - *Lands in:* §5a, §10. *(Tags: inferred — liquibase production posture)*

4. **Kafka authentication defaults.** The Kafka docker-compose examples show PLAINTEXT (no auth) and AWS MSK (IAM auth). Is PLAINTEXT a dev-only default, or is unauthenticated Kafka considered a supported production configuration?
   - *Proposed answer:* PLAINTEXT is dev-only; production must use SASL/SSL or mTLS.
   - *Lands in:* §5a, §7. *(Tags: inferred — kafka auth)*

5. **ActiveMQ authentication defaults.** Similarly, the ActiveMQ docker-compose uses `tcp://activemq:61616` without credentials. Is this dev-only?
   - *Proposed answer:* Dev-only; production ActiveMQ must use authenticated connections.
   - *Lands in:* §5a, §7. *(Tags: inferred — activemq auth)*

### Wave 2 — Input Trust and Resource Bounds

6. **JSON depth / recursion limits.** Does Fineract configure Jackson `StreamReadConstraints` to limit JSON depth, string length, or number length? We do not see explicit configuration in `application.properties`.
   - *Proposed answer:* No explicit limits beyond Spring Boot defaults; large nested JSON may cause `StackOverflowError`.
   - *Lands in:* §6, §8. *(Tags: inferred — json bounds)*

7. **File upload size limits.** Beyond the 2MB form post limit, are there separate limits for multipart file uploads (e.g., client images, document uploads)?
   - *Proposed answer:* The same 2MB Tomcat limit applies; larger uploads require streaming configuration.
   - *Lands in:* §6. *(Tags: inferred — upload limits)*

8. **Rate limiting on batch job submission.** Can an authenticated user flood the system with batch job launch requests? Is there any throttling?
   - *Proposed answer:* No built-in throttling; relies on downstream reverse proxy.
   - *Lands in:* §8, §9. *(Tags: inferred — batch rate limits)*

9. **Custom report SQL validation.** Administrators can write custom SQL reports. Is this SQL subject to the same SQL Validator as the `sqlSearch` parameter?
   - *Proposed answer:* Custom report SQL is executed with elevated privileges and is not validated by the SQL Validator; it is an admin-only feature.
   - *Lands in:* §6, §9. *(Tags: inferred — custom report sql)*

### Wave 3 — Meta and Document Ownership

10. **Document coexistence with SECURITY.md and security.html.** The project has `fineract.apache.org/security.html` (CVE list) and a `SECURITY.md` (disclosure process). This threat model is a new document. Should it (a) replace sections of `SECURITY.md`, (b) become canonical and be linked from `SECURITY.md`, or (c) sit alongside?
    - *Proposed answer:* It becomes canonical and is linked from `SECURITY.md` and `security.html`.
    - *Lands in:* §1. *(No body tag — meta question)*

11. **Revision policy.** Who owns this document? Should it be reviewed per-release, per-major-change, or on a schedule?
    - *Proposed answer:* The PMC owns it; reviewed per-major-release or when §12 conditions are met.
    - *Lands in:* §1. *(No body tag — meta question)*

12. **OpenFAIR / ArchiMate integration acceptance.** This model uses OMG OpenFAIR taxonomy and ArchiMate RSO notation in §4, §7, and §8. Does the PMC accept these notations, or prefer plain prose only?
    - *Proposed answer:* Accept as supplementary notation; keep plain prose canonical.
    - *Lands in:* Header. *(No body tag — meta question)*

---

## §15 Optional: machine-readable companion

A sidecar `threat-model.yaml` is recommended for automated triage pipelines. The prose document remains canonical; this is a derived index.

```yaml
project: apache-fineract
version: "1.12.x"
date: "2026-06-05"

entry_points:
  - path: "/api/v1/clients"
    method: "GET"
    parameters:
      - name: "sqlSearch"
        attacker_controllable: true
        trust_ref: "§6"
      - name: "orderBy"
        attacker_controllable: true
        trust_ref: "§6"
      - name: "sortOrder"
        attacker_controllable: true
        trust_ref: "§6"
  - path: "/api/v1/loans"
    method: "GET"
    parameters:
      - name: "sqlSearch"
        attacker_controllable: true
        trust_ref: "§6"
  - path: "/v1/self/authentication"
    method: "POST"
    parameters:
      - name: "username"
        attacker_controllable: true
        trust_ref: "§6"
      - name: "password"
        attacker_controllable: true
        trust_ref: "§6"

component_families:
  - name: "Core REST API"
    in_scope: true
    trust_boundary: "HTTP API surface"
  - name: "Self-Service API"
    in_scope: false
    note: "External plugin; out of scope per §3"
  - name: "Batch Engine"
    in_scope: true
    trust_boundary: "Batch job launch / partition messages"
  - name: "Event Publisher"
    in_scope: true
    trust_boundary: "Message broker producer"
  - name: "Kubernetes manifests"
    in_scope: false
    note: "Deployment artifact per §3"

build_flags:
  - name: "FINERACT_SERVER_SSL_ENABLED"
    default: true
    security_relevant: true
    discouraged_value: false
    maintainer_stance: "dev-only"
  - name: "fineract.security.oauth.enabled"
    default: false
    security_relevant: true
    discouraged_value: null
    maintainer_stance: "supported"
  - name: "fineract.security.2fa.enabled"
    default: false
    security_relevant: true
    discouraged_value: null
    maintainer_stance: "supported"
  - name: "FINERACT_REMOTE_JOB_MESSAGE_HANDLER_KAFKA_ENABLED"
    default: false
    security_relevant: true
    discouraged_value: null
    maintainer_stance: "supported when enabled"

claimed_properties:
  - property: "No SQL injection on sqlSearch/orderBy/sortOrder"
    severity: "Critical"
    violation_symptom: "Database error, unauthorized data extraction"
    section: "§8"
  - property: "Tenant isolation"
    severity: "Critical"
    violation_symptom: "Cross-tenant data visibility"
    section: "§8"
  - property: "RBAC enforcement"
    severity: "Critical"
    violation_symptom: "Low-privilege user performs admin action"
    section: "§8"
  - property: "TLS encryption on API traffic"
    severity: "Critical"
    violation_symptom: "Credentials in plaintext"
    section: "§8"

disclaimed_properties:
  - property: "Rate limiting / brute-force protection"
    section: "§9"
  - property: "Constant-time comparison"
    section: "§9"
  - property: "Backup encryption"
    section: "§9"
  - property: "Real-time fraud detection"
    section: "§9"

known_non_findings:
  - pattern: "Self-signed certificate in keystore.jks"
    disposition: "OUT-OF-MODEL: non-default-build"
    section: "§11a"
  - pattern: "Weak cipher in default SSL config"
    disposition: "OUT-OF-MODEL: non-default-build"
    section: "§11a"
  - pattern: "Hardcoded password in application.properties"
    disposition: "OUT-OF-MODEL: non-default-build"
    section: "§11a"
  - pattern: "SQL injection in sqlSearch (generic pattern)"
    disposition: "KNOWN-NON-FINDING"
    section: "§11a"

dispositions:
  - label: "VALID"
    licensed_by: "§8, §6, §7"
  - label: "VALID-HARDENING"
    licensed_by: "§11"
  - label: "OUT-OF-MODEL: trusted-input"
    licensed_by: "§6"
  - label: "OUT-OF-MODEL: adversary-not-in-scope"
    licensed_by: "§7"
  - label: "OUT-OF-MODEL: unsupported-component"
    licensed_by: "§3"
  - label: "OUT-OF-MODEL: non-default-build"
    licensed_by: "§5a"
  - label: "BY-DESIGN: property-disclaimed"
    licensed_by: "§9"
  - label: "KNOWN-NON-FINDING"
    licensed_by: "§11a"
  - label: "MODEL-GAP"
    licensed_by: "§12"
```

---

> This document follows the ASF Security Team Threat-Model Producer Skill, with OMG OpenFAIR taxonomy used for adversary capability/frequency annotation in §7 and ArchiMate 3.2 Risk and Security Overlay (RSO) notation used for trust-boundary and data-flow visualization in §4.
