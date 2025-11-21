# Fineract Config CLI - Development Roadmap

## Project Overview

Fineract Config CLI is a Configuration-as-Code tool for Apache Fineract, enabling declarative management of Fineract configurations through YAML files. This roadmap outlines completed work and remaining enhancement tasks.

## Completed Phases ✅

### Phase 1: Core Framework & System Configuration (COMPLETED)
- ✅ Project structure and build configuration
- ✅ Spring Boot 3.2.1 + Java 17 setup
- ✅ Fineract API client with OAuth2 authentication
- ✅ YAML parsing service
- ✅ Import context and dependency resolution
- ✅ 8 System configuration loaders:
  - Global configurations
  - Codes and code values
  - Hooks
  - External services
  - Account number preferences
  - Entity-to-entity mappings
  - SMS/Email campaigns
  - Tax configurations

### Phase 2: State Management & Security (COMPLETED)
- ✅ State service with checksum tracking
- ✅ Remote state storage in Fineract
- ✅ OAuth2Provider with automatic token refresh
- ✅ Office loader (hierarchical parent-child support)
- ✅ Role loader with permission management
- ✅ User loader with role assignments

### Phase 3: Accounting Foundation (COMPLETED)
- ✅ Chart of Accounts loader (27 GL accounts demo)
- ✅ Financial Activity Mapping loader
- ✅ Payment Type loader
- ✅ Fund Source loader
- ✅ Hierarchical GL account resolution
- ✅ Type/usage mapping (ASSET, LIABILITY, etc.)

### Phase 4: Financial Products (COMPLETED)
- ✅ Charge loader (7 charge types in demo)
- ✅ Loan Product loader (322 lines, comprehensive config)
- ✅ Savings Product loader (316 lines)
- ✅ Support for 4 accounting rules
- ✅ Interest calculation configurations
- ✅ Fee and penalty handling

### Phase 5: Client Operations & Accounts (COMPLETED)
- ✅ Client loader with address support
- ✅ Group loader with member assignments
- ✅ Center loader
- ✅ Savings Account loader (approve → activate workflow)
- ✅ Loan Account loader (submit → approve → disburse workflow)
- ✅ 490-line comprehensive demo configuration

### Phase 6: Transactions & Operations (COMPLETED)
- ✅ Savings Transaction loader (deposits, withdrawals)
- ✅ Loan Transaction loader (repayments, waivers, writeoffs)
- ✅ Payment type integration
- ✅ 33 transactions in demo configuration

### CLI Application (COMPLETED)
- ✅ Spring Shell 3.2.0 integration
- ✅ Interactive command-line interface
- ✅ Commands implemented:
  - `import --file <path> [--dry-run] [--force]`
  - `validate --file <path>`
  - `export --output <path> [--phases <1-6|all>]`
  - `version`
- ✅ Executable JAR packaging

### Export Functionality (BASIC IMPLEMENTATION)
- ✅ ExportService with phase-based export
- ✅ Export for Phases 2-5 implemented:
  - Offices
  - Roles
  - GL Accounts
  - Payment Types
  - Charges
  - Loan Products
  - Savings Products
  - Clients
- ✅ YAML file generation with headers
- ✅ Type/ID mapping (integers back to strings)

### Packaging (COMPLETED)
- ✅ Maven build configuration
- ✅ Spotless code formatting (Google Java Style)
- ✅ JaCoCo code coverage (80% minimum)
- ✅ Executable JAR: `fineract-config-cli-1.0.0-SNAPSHOT.jar`
- ✅ Spring Boot packaging with dependencies

---

## Remaining Enhancement Tasks

### 1. Complete Dry-Run Mode 🚧

**Status:** Infrastructure in place, needs implementation

**Current State:**
- `--dry-run` flag exists in CLI commands
- `dryRun` property added to ImportProperties
- Loaders don't yet respect the flag

**Required Work:**

#### 1.1 Update ImportService
```java
// Pass dryRun flag to all loaders
public ImportResult executeImport() {
  boolean dryRun = importProperties.isDryRun();

  // Phase 1
  globalConfigLoader.load(config.getGlobalConfigs(), context, dryRun);
  codeLoader.load(config.getCodes(), context, dryRun);
  // ... all other loaders
}
```

