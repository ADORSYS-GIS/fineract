# Fineract Config CLI - Architecture

**Version**: 1.0
**Last Updated**: 2025-01-20

---

## Table of Contents

- [Overview](#overview)
- [Design Philosophy](#design-philosophy)
- [Architecture Patterns](#architecture-patterns)
- [System Components](#system-components)
- [Data Flow](#data-flow)
- [State Management](#state-management)
- [Dependency Resolution](#dependency-resolution)
- [Authentication](#authentication)
- [Error Handling](#error-handling)
- [Performance Considerations](#performance-considerations)

---

## Overview

Fineract Config CLI is a Spring Boot-based command-line application that enables declarative configuration management for Apache Fineract. It follows the Configuration-as-Code (CaC) pattern, inspired by [keycloak-config-cli](https://github.com/adorsys/keycloak-config-cli).

### Key Characteristics

- **Stateless Application**: No local persistence required
- **Remote State Tracking**: State stored in Fineract database
- **Idempotent**: Same input produces same output, safe to re-run
- **Declarative**: Describe desired state, tool converges to it
- **API-Driven**: All operations via Fineract REST API

---

## Design Philosophy

### 1. Configuration as Code

Configuration files are treated as code:

- **Version Controlled**: Store in Git for history and audit
- **Peer Reviewed**: Use Pull Requests for changes
- **Tested**: Validate before deployment
- **Automated**: CI/CD pipeline integration

### 2. Idempotency

Running the tool multiple times with same config produces identical results:

```
Initial State → Apply Config → Final State
Final State   → Apply Config → Final State (no changes)
```

**Implementation**: Checksum-based change detection

### 3. Fail-Fast Validation

Detect errors before making any changes:

- YAML syntax validation
- Schema validation
- Dependency validation
- Pre-flight API checks

### 4. Explicit Over Implicit

Clear, verbose configuration preferred over magic:

```yaml
# Good: Explicit reference
staff:
  office: $office.Head Office

# Avoid: Implicit lookup by name
staff:
  office: Head Office  # Could be ambiguous
```

---

## Architecture Patterns

### 1. Layered Architecture

```
┌─────────────────────────────────────────┐
│         CLI Interface Layer             │
│  (Command Line Args, Environment Vars)  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Application Service Layer         │
│    (ImportService, Orchestration)       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Domain Service Layer               │
│  (Entity Loaders, State Management,     │
│   Validation, Dependency Resolution)    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Infrastructure Layer               │
│  (Fineract API Client, YAML Parser,     │
│   State Repository, Checksum Util)      │
└─────────────────────────────────────────┘
```

### 2. Strategy Pattern (Entity Loaders)

Each entity type has dedicated loader implementing common interface:

```java
public interface EntityLoader<T> {
    String getEntityType();
    int getLoadOrder();
    LoadResult load(T entity, ImportContext context);
    boolean supports(T entity);
}
```

### 3. Template Method Pattern (Import Process)

```java
public abstract class AbstractImportService {
    public final ImportResult executeImport(Config config) {
        // Template method
        validate(config);
        calculateChecksum(config);
        if (!hasChanges()) return noChanges();

        backup();  // Optional
        executeLoaders(config);
        saveState();

        return success();
    }

    protected abstract void validate(Config config);
    protected abstract void executeLoaders(Config config);
}
```

### 4. Chain of Responsibility (Variable Substitution)

```java
public interface VariableResolver {
    boolean supports(String variable);
    String resolve(String variable);
}

// Chain: EnvVarResolver → FileResolver → SysPropertyResolver
```

### 5. Repository Pattern (State Management)

```java
public interface StateRepository {
    Optional<ImportState> findByTenant(String tenant);
    void save(ImportState state);
    void delete(String tenant);
}
```

---

## System Components

### Core Components

#### 1. FineractConfigCliApplication

**Responsibility**: Application entry point, Spring Boot initialization

```java
@SpringBootApplication
public class FineractConfigCliApplication implements CommandLineRunner {

    @Autowired
    private ImportService importService;

    public static void main(String[] args) {
        SpringApplication.run(FineractConfigCliApplication.class, args);
    }

    @Override
    public void run(String... args) {
        importService.executeImport();
    }
}
```

#### 2. ImportService

**Responsibility**: Orchestrate import process

```java
@Service
public class ImportService {

    private final YamlParser yamlParser;
    private final ChecksumService checksumService;
    private final StateManagementService stateService;
    private final ValidationService validationService;
    private final List<EntityLoader<?>> loaders;

    public ImportResult executeImport() {
        // 1. Load configuration
        FineractConfig config = yamlParser.parse(configPath);

        // 2. Validate
        validationService.validate(config);

        // 3. Calculate checksum
        String checksum = checksumService.calculate(config);

        // 4. Check if changes exist
        if (stateService.checksumMatches(checksum)) {
            return ImportResult.noChanges();
        }

        // 5. Execute loaders in order
        ImportContext context = new ImportContext();
        for (EntityLoader<?> loader : getSortedLoaders()) {
            loader.load(config, context);
        }

        // 6. Save state
        stateService.saveState(checksum, context);

        return ImportResult.success();
    }
}
```

#### 3. FineractApiClient

**Responsibility**: HTTP communication with Fineract API

```java
@Component
public class FineractApiClient {

    private final WebClient webClient;
    private final AuthProvider authProvider;

    public <T> T get(String path, Class<T> responseType) {
        return webClient.get()
            .uri(path)
            .headers(h -> h.setBearerAuth(authProvider.getToken()))
            .retrieve()
            .bodyToMono(responseType)
            .block();
    }

    public <T, R> R post(String path, T body, Class<R> responseType) {
        return webClient.post()
            .uri(path)
            .headers(h -> h.setBearerAuth(authProvider.getToken()))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(responseType)
            .block();
    }
}
```

#### 4. StateManagementService

**Responsibility**: Track managed resources and checksums

```java
@Service
public class StateManagementService {

    private final StateRepository stateRepository;

    public boolean checksumMatches(String newChecksum) {
        Optional<ImportState> state = stateRepository.findByTenant(tenant);
        return state.isPresent() && state.get().getChecksum().equals(newChecksum);
    }

    public void saveState(String checksum, ImportContext context) {
        ImportState state = new ImportState();
        state.setTenant(tenant);
        state.setChecksum(checksum);
        state.setManagedResources(context.getManagedResources());
        state.setLastImport(Instant.now());

        stateRepository.save(state);
    }

    public Set<String> getManagedResources(String entityType) {
        return stateRepository.findByTenant(tenant)
            .map(s -> s.getManagedResources(entityType))
            .orElse(Set.of());
    }
}
```

#### 5. DependencyResolver

**Responsibility**: Resolve entity references

```java
@Component
public class DependencyResolver {

    private final Map<String, Object> resolvedEntities = new ConcurrentHashMap<>();

    public Long resolve(String reference, ImportContext context) {
        // Parse: $office.Head Office → type=office, name=Head Office
        EntityReference ref = parse(reference);

        // Check cache
        if (resolvedEntities.containsKey(reference)) {
            return (Long) resolvedEntities.get(reference);
        }

        // Resolve via API
        Long id = context.getEntityId(ref.getType(), ref.getIdentifier());
        if (id == null) {
            throw new DependencyNotFoundException(
                "Entity not found: " + reference
            );
        }

        // Cache
        resolvedEntities.put(reference, id);
        return id;
    }
}
```

#### 6. Entity Loaders

**Responsibility**: Load specific entity types

```java
@Component
public class OfficeLoader implements EntityLoader<Office> {

    private final FineractApiClient apiClient;
    private final DependencyResolver resolver;

    @Override
    public String getEntityType() {
        return "office";
    }

    @Override
    public int getLoadOrder() {
        return 10;  // After roles, before staff
    }

    @Override
    public LoadResult load(Office office, ImportContext context) {
        // Resolve parent office if exists
        if (office.getParentOffice() != null) {
            Long parentId = resolver.resolve(office.getParentOffice(), context);
            office.setParentId(parentId);
        }

        // Check if exists
        Optional<Long> existingId = findByExternalId(office.getExternalId());

        if (existingId.isPresent()) {
            // Update if changed
            if (hasChanges(existingId.get(), office)) {
                update(existingId.get(), office);
                return LoadResult.updated();
            }
            return LoadResult.unchanged();
        } else {
            // Create new
            Long id = create(office);
            context.registerEntity("office", office.getName(), id);
            return LoadResult.created();
        }
    }
}
```

---

## Data Flow

### Import Process Flow

```
┌─────────────┐
│ YAML Files  │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│  Parse & Validate   │
│  - YAML syntax      │
│  - Schema validation│
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Calculate Checksum │
│  (SHA256)           │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Compare with State │
│  Changes detected?  │
└──────┬──────────────┘
       │
       ├─NO──► Exit (no changes)
       │
       YES
       ▼
┌─────────────────────┐
│  Variable           │
│  Substitution       │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Execute Loaders    │
│  (Ordered by phase) │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Save State         │
│  - Checksum         │
│  - Managed Resources│
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Import Complete    │
└─────────────────────┘
```

### Entity Loading Flow

```
For each entity type (in dependency order):
    ┌──────────────────────┐
    │  Get entities from   │
    │  YAML config         │
    └──────┬───────────────┘
           │
           ▼
    ┌──────────────────────┐
    │  Resolve dependencies│
    │  ($references)       │
    └──────┬───────────────┘
           │
           ▼
    ┌──────────────────────┐
    │  Check if exists     │
    │  (by externalId)     │
    └──────┬───────────────┘
           │
           ├─EXISTS──┐
           │         ▼
           │    ┌─────────────────┐
           │    │  Compare state  │
           │    │  Changed?       │
           │    └────┬────────────┘
           │         │
           │         ├─YES─► Update
           │         │
           │         └─NO──► Skip
           │
           └─NOT EXISTS──► Create
                            │
                            ▼
                     ┌─────────────────┐
                     │  Register in    │
                     │  context for    │
                     │  dependency     │
                     │  resolution     │
                     └─────────────────┘
```

---

## State Management

### State Storage Structure

State is stored in Fineract database as JSON in a dedicated table or using existing structures like `m_code_value`.

#### Option 1: Custom Table

```sql
CREATE TABLE m_config_cli_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant VARCHAR(100) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    managed_resources JSON,
    last_import_date TIMESTAMP,
    UNIQUE KEY unique_tenant (tenant)
);
```

#### Option 2: Using m_code_value

```json
{
  "codeId": 999,
  "codeName": "FineractConfigCLI",
  "codeValue": {
    "tenant": "default",
    "checksum": "sha256:abc123...",
    "lastImport": "2024-01-20T10:30:00Z",
    "managedResources": {
      "office": ["1", "2", "3"],
      "staff": ["1", "2"],
      "loanProduct": ["1", "2", "3"]
    }
  }
}
```

### Managed Resources Tracking

Track which resources were created by the tool:

```java
public class ImportState {
    private String tenant;
    private String checksum;
    private Instant lastImport;
    private Map<String, Set<String>> managedResources;  // entity type → IDs

    public void addManagedResource(String entityType, String entityId) {
        managedResources
            .computeIfAbsent(entityType, k -> new HashSet<>())
            .add(entityId);
    }

    public boolean isManagedResource(String entityType, String entityId) {
        return managedResources
            .getOrDefault(entityType, Set.of())
            .contains(entityId);
    }
}
```

### Resource Lifecycle with Managed Modes

#### Full Mode (`import.managed.office=full`)

```
YAML:     [Office A, Office B]
Existing: [Office A (managed), Office C (managed), Office D (manual)]

Actions:
- Office A: Update if changed
- Office B: Create (new)
- Office C: Delete (managed, not in YAML)
- Office D: Keep (not managed by tool)

Result:   [Office A, Office B, Office D]
```

#### No-Delete Mode (`import.managed.client=no-delete`)

```
YAML:     [Client A, Client B]
Existing: [Client A (managed), Client C (managed), Client D (manual)]

Actions:
- Client A: Update if changed
- Client B: Create (new)
- Client C: Keep (no-delete mode)
- Client D: Keep (not managed)

Result:   [Client A, Client B, Client C, Client D]
```

---

## Dependency Resolution

### Reference Syntax

```
$<entity-type>.<identifier>

Examples:
$office.Head Office      → Resolves to office ID by name
$glAccount.42            → Resolves to GL account ID by code
$role.Branch Manager     → Resolves to role ID by name
$loanProduct.MSOL        → Resolves to loan product ID by shortName
```

### Resolution Algorithm

```java
public Long resolve(String reference, ImportContext context) {
    // 1. Parse reference
    EntityReference ref = parseReference(reference);  // type + identifier

    // 2. Check import context (entities created this run)
    Long id = context.getEntityId(ref.type, ref.identifier);
    if (id != null) return id;

    // 3. Query Fineract API
    id = queryFineractApi(ref.type, ref.identifier);
    if (id != null) {
        context.cacheEntity(ref.type, ref.identifier, id);
        return id;
    }

    // 4. Not found - fail fast
    throw new DependencyNotFoundException(
        "Cannot resolve: " + reference
    );
}
```

### Dependency Graph

Entities loaded in phases to respect dependencies:

```
Phase 1: System Config
  ↓
Phase 2: Security & Org
  Roles → Offices → Staff → Tellers
  ↓
Phase 3: Accounting
  Chart of Accounts → Financial Mappings → Teller Rules
  ↓
Phase 4: Products
  Charges → Loan Products → Savings Products
  (Products depend on GL Accounts)
  ↓
Phase 5: Accounts
  Clients → Savings Accounts → Loan Accounts
  (Accounts depend on Staff & Products)
  ↓
Phase 6: Transactions
  Deposits, Withdrawals, Repayments
```

### Circular Dependency Detection

```java
public void validateDependencies(FineractConfig config) {
    Graph<String> dependencyGraph = buildDependencyGraph(config);

    if (dependencyGraph.hasCycle()) {
        List<String> cycle = dependencyGraph.findCycle();
        throw new CircularDependencyException(
            "Circular dependency detected: " + String.join(" → ", cycle)
        );
    }
}
```

---

## Authentication

### Authentication Providers

#### 1. Basic Authentication

```java
@Component
@ConditionalOnProperty(name = "fineract.auth.type", havingValue = "basic")
public class BasicAuthProvider implements AuthProvider {

    @Value("${fineract.auth.username}")
    private String username;

    @Value("${fineract.auth.password}")
    private String password;

    @Value("${fineract.tenant}")
    private String tenant;

    @Override
    public void configureHeaders(HttpHeaders headers) {
        headers.setBasicAuth(username, password);
        headers.set("Fineract-Platform-TenantId", tenant);
    }
}
```

#### 2. OAuth2 Authentication

```java
@Component
@ConditionalOnProperty(name = "fineract.auth.type", havingValue = "oauth2")
public class OAuth2AuthProvider implements AuthProvider {

    private final WebClient webClient;
    private String cachedToken;
    private Instant tokenExpiry;

    @Override
    public String getToken() {
        if (cachedToken == null || isExpired()) {
            refreshToken();
        }
        return cachedToken;
    }

    private void refreshToken() {
        TokenResponse response = webClient.post()
            .uri(tokenUrl)
            .bodyValue(tokenRequest())
            .retrieve()
            .bodyToMono(TokenResponse.class)
            .block();

        this.cachedToken = response.getAccessToken();
        this.tokenExpiry = Instant.now().plusSeconds(response.getExpiresIn());
    }
}
```

### SSL Verification Toggle

```java
@Configuration
public class WebClientConfig {

    @Value("${fineract.ssl-verify:true}")
    private boolean sslVerify;

    @Bean
    public WebClient webClient() {
        SslContext sslContext = sslVerify
            ? SslContextBuilder.forClient().build()
            : SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
            .secure(spec -> spec.sslContext(sslContext));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

---

## Error Handling

### Error Hierarchy

```
ImportException (base)
├── ValidationException
│   ├── YamlSyntaxException
│   ├── SchemaValidationException
│   └── DependencyValidationException
├── ApiException
│   ├── AuthenticationException
│   ├── AuthorizationException
│   └── FineractApiException
├── StateException
│   ├── ChecksumMismatchException
│   └── StateCorruptedException
└── ConfigurationException
    ├── VariableNotFoundException
    └── CircularDependencyException
```

### Error Handling Strategy

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public void handleValidation(ValidationException ex) {
        log.error("Validation failed: {}", ex.getMessage());
        log.error("Details: {}", ex.getValidationErrors());
        System.exit(1);  // Fail fast
    }

    @ExceptionHandler(ApiException.class)
    public void handleApi(ApiException ex) {
        log.error("API call failed: {}", ex.getMessage());
        log.error("URL: {}, Status: {}", ex.getUrl(), ex.getStatus());

        if (ex.isRetryable()) {
            retry(ex);
        } else {
            System.exit(1);
        }
    }
}
```

### Transactional Consistency

For atomic operations within single entity type:

```java
@Transactional
public LoadResult loadOffices(List<Office> offices) {
    try {
        for (Office office : offices) {
            loadOffice(office);
        }
        return LoadResult.success();
    } catch (Exception ex) {
        // Rollback handled by @Transactional
        throw new ImportException("Failed to load offices", ex);
    }
}
```

**Note**: Cross-entity rollback not supported (Fineract API limitation). Use backup feature instead.

---

## Performance Considerations

### 1. Parallel Processing

```java
@Configuration
public class ParallelImportConfig {

    @Value("${import.parallel.enabled:false}")
    private boolean parallelEnabled;

    @Value("${import.parallel.thread-pool-size:4}")
    private int threadPoolSize;

    @Bean
    @ConditionalOnProperty("import.parallel.enabled")
    public ExecutorService importExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}

// Usage
if (parallelEnabled && !hasDependencies(loaders)) {
    List<Future<LoadResult>> futures = loaders.stream()
        .map(loader -> executor.submit(() -> loader.load(config, context)))
        .collect(Collectors.toList());

    for (Future<LoadResult> future : futures) {
        future.get();  // Wait for completion
    }
}
```

### 2. Caching

```java
@Cacheable("entityLookup")
public Long findEntityId(String entityType, String identifier) {
    // Expensive API call
    return apiClient.get("/api/v1/" + entityType + "?externalId=" + identifier);
}
```

### 3. Batch Operations

```java
// Instead of individual API calls
for (Client client : clients) {
    apiClient.post("/api/v1/clients", client);
}

// Use batch if supported
apiClient.post("/api/v1/clients/batch", clients);
```

### 4. Connection Pooling

```java
@Bean
public HttpClient httpClient() {
    ConnectionProvider provider = ConnectionProvider.builder("fineract")
        .maxConnections(50)
        .maxIdleTime(Duration.ofSeconds(20))
        .maxLifeTime(Duration.ofSeconds(60))
        .build();

    return HttpClient.create(provider);
}
```

---

## Security Considerations

### 1. Secrets Management

Never log or expose sensitive information:

```java
@JsonIgnore
private String password;

@ToString.Exclude
private String clientSecret;
```

### 2. Input Validation

Sanitize all inputs before API calls:

```java
public void validate(FineractConfig config) {
    config.getOffices().forEach(office -> {
        validateNotNull(office.getName(), "Office name required");
        validatePattern(office.getExternalId(), "[A-Z0-9]+", "Invalid external ID");
        validateNoSqlInjection(office.getName());
    });
}
```

### 3. Audit Logging

Log all configuration changes:

```java
@Aspect
@Component
public class AuditAspect {

    @AfterReturning(pointcut = "execution(* load*(..))", returning = "result")
    public void auditLoad(JoinPoint joinPoint, LoadResult result) {
        log.info("Entity loaded: type={}, action={}, user={}",
            joinPoint.getTarget().getClass().getSimpleName(),
            result.getAction(),
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }
}
```

---

## Observability

### Metrics

```java
@Component
public class ImportMetrics {

    private final MeterRegistry registry;

    public void recordImport(String entityType, LoadResult result) {
        registry.counter("import.entities",
            "type", entityType,
            "action", result.getAction()
        ).increment();
    }

    public void recordDuration(String phase, Duration duration) {
        registry.timer("import.phase.duration",
            "phase", phase
        ).record(duration);
    }
}
```

### Structured Logging

```java
@Slf4j
public class ImportService {

    public ImportResult executeImport() {
        MDC.put("tenant", tenant);
        MDC.put("importId", UUID.randomUUID().toString());

        try {
            log.info("Starting import: tenant={}, config={}", tenant, configPath);
            // ... import logic
            log.info("Import completed: entities={}, duration={}ms",
                result.getEntityCount(),
                result.getDuration());
        } finally {
            MDC.clear();
        }
    }
}
```

---

## Future Enhancements

### Planned Improvements

1. **Event Sourcing**: Track all configuration changes as events
2. **GraphQL API**: Alternative to REST for better performance
3. **Web UI**: Visual configuration builder
4. **Conflict Resolution**: Automatic merge strategies
5. **Incremental Imports**: Only process changed sections
6. **Schema Evolution**: Handle Fineract version upgrades

---

## References

- [Keycloak Config CLI Architecture](https://github.com/adorsys/keycloak-config-cli/wiki/Architecture)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Fineract API](https://demo.fineract.dev/fineract-provider/api-docs/apiLive.htm)
- [Configuration as Code Best Practices](https://www.gitops.tech/)

---

**Last Reviewed**: 2025-01-20
**Reviewers**: Architecture Team
**Status**: Approved for Implementation
