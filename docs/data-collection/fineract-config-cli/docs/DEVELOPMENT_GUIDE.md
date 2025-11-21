# Fineract Config CLI - Development Guide

**Developer setup, testing, and contribution guide**

**Version**: 1.0
**Last Updated**: 2025-01-20

---

## Table of Contents

- [Getting Started](#getting-started)
- [Development Environment](#development-environment)
- [Project Structure](#project-structure)
- [Building the Project](#building-the-project)
- [Running Tests](#running-tests)
- [Development Workflow](#development-workflow)
- [Code Standards](#code-standards)
- [Debugging](#debugging)
- [Contributing](#contributing)

---

## Getting Started

### Prerequisites

**Required**:
- Java 17 or higher (OpenJDK recommended)
- Maven 3.9+
- Docker & Docker Compose
- Git

**Optional**:
- IntelliJ IDEA / VS Code with Java extensions
- Python 3.8+ (for scripts)
- Postman / Insomnia (API testing)

### Quick Setup

```bash
# 1. Clone repository
cd fineract/docs/data-collection/fineract-config-cli

# 2. Verify Java version
java -version  # Should be 17+

# 3. Build project
mvn clean install

# 4. Run tests
mvn test

# 5. Start dev environment
docker-compose up -d
```

---

## Development Environment

### IDE Setup

#### IntelliJ IDEA

```bash
# 1. Open project
File → Open → Select pom.xml

# 2. Import Maven project
# IntelliJ will auto-detect and import

# 3. Enable annotation processing
Settings → Build, Execution, Deployment → Compiler → Annotation Processors
  ✓ Enable annotation processing

# 4. Install plugins (optional)
- Lombok
- Spring Boot
- Docker
```

#### VS Code

```bash
# 1. Open folder
code .

# 2. Install extensions
- Extension Pack for Java
- Spring Boot Extension Pack
- Docker
- YAML

# 3. Configure Java
CMD+SHIFT+P → Java: Configure Java Runtime
Select JDK 17
```

### Docker Environment

Create `docker-compose.dev.yml`:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: mysql
      MYSQL_DATABASE: fineract_default
    ports:
      - "3306:3306"
    volumes:
      - mysql_dev_data:/var/lib/mysql

  fineract:
    image: apache/fineract:latest
    environment:
      FINERACT_DEFAULT_TENANTDB_HOSTNAME: mysql
      FINERACT_DEFAULT_TENANTDB_PWD: mysql
    ports:
      - "8080:8080"
    depends_on:
      - mysql

volumes:
  mysql_dev_data:
```

```bash
# Start dev environment
docker-compose -f docker-compose.dev.yml up -d

# Check Fineract is running
curl http://localhost:8080/fineract-provider/actuator/health

# View logs
docker-compose -f docker-compose.dev.yml logs -f fineract
```

---

## Project Structure

```
fineract-config-cli/
├── src/
│   ├── main/
│   │   ├── java/org/apache/fineract/config/
│   │   │   ├── FineractConfigCliApplication.java
│   │   │   ├── config/
│   │   │   │   ├── FineractProperties.java
│   │   │   │   ├── ImportProperties.java
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── provider/
│   │   │   │   ├── FineractApiClient.java
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthProvider.java
│   │   │   │   │   ├── BasicAuthProvider.java
│   │   │   │   │   └── OAuth2AuthProvider.java
│   │   │   │   └── FineractEntityService.java
│   │   │   ├── service/
│   │   │   │   ├── ImportService.java
│   │   │   │   ├── StateManagementService.java
│   │   │   │   ├── ChecksumService.java
│   │   │   │   ├── ValidationService.java
│   │   │   │   ├── VariableSubstitutionService.java
│   │   │   │   └── loaders/
│   │   │   │       ├── EntityLoader.java (interface)
│   │   │   │       ├── AbstractEntityLoader.java
│   │   │   │       ├── OfficeLoader.java
│   │   │   │       ├── StaffLoader.java
│   │   │   │       └── ... (31 loaders total)
│   │   │   ├── model/
│   │   │   │   ├── FineractConfig.java
│   │   │   │   ├── ImportContext.java
│   │   │   │   ├── ImportResult.java
│   │   │   │   ├── ImportState.java
│   │   │   │   └── entities/
│   │   │   │       ├── Office.java
│   │   │   │       ├── Staff.java
│   │   │   │       └── ... (entity POJOs)
│   │   │   ├── repository/
│   │   │   │   └── StateRepository.java
│   │   │   ├── util/
│   │   │   │   ├── ChecksumUtil.java
│   │   │   │   ├── DependencyResolver.java
│   │   │   │   └── YamlParser.java
│   │   │   └── exception/
│   │   │       ├── ImportException.java
│   │   │       ├── ValidationException.java
│   │   │       └── ...
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── logback.xml
│   │       └── schema/
│   │           └── fineract-config-schema.json
│   └── test/
│       ├── java/org/apache/fineract/config/
│       │   ├── unit/
│       │   │   ├── ChecksumServiceTest.java
│       │   │   ├── DependencyResolverTest.java
│       │   │   └── ...
│       │   └── integration/
│       │       ├── ImportServiceIntegrationTest.java
│       │       ├── OfficeLoaderIntegrationTest.java
│       │       └── ...
│       └── resources/
│           ├── test-config.yml
│           └── test-data/
├── scripts/
│   ├── excel_to_yaml.py
│   ├── validate_yaml.py
│   └── test_integration.sh
├── config-examples/
│   ├── minimal-config.yml
│   ├── complete-demo.yml
│   └── production-template.yml
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── ENTITY_REFERENCE.md
│   ├── YAML_SCHEMA.md
│   └── DEVELOPMENT_GUIDE.md
├── pom.xml
├── README.md
└── IMPLEMENTATION_PLAN.md
```

---

## Building the Project

### Maven Build

```bash
# Clean build
mvn clean install

# Skip tests (faster)
mvn clean install -DskipTests

# Build without running integration tests
mvn clean install -DskipITs

# Package JAR only
mvn package

# Run specific phase
mvn compile
mvn test
mvn verify
```

### Build Profiles

```bash
# Development profile
mvn clean install -Pdev

# Production profile
mvn clean install -Pprod

# Integration tests
mvn verify -Pintegration-tests
```

### Docker Build

```bash
# Build Docker image
docker build -t fineract-config-cli:dev -f docker/Dockerfile .

# Build with specific tag
docker build -t fineract-config-cli:1.0.0-SNAPSHOT .

# Multi-stage build (optimized)
docker build --target builder -t fineract-config-cli:builder .
docker build --target runtime -t fineract-config-cli:latest .
```

---

## Running Tests

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=ChecksumServiceTest

# Run specific test method
mvn test -Dtest=ChecksumServiceTest#testSha256Calculation

# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Integration Tests

```bash
# Run integration tests (requires Docker)
mvn verify -Pintegration-tests

# Run specific integration test
mvn verify -Dit.test=ImportServiceIntegrationTest

# Skip unit tests, run only integration tests
mvn verify -DskipTests -Pintegration-tests
```

### Test Coverage Requirements

- **Unit Tests**: > 80% coverage
- **Integration Tests**: All critical paths
- **E2E Tests**: Complete workflow scenarios

### Writing Tests

#### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class ChecksumServiceTest {

    @InjectMocks
    private ChecksumService checksumService;

    @Test
    void testSha256Calculation() {
        // Given
        String input = "test content";

        // When
        String checksum = checksumService.calculate(input);

        // Then
        assertThat(checksum).isNotNull();
        assertThat(checksum).hasSize(64);
        assertThat(checksum).matches("[a-f0-9]{64}");
    }

    @Test
    void testChecksumConsistency() {
        // Given
        String input = "test content";

        // When
        String checksum1 = checksumService.calculate(input);
        String checksum2 = checksumService.calculate(input);

        // Then
        assertThat(checksum1).isEqualTo(checksum2);
    }
}
```

#### Integration Test Example

```java
@SpringBootTest
@Testcontainers
class OfficeLoaderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("fineract_test");

    @Container
    static GenericContainer<?> fineract = new GenericContainer<>("apache/fineract:latest")
        .withExposedPorts(8080)
        .dependsOn(mysql);

    @Autowired
    private OfficeLoader officeLoader;

    @Autowired
    private FineractApiClient apiClient;

    @Test
    void testLoadOffice() {
        // Given
        Office office = Office.builder()
            .name("Test Office")
            .externalId("TEST001")
            .openingDate(LocalDate.now())
            .build();

        ImportContext context = new ImportContext();

        // When
        LoadResult result = officeLoader.load(office, context);

        // Then
        assertThat(result.getAction()).isEqualTo(LoadAction.CREATED);
        assertThat(context.getEntityId("office", "Test Office")).isNotNull();
    }

    @Test
    void testIdempotentLoad() {
        // First load
        LoadResult result1 = officeLoader.load(office, context);

        // Second load (should detect no changes)
        LoadResult result2 = officeLoader.load(office, context);

        assertThat(result1.getAction()).isEqualTo(LoadAction.CREATED);
        assertThat(result2.getAction()).isEqualTo(LoadAction.UNCHANGED);
    }
}
```

---

## Development Workflow

### Feature Development

```bash
# 1. Create feature branch
git checkout -b feature/add-loan-product-loader

# 2. Make changes
# - Write code
# - Write tests
# - Update documentation

# 3. Run tests locally
mvn clean verify

# 4. Commit changes
git add .
git commit -m "feat: add loan product loader

- Implement LoanProductLoader
- Add integration tests
- Update ENTITY_REFERENCE.md"

# 5. Push to remote
git push origin feature/add-loan-product-loader

# 6. Create pull request
# Open PR on GitHub
```

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting)
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Build/tooling changes

**Example**:

```
feat(loaders): add loan product loader

- Implement LoanProductLoader with accounting mappings
- Add dependency resolution for GL accounts and charges
- Include integration tests with Testcontainers

Closes #123
```

### Code Review Checklist

- [ ] All tests passing
- [ ] Code coverage > 80%
- [ ] Documentation updated
- [ ] No compiler warnings
- [ ] Follows code standards
- [ ] Commit message follows format
- [ ] PR description clear

---

## Code Standards

### Java Code Style

```java
// ✅ GOOD: Clear, readable code
@Service
@Slf4j
public class OfficeLoader implements EntityLoader<Office> {

    private final FineractApiClient apiClient;
    private final DependencyResolver resolver;

    public OfficeLoader(FineractApiClient apiClient, DependencyResolver resolver) {
        this.apiClient = apiClient;
        this.resolver = resolver;
    }

    @Override
    public LoadResult load(Office office, ImportContext context) {
        log.debug("Loading office: {}", office.getName());

        // Resolve parent office if exists
        if (office.getParentOffice() != null) {
            Long parentId = resolver.resolve(office.getParentOffice(), context);
            office.setParentId(parentId);
        }

        // Check if exists
        Optional<Long> existingId = findByExternalId(office.getExternalId());

        if (existingId.isPresent()) {
            return updateIfChanged(existingId.get(), office);
        } else {
            return create(office, context);
        }
    }
}

// ❌ BAD: Unclear, no error handling
public class OfficeLoader {
    public void load(Office o) {
        Long id = apiClient.post("/offices", o).getId();
    }
}
```

### Formatting

Use **spotless** for automatic formatting:

```bash
# Check formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply
```

**pom.xml configuration**:

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.41.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.17.0</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
            <importOrder>
                <order>java,javax,org,com</order>
            </importOrder>
            <removeUnusedImports/>
        </java>
    </configuration>
</plugin>
```

### Naming Conventions

```java
// Classes: PascalCase
public class ImportService { }
public class OfficeLoader { }

// Methods: camelCase
public LoadResult loadOffice(Office office) { }
public boolean checksumMatches(String checksum) { }

// Constants: UPPER_SNAKE_CASE
public static final String DEFAULT_TENANT = "default";
public static final int MAX_RETRIES = 3;

// Variables: camelCase
String configPath = "/config/demo.yml";
ImportContext context = new ImportContext();

// Packages: lowercase
org.apache.fineract.config.service
org.apache.fineract.config.model
```

### Logging

```java
@Slf4j
public class ImportService {

    public ImportResult executeImport() {
        log.info("Starting import: tenant={}, config={}", tenant, configPath);

        try {
            // ... import logic
            log.debug("Loaded {} entities", entityCount);
            log.info("Import completed successfully: duration={}ms", duration);
        } catch (ImportException ex) {
            log.error("Import failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
```

**Logging Levels**:
- `ERROR`: Failures requiring immediate attention
- `WARN`: Unexpected but handled situations
- `INFO`: Major operations and milestones
- `DEBUG`: Detailed execution flow
- `TRACE`: Very verbose debugging

---

## Debugging

### Local Debugging

#### IntelliJ IDEA

```bash
# 1. Set breakpoints in code
# 2. Right-click main class
# 3. Debug 'FineractConfigCliApplication'
```

#### VS Code

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Fineract Config CLI",
      "request": "launch",
      "mainClass": "org.apache.fineract.config.FineractConfigCliApplication",
      "args": [
        "--fineract.url=http://localhost:8080/fineract-provider",
        "--import.files=config-examples/minimal-config.yml"
      ],
      "env": {
        "FINERACT_USER": "mifos",
        "FINERACT_PASSWORD": "password"
      }
    }
  ]
}
```

### Remote Debugging

```bash
# 1. Start application with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar target/fineract-config-cli.jar

# 2. Attach debugger
# IntelliJ: Run → Attach to Process
# VS Code: Use "Attach" configuration
```

### Docker Debugging

```yaml
# docker-compose.debug.yml
services:
  config-cli:
    image: fineract-config-cli:dev
    environment:
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    ports:
      - "5005:5005"
```

### Common Issues

#### Issue: "Cannot connect to Fineract"

```bash
# Check Fineract is running
curl http://localhost:8080/fineract-provider/actuator/health

# Check Docker network
docker network inspect fineract_default

# Enable SSL debug
java -Djavax.net.debug=ssl -jar fineract-config-cli.jar
```

#### Issue: "Dependency not found"

```bash
# Enable dependency resolution logging
logging.level.org.apache.fineract.config.util.DependencyResolver=TRACE
```

#### Issue: "Out of memory"

```bash
# Increase heap size
java -Xmx2g -jar fineract-config-cli.jar
```

---

## Contributing

### Before Contributing

1. Read [IMPLEMENTATION_PLAN.md](../IMPLEMENTATION_PLAN.md)
2. Check [GitHub Issues](https://github.com/apache/fineract/issues)
3. Join [mailing list](https://fineract.apache.org/community.html)

### Contribution Process

```bash
# 1. Fork repository
# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/fineract.git

# 3. Add upstream remote
git remote add upstream https://github.com/apache/fineract.git

# 4. Create branch
git checkout -b feature/my-feature

# 5. Make changes and test
mvn clean verify

# 6. Commit with clear messages
git commit -m "feat: add feature X"

# 7. Push to your fork
git push origin feature/my-feature

# 8. Create pull request on GitHub
```

### Pull Request Guidelines

**Title**: Clear, concise description
```
feat(loaders): add savings product loader
fix(validation): handle null references
docs(readme): update installation steps
```

**Description**: Should include:
- What changed
- Why it changed
- How to test
- Related issues

**Example**:

```markdown
## Description
Implements savings product loader with full accounting integration.

## Changes
- Add SavingsProductLoader class
- Implement GL account mapping
- Add integration tests
- Update ENTITY_REFERENCE.md

## Testing
1. Start dev environment: `docker-compose up`
2. Run tests: `mvn verify -Pintegration-tests`
3. Load sample config: `java -jar target/fineract-config-cli.jar --import.files=config-examples/savings-product.yml`

## Related Issues
Closes #123
```

### Code Review Process

1. **Automated checks** run on PR
   - Build succeeds
   - Tests pass
   - Code coverage > 80%
   - No style violations

2. **Manual review** by maintainer
   - Code quality
   - Architecture fit
   - Documentation

3. **Feedback addressed**
   - Make requested changes
   - Push updates to PR branch

4. **Approval and merge**
   - 1+ approvals required
   - Squash and merge to main

---

## Resources

### Documentation

- [README.md](../README.md) - Project overview
- [IMPLEMENTATION_PLAN.md](../IMPLEMENTATION_PLAN.md) - Development roadmap
- [ARCHITECTURE.md](ARCHITECTURE.md) - Technical architecture
- [ENTITY_REFERENCE.md](ENTITY_REFERENCE.md) - Entity documentation
- [YAML_SCHEMA.md](YAML_SCHEMA.md) - Configuration format

### External Resources

- [Fineract API Docs](https://demo.fineract.dev/fineract-provider/api-docs/apiLive.htm)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Keycloak Config CLI](https://github.com/adorsys/keycloak-config-cli) (inspiration)
- [Testcontainers](https://www.testcontainers.org/)

### Community

- **Mailing Lists**: [Fineract Community](https://fineract.apache.org/community.html)
- **Slack**: [Apache Fineract](https://fineract.apache.org/community.html)
- **GitHub**: [Issues](https://github.com/apache/fineract/issues) | [Discussions](https://github.com/apache/fineract/discussions)

---

## Getting Help

1. **Check Documentation**: Search docs first
2. **Search Issues**: Look for similar problems
3. **Ask on Slack**: Real-time help from community
4. **Create Issue**: For bugs or feature requests
5. **Email List**: For design discussions

---

**Happy Coding!** 🚀

Thank you for contributing to Fineract Config CLI!