#### 1.2 Modify All 23 Loaders
Pattern to implement in each loader:
```java
public void load(List<EntityType> entities, ImportContext context, boolean dryRun) {
  for (EntityType entity : entities) {
    // Check if exists
    Map<String, Object> existing = checkExistence(entity);

    if (existing != null) {
      if (hasChanges(entity, existing)) {
        if (dryRun) {
          log.info("[DRY-RUN] Would update {}: {}", type, identifier);
          // Add to preview list
        } else {
          // Actual update
        }
      }
    } else {
      if (dryRun) {
        log.info("[DRY-RUN] Would create {}: {}", type, identifier);
        // Add to preview list
      } else {
        // Actual creation
      }
    }
  }
}
```

#### 1.3 Enhanced Result Reporting
```java
public class ImportResult {
  // Add dry-run specific fields
  private List<PlannedChange> plannedChanges;

  @Data
  public static class PlannedChange {
    private String entityType;
    private String identifier;
    private ChangeType changeType; // CREATE, UPDATE, DELETE
    private Map<String, Object> proposedData;
  }
}
```

**Estimated Effort:** 3-4 days
- Update ImportService: 1 hour
- Update 23 loaders: 2-3 days (30-60 min per loader)
- Enhanced reporting: 4 hours
- Testing: 1 day

---

### 2. Implement Update Support 🔴

**Status:** Not started - Critical feature

**Current State:**
- All loaders only CREATE entities
- If entity exists with same externalId/name, it's skipped
- No change detection or update logic

**Required Work:**

#### 2.1 Change Detection Service
Create new service: `ChangeDetectionService.java`
```java
@Service
public class ChangeDetectionService {

  /**
   * Compare YAML entity with existing Fineract entity
   * @return Map of changed fields
   */
  public Map<String, Object> detectChanges(
      Map<String, Object> yamlData,
      Map<String, Object> existingData,
      Set<String> ignoredFields) {

    Map<String, Object> changes = new HashMap<>();

    for (Map.Entry<String, Object> entry : yamlData.entrySet()) {
      String field = entry.getKey();
      if (ignoredFields.contains(field)) continue;

      Object yamlValue = entry.getValue();
      Object existingValue = existingData.get(field);

      if (!Objects.equals(yamlValue, existingValue)) {
        changes.put(field, yamlValue);
      }
    }

    return changes;
  }
}
```

#### 2.2 Update Logic in All Loaders
Example: OfficeLoader.java
```java
public void load(List<Office> offices, ImportContext context, boolean dryRun) {
  for (Office office : offices) {
    Map<String, Object> existing = apiClient.get("/api/v1/offices?name=" + office.getName());

    if (existing != null && existing.containsKey("id")) {
      Long officeId = ((Number) existing.get("id")).longValue();

      // Build request body from YAML
      Map<String, Object> yamlData = buildOfficeRequest(office);

      // Detect changes (ignore created date, ID, etc.)
      Set<String> ignoredFields = Set.of("id", "openingDate", "hierarchy");
      Map<String, Object> changes = changeDetectionService.detectChanges(
          yamlData, existing, ignoredFields);

      if (!changes.isEmpty()) {
        if (dryRun) {
          log.info("[DRY-RUN] Would update office {} with changes: {}",
              office.getName(), changes.keySet());
        } else {
          // Perform update
          apiClient.put("/api/v1/offices/" + officeId, changes);
          log.info("Updated office: {} (changed fields: {})",
              office.getName(), changes.keySet());
        }
      } else {
        log.debug("Office {} unchanged", office.getName());
      }

      context.registerEntity("office", office.getName(), officeId);
    } else {
      // Create new office (existing logic)
    }
  }
}
```

#### 2.3 API Endpoint Mapping
Document all update endpoints:
```yaml
Office: PUT /api/v1/offices/{id}
Role: PUT /api/v1/roles/{id}
User: PUT /api/v1/users/{id}
GLAccount: PUT /api/v1/glaccounts/{id}
Charge: PUT /api/v1/charges/{id}
LoanProduct: PUT /api/v1/loanproducts/{id}
SavingsProduct: PUT /api/v1/savingsproducts/{id}
Client: PUT /api/v1/clients/{id}
Group: PUT /api/v1/groups/{id}
Center: PUT /api/v1/centers/{id}
SavingsAccount: Cannot update after activation (business rule)
LoanAccount: Cannot update after disbursement (business rule)
```

#### 2.4 Handle Immutable Fields
Some entities can't be updated after certain states:
- Activated accounts can't change product
- Disbursed loans can't change principal
- Need to detect and warn about these cases

**Estimated Effort:** 1-2 weeks
- ChangeDetectionService: 2 days
- Update 20 entity loaders (accounts excluded): 1 week
- Immutable field handling: 2 days
- Testing: 3 days

---

### 3. Additional Entity Types 🔴

**Status:** Not started - 6 entity types remaining

#### 3.1 Staff Management

**Priority:** HIGH (referenced by many entities)

**Files to Create:**
- `model/organization/Staff.java`
- `service/loader/StaffLoader.java`

**YAML Structure:**
```yaml
staff:
  - externalId: "STAFF-001"
    officeName: "Head Office"
    firstName: "John"
    lastName: "Doe"
    isLoanOfficer: true
    isActive: true
    joiningDate: [2024, 1, 15]
```

**API Endpoints:**
- GET `/api/v1/staff?externalId={id}`
- POST `/api/v1/staff`
- PUT `/api/v1/staff/{id}`

**Effort:** 1 day

#### 3.2 Holidays

**Priority:** MEDIUM (affects loan schedules)

**Files to Create:**
- `model/organization/Holiday.java`
- `service/loader/HolidayLoader.java`

**YAML Structure:**
```yaml
holidays:
  - name: "New Year 2024"
    fromDate: [2024, 1, 1]
    toDate: [2024, 1, 1]
    officeNames: ["Head Office", "Branch 1"]
    repaymentSchedulingRule: "NEXT_WORKING_DAY"
    description: "New Year Holiday"
```

**API Endpoints:**
- GET `/api/v1/holidays?officeId={id}`
- POST `/api/v1/holidays`
- PUT `/api/v1/holidays/{id}`

**Effort:** 1-2 days

#### 3.3 Tellers

**Priority:** LOW (optional feature)

**Files to Create:**
- `model/organization/Teller.java`
- `model/organization/Cashier.java`
- `service/loader/TellerLoader.java`
- `service/loader/CashierLoader.java`

**YAML Structure:**
```yaml
tellers:
  - name: "Main Teller"
    officeName: "Head Office"
    description: "Primary teller station"
    startDate: [2024, 1, 1]
    status: "ACTIVE"
    cashiers:
      - staffExternalId: "STAFF-001"
        description: "Morning shift cashier"
        startDate: [2024, 1, 1]
        endDate: [2024, 12, 31]
```

**Effort:** 2 days

#### 3.4 Share Products

**Priority:** MEDIUM (financial product)

**Files to Create:**
- `model/product/ShareProduct.java`
- `service/loader/ShareProductLoader.java`

**YAML Structure:**
```yaml
shareProducts:
  - name: "Member Shares"
    shortName: "MSHARE"
    description: "Membership share product"
    currencyCode: "USD"
    digitsAfterDecimal: 2
    totalShares: 100000
    sharesIssued: 0
    unitPrice: 10.00
    capitalAccountCode: "20000"
    shareReferenceAccountCode: "20100"
    accountingRule: "CASH_BASED"
```

**API Endpoints:**
- GET `/api/v1/products/share`
- POST `/api/v1/products/share`
- PUT `/api/v1/products/share/{id}`

**Effort:** 2-3 days (similar complexity to loan/savings products)

#### 3.5 Fixed Deposit Products

**Priority:** MEDIUM (financial product)

**Files to Create:**
- `model/product/FixedDepositProduct.java`
- `service/loader/FixedDepositProductLoader.java`

**YAML Structure:**
```yaml
fixedDepositProducts:
  - name: "12-Month Fixed Deposit"
    shortName: "FD12M"
    currencyCode: "USD"
    interestRate: 5.0
    minDepositAmount: 1000.00
    maxDepositAmount: 100000.00
    minDepositTerm: 12
    maxDepositTerm: 12
    inMultiplesOfDepositTerm: 1
    depositPeriodFrequency: "MONTHS"
    preClosurePenalApplicable: true
    preClosurePenalInterest: 1.0
    accountingRule: "CASH_BASED"
```

**Effort:** 2-3 days

#### 3.6 Recurring Deposit Products

**Priority:** MEDIUM (financial product)

**Files to Create:**
- `model/product/RecurringDepositProduct.java`
- `service/loader/RecurringDepositProductLoader.java`

**YAML Structure:**
```yaml
recurringDepositProducts:
  - name: "Monthly Recurring Deposit"
    shortName: "RD1M"
    currencyCode: "USD"
    interestRate: 4.5
    recurringDepositAmount: 100.00
    recurringDepositFrequency: 1
    recurringDepositFrequencyType: "MONTHS"
    minDepositTerm: 12
    maxDepositTerm: 60
    accountingRule: "CASH_BASED"
```

**Effort:** 2-3 days

**Total Effort for Additional Entities:** 2-3 weeks

---

### 4. Enhanced Export Functionality 🚧

**Status:** Basic implementation exists, needs enhancements

**Current Limitations:**
- Phase 1 (system config) not exported
- Users not exported
- Financial activity mappings not exported
- Fund sources not exported
- Groups/Centers not exported
- Accounts not exported
- Transactions not exported (intentional - operational data)

**Required Work:**

#### 4.1 Complete Phase 1 Export
Add to `ExportService.exportSystemConfig()`:
```java
private void exportSystemConfig(Map<String, Object> config) {
  // Global configurations
  List<Map<String, Object>> globalConfigs =
      apiClient.get("/api/v1/configurations", List.class);
  if (globalConfigs != null && !globalConfigs.isEmpty()) {
    config.put("globalConfigurations", processGlobalConfigs(globalConfigs));
  }

  // Codes and code values
  List<Map<String, Object>> codes = apiClient.get("/api/v1/codes", List.class);
  if (codes != null && !codes.isEmpty()) {
    config.put("codes", processCodes(codes));
  }

  // Hooks, external services, etc.
  // ... similar pattern
}
```

#### 4.2 Add Missing Phase 2 Exports
```java
private void exportSecurityAndOrganization(Map<String, Object> config) {
  // ... existing office/role export

  // Add users export
  List<Map<String, Object>> users = apiClient.get("/api/v1/users", List.class);
  if (users != null && !users.isEmpty()) {
    List<Map<String, Object>> exportedUsers = new ArrayList<>();
    for (Map<String, Object> user : users) {
      if (Boolean.TRUE.equals(user.get("deleted"))) continue;

      Map<String, Object> userData = new LinkedHashMap<>();
      userData.put("username", user.get("username"));
      userData.put("firstName", user.get("firstname"));
      userData.put("lastName", user.get("lastname"));
      userData.put("email", user.get("email"));
      userData.put("officeName", user.get("officeName"));
      userData.put("roles", extractRoleNames(user.get("selectedRoles")));
      exportedUsers.add(userData);
    }
    config.put("users", exportedUsers);
  }
}
```

#### 4.3 Add Phase 3 Missing Exports
```java
// Financial Activity Mappings
List<Map<String, Object>> mappings =
    apiClient.get("/api/v1/financialactivityaccounts", List.class);

// Fund Sources
List<Map<String, Object>> fundSources =
    apiClient.get("/api/v1/fundsources", List.class);
```

#### 4.4 Add Phase 5 Exports
```java
private void exportClientOperations(Map<String, Object> config) {
  // ... existing clients export

  // Add groups
  List<Map<String, Object>> groups = apiClient.get("/api/v1/groups", List.class);
  // Process groups...

  // Add centers
  List<Map<String, Object>> centers = apiClient.get("/api/v1/centers", List.class);
  // Process centers...

  // Add savings accounts (optional - may be operational data)
  // Add loan accounts (optional - may be operational data)
}
```

#### 4.5 Export Filtering Options
Add parameters to export command:
```java
@ShellMethod(key = "export", value = "Export Fineract configuration to YAML file")
public String export(
    @ShellOption(help = "Output YAML file path") String output,
    @ShellOption(help = "Phases to export (1-6 or 'all')", defaultValue = "all") String phases,
    @ShellOption(help = "Include operational data (accounts, transactions)", defaultValue = "false") boolean includeOperational,
    @ShellOption(help = "Entity types to export (comma-separated)", defaultValue = "all") String entityTypes)
```

**Estimated Effort:** 1 week
- Complete Phase 1 export: 2 days
- Add missing entities (users, mappings, fund sources): 1 day
- Phase 5 exports (groups, centers, accounts): 2 days
- Export filtering: 1 day
- Testing: 1 day

---

### 5. Comprehensive Documentation 📚

**Status:** Not started

#### 5.1 README.md Enhancement

**Add sections:**
- Quick start guide with Docker
- Prerequisites (Java 17, Maven)
- Building from source
- Running the CLI
- Configuration examples
- Troubleshooting

**Estimated:** 1 day

#### 5.2 USAGE.md (New File)

**Content:**
```markdown
# Fineract Config CLI - Usage Guide

## Installation
...

## Commands

### import
Import configuration from YAML file.

**Syntax:**
```
import --file <path> [--dry-run] [--force]
```

**Examples:**
```
# Import demo configuration
import --file demo-config.yml

# Preview changes without applying
import --file config.yml --dry-run

# Force re-import even if unchanged
import --file config.yml --force
```

... (document all commands)

## Configuration File Structure
... (explain YAML structure)

## Workflow Examples
... (common scenarios)
```

**Estimated:** 2 days

#### 5.3 CONFIGURATION.md (New File)

**Content:** Complete reference for all 40+ entity types

```markdown
# Configuration Reference

## Phase 1: System Configuration

### Global Configurations
...

### Codes and Code Values
...

## Phase 2: Security & Organization

### Offices
**YAML Structure:**
```yaml
offices:
  - name: "Head Office"
    openingDate: [2024, 1, 1]
```

**Fields:**
- `name` (required): Office name
- `openingDate` (required): Opening date as [year, month, day]
- `parentName` (optional): Parent office name for hierarchy
- `externalId` (optional): External identifier

**API Endpoints:**
- GET `/api/v1/offices`
- POST `/api/v1/offices`

**Dependencies:**
- Parent office must exist before child

... (document all 40+ entity types)
```

**Estimated:** 1 week

#### 5.4 ARCHITECTURE.md (New File)

**Content:**
```markdown
# Architecture Documentation

## Overview
Fineract Config CLI follows the Configuration-as-Code pattern...

## Design Patterns

### Loader Pattern
All entity loaders follow a consistent pattern:
1. Check existence
2. Resolve dependencies via ImportContext
3. Build request payload
4. Create/Update entity
5. Register in ImportContext

### Dependency Resolution
ImportContext provides entity ID caching...

### State Management
StateService tracks import history...

## Key Components

### ImportService
Orchestrates the import process...

### FineractApiClient
WebClient-based HTTP client...

### YamlParserService
Parses YAML with Jackson...

## Extension Points

### Adding New Entity Types
1. Create model class in `model/` package
2. Create loader in `service/loader/` package
3. Wire loader into ImportService
4. Add to FineractConfig model
5. Add export support in ExportService
6. Document in CONFIGURATION.md
```

**Estimated:** 2 days

#### 5.5 CONTRIBUTING.md (New File)

**Content:**
- Code style guide (Google Java Format via Spotless)
- Testing requirements (80% coverage)
- Pull request process
- Development setup

**Estimated:** 1 day

#### 5.6 API Documentation (JavaDoc)

**Enhance existing JavaDocs:**
- Add `@param` descriptions for all public methods
- Add `@return` descriptions
- Add `@throws` for exceptions
- Add code examples in class-level docs

**Estimated:** 2 days

**Total Documentation Effort:** 2 weeks

---

### 6. Docker Image & Distribution 🐳

**Status:** Not started

#### 6.1 Create Dockerfile

**File:** `Dockerfile`
```dockerfile
# Multi-stage build for smaller image
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/fineract-config-cli.jar /app/fineract-config-cli.jar

# Create directory for config files
RUN mkdir /config

# Set default environment variables
ENV FINERACT_BASE_URL=http://localhost:8443/fineract-provider \
    FINERACT_USERNAME=mifos \
    FINERACT_PASSWORD=password \
    FINERACT_TENANT=default

# Run CLI in interactive mode
ENTRYPOINT ["java", "-jar", "/app/fineract-config-cli.jar"]
```

**Estimated:** 2 hours

#### 6.2 Docker Compose Example

**File:** `docker-compose.yml`
```yaml
version: '3.8'

services:
  fineract-config-cli:
    build: .
    image: fineract-config-cli:latest
    container_name: fineract-config-cli
    environment:
      FINERACT_BASE_URL: http://fineract:8443/fineract-provider
      FINERACT_USERNAME: mifos
      FINERACT_PASSWORD: password
      FINERACT_TENANT: default
    volumes:
      - ./config:/config
    depends_on:
      - fineract
    stdin_open: true
    tty: true

  # Optional: Include Fineract for local testing
  fineract:
    image: apache/fineract:latest
    ports:
      - "8443:8443"
    # ... Fineract configuration
```

**Estimated:** 1 hour

#### 6.3 Build Scripts

**File:** `build.sh`
```bash
#!/bin/bash
set -e

VERSION=${1:-"1.0.0-SNAPSHOT"}

echo "Building Fineract Config CLI version ${VERSION}..."

# Build JAR
mvn clean package -DskipTests

# Build Docker image
docker build -t fineract-config-cli:${VERSION} .
docker tag fineract-config-cli:${VERSION} fineract-config-cli:latest

echo "Build complete!"
echo "Run with: docker run -it fineract-config-cli:${VERSION}"
```

**Estimated:** 30 minutes

#### 6.4 Distribution

**GitHub Releases:**
1. Create release workflow (`.github/workflows/release.yml`)
2. Attach JAR to releases
3. Publish Docker image to Docker Hub / GitHub Container Registry

**Maven Central:**
1. Configure `pom.xml` for OSSRH
2. Add GPG signing
3. Deploy to Maven Central

**Estimated:** 1 day (setup + testing)

**Total Docker & Distribution Effort:** 2-3 days

---

## Implementation Priority

### High Priority (MVP for Production Use)
1. **Update Support** (2 weeks) - Critical for real-world usage
2. **Complete Dry-Run Mode** (4 days) - Important for safety
3. **Staff Management Entity** (1 day) - Frequently referenced
4. **Enhanced Export** (1 week) - Complete the import/export cycle

**Total High Priority: 4-5 weeks**

### Medium Priority (Important Features)
5. **Holidays Entity** (2 days) - Affects loan schedules
6. **Share Products** (3 days) - Common financial product
7. **Fixed/Recurring Deposits** (6 days) - Common products
8. **Comprehensive Documentation** (2 weeks) - User onboarding

**Total Medium Priority: 3-4 weeks**

### Low Priority (Nice to Have)
9. **Tellers & Cashiers** (2 days) - Optional feature
10. **Docker Image** (3 days) - Deployment convenience

**Total Low Priority: 1 week**

---

## Total Estimated Timeline

**Full Implementation:** 8-10 weeks (2-2.5 months)

### Sprint Breakdown (2-week sprints)

**Sprint 1-2: Core Updates (4 weeks)**
- Update support implementation
- Complete dry-run mode
- Staff management entity
- Enhanced export functionality

**Sprint 3: Financial Products (2 weeks)**
- Holidays entity
- Share products
- Fixed deposit products
- Recurring deposit products

**Sprint 4-5: Documentation & Distribution (4 weeks)**
- Complete documentation (README, USAGE, CONFIGURATION, ARCHITECTURE)
- Docker image and compose files
- GitHub Actions workflows
- Maven Central setup

**Sprint 6: Testing & Polish (2 weeks)**
- Integration testing
- Bug fixes
- Performance optimization
- Release preparation

---

## Success Metrics

### Functionality
- ✅ All 40+ entity types supported
- ✅ Full CRUD operations (Create, Read, Update, Delete)
- ✅ Idempotent imports
- ✅ Dry-run preview
- ✅ Export functionality

### Quality
- ✅ 80%+ code coverage
- ✅ Google Java Style compliance
- ✅ Zero Spotless violations
- ✅ Integration tests for all loaders

### Usability
- ✅ Comprehensive documentation
- ✅ Docker support
- ✅ Clear error messages
- ✅ Interactive CLI

### Distribution
- ✅ Maven Central publication
- ✅ Docker Hub image
- ✅ GitHub releases with binaries

---

## Getting Started with Remaining Work

### For Update Support (Start Here)
1. Create `ChangeDetectionService.java`
2. Update `OfficeLoader.java` as proof of concept
3. Test update workflow
4. Replicate pattern to remaining 22 loaders

### For Dry-Run Mode
1. Update `ImportService.executeLoaders()` signature
2. Update `OfficeLoader.load()` as proof of concept
3. Test dry-run output
4. Replicate to all loaders

### For Additional Entities
1. Start with Staff (highest priority)
2. Create model and loader
3. Wire into ImportService
4. Add demo configuration
5. Test end-to-end

---

## Notes

- **Code Quality:** Maintain 80%+ test coverage for all new code
- **Documentation:** Update CONFIGURATION.md as new entities are added
- **Breaking Changes:** Avoid breaking changes to YAML format
- **Backward Compatibility:** Support importing old YAML files
- **Performance:** Consider parallel processing for large imports (already configured in ImportProperties)

---

## Questions to Resolve

1. **Delete Unmanaged Resources:** Should the tool delete entities not in YAML? (ManagedResourceMode.FULL vs NO_DELETE)
2. **Operational Data:** Should accounts/transactions be exported by default?
3. **Versioning:** How to handle Fineract API version differences?
4. **State Storage:** Should state be stored in Fineract DB or external file?
5. **Multi-Tenant:** How to handle multiple tenants in one YAML?

---

*Last Updated: 2025-11-20*
*Current Version: 1.0.0-SNAPSHOT*
*Completed Phases: 6/6 (100%)*
*Completed Enhancements: 4/10 (40%)*
