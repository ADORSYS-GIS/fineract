# Fineract Config CLI

**Configuration-as-Code tool for Apache Fineract**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)

Manage your Fineract configuration declaratively using YAML files. Inspired by [keycloak-config-cli](https://github.com/adorsys/keycloak-config-cli), this tool enables GitOps workflows, idempotent operations, and comprehensive entity management for Apache Fineract.

---

## Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [Supported Entities](#supported-entities)
- [Examples](#examples)
- [Docker](#docker)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

---

## Features

### Core Capabilities

✅ **Configuration as Code** - Manage Fineract configuration in version-controlled YAML files
✅ **Idempotent Operations** - Safe to run multiple times with the same configuration
✅ **Complete Entity Coverage** - Support for 40+ Fineract entity types
✅ **Smart Dependency Resolution** - Auto-resolve entity references (`$office.name`, `$glAccount.42`)
✅ **State Management** - Track managed resources, detect changes via checksums
✅ **Flexible Authentication** - Basic Auth and OAuth2 support
✅ **Variable Substitution** - Environment variables, file contents, system properties
✅ **Managed Resource Modes** - Full management or preserve unmanaged resources
✅ **Production Ready** - Docker image, comprehensive logging, error handling

### Advanced Features

🚀 **Dry-Run Mode** - Preview changes without applying
🚀 **Backup Before Import** - Export current state before changes
🚀 **Partial Imports** - Import specific entity types only
🚀 **Multi-Tenant Support** - Manage multiple tenants in parallel
🚀 **Parallel Processing** - Fast imports with concurrent execution
🚀 **Excel Integration** - Convert existing Excel templates to YAML
🚀 **Rollback Capability** - Restore previous configurations

---

## Quick Start

### Prerequisites

- Java 17 or higher
- Apache Fineract instance (v1.8.0+)
- Maven 3.9+ (for building from source)
- Docker (optional, for containerized usage)

### Installation

**Option 1: Download JAR**

```bash
# Download latest release
wget https://github.com/apache/fineract/releases/download/v1.0.0/fineract-config-cli.jar

# Run
java -jar fineract-config-cli.jar --help
```

**Option 2: Build from Source**

```bash
# Clone repository
cd docs/data-collection/fineract-config-cli

# Build
mvn clean package

# Run
java -jar target/fineract-config-cli.jar --help
```

**Option 3: Docker**

```bash
# Pull image
docker pull fineract/config-cli:latest

# Run
docker run --rm fineract/config-cli:latest --help
```

### Basic Usage

```bash
# Import configuration
java -jar fineract-config-cli.jar \
  --fineract.url=http://localhost:8080/fineract-provider \
  --fineract.tenant=default \
  --fineract.user=mifos \
  --fineract.password=password \
  --import.files=/path/to/config.yml

# Dry-run (preview changes)
java -jar fineract-config-cli.jar \
  --dry-run \
  --import.files=/path/to/config.yml

# Validate YAML
python3 scripts/validate_yaml.py --config /path/to/config.yml
```

---

## Installation

### System Requirements

- **Java**: OpenJDK 17 or later
- **Memory**: Minimum 512MB RAM
- **Disk**: 100MB for application + space for logs
- **Network**: Access to Fineract instance

### Install via Docker

```bash
# Run with environment variables
docker run --rm \
  -e FINERACT_URL=http://fineract:8080/fineract-provider \
  -e FINERACT_TENANT=default \
  -e FINERACT_USER=mifos \
  -e FINERACT_PASSWORD=password \
  -e IMPORT_FILES_LOCATIONS=/config/* \
  -v $(pwd)/config:/config \
  fineract/config-cli:latest
```

### Install from Source

```bash
# Clone Fineract repository
git clone https://github.com/apache/fineract.git
cd fineract/docs/data-collection/fineract-config-cli

# Build
mvn clean package -DskipTests

# Verify
java -jar target/fineract-config-cli.jar --version
```

---

## Usage

### Command Line Interface

```bash
java -jar fineract-config-cli.jar [OPTIONS]

Options:
  --fineract.url=<url>              Fineract base URL
  --fineract.tenant=<tenant>        Tenant identifier (default: default)
  --fineract.user=<username>        Username for authentication
  --fineract.password=<password>    Password for authentication
  --fineract.ssl-verify=<boolean>   Enable SSL verification (default: true)

  --import.files=<path>             Path to YAML config file(s) (glob supported)
  --import.validate=<boolean>       Validate before import (default: true)
  --import.parallel=<boolean>       Enable parallel imports (default: false)

  --dry-run                         Preview changes without applying
  --backup                          Export current state before import

  --help                            Display help information
  --version                         Display version information
```

### Environment Variables

All command-line options can be set via environment variables:

```bash
# Fineract connection
export FINERACT_URL=http://localhost:8080/fineract-provider
export FINERACT_TENANT=default
export FINERACT_USER=mifos
export FINERACT_PASSWORD=password
export FINERACT_SSL_VERIFY=true

# Authentication
export FINERACT_AUTH_TYPE=basic  # or oauth2

# OAuth2 (if using)
export FINERACT_AUTH_OAUTH2_TOKEN_URL=http://keycloak:9000/realms/fineract/protocol/openid-connect/token
export FINERACT_AUTH_OAUTH2_CLIENT_ID=fineract-client
export FINERACT_AUTH_OAUTH2_CLIENT_SECRET=secret

# Import configuration
export IMPORT_FILES_LOCATIONS=/config/*.yml
export IMPORT_VALIDATE=true
export IMPORT_PARALLEL=false

# State management
export IMPORT_REMOTE_STATE_ENABLED=true
export IMPORT_MANAGED_OFFICE=full
export IMPORT_MANAGED_CLIENT=no-delete

# Variable substitution
export IMPORT_VAR_SUBSTITUTION_ENABLED=true

# Run
java -jar fineract-config-cli.jar
```

### Configuration File

Create `application.yml` for persistent configuration:

```yaml
fineract:
  url: http://localhost:8080/fineract-provider
  tenant: default
  user: mifos
  password: ${env:FINERACT_PASSWORD}
  ssl-verify: true

  auth:
    type: basic  # or oauth2

import:
  files:
    locations: /config/*.yml
  validate: true
  parallel: false

  remote-state:
    enabled: true

  managed:
    office: full
    staff: full
    client: no-delete
    loanProduct: full
    savingsProduct: full

logging:
  level:
    root: INFO
    org.apache.fineract.config: DEBUG
```

---

## Configuration

### YAML Structure

```yaml
# Minimal configuration example
tenant: default

# Phase 1: System Configuration
systemConfig:
  currency:
    code: USD
    name: US Dollar
    decimalPlaces: 2

  workingDays:
    - MONDAY
    - TUESDAY
    - WEDNESDAY
    - THURSDAY
    - FRIDAY

# Phase 2: Organizational Structure
offices:
  - name: Head Office
    externalId: HO001
    openingDate: "2024-01-01"

staff:
  - firstname: John
    lastname: Doe
    office: $office.Head Office
    username: john.doe
    createUser: true

# Phase 3: Accounting
chartOfAccounts:
  - name: Cash
    glCode: "1000"
    type: ASSET
    usage: DETAIL

# Phase 4: Products
loanProducts:
  - name: Personal Loan
    shortName: PL
    currencyCode: USD
    principal: 10000
    numberOfRepayments: 12
    interestRate: 12.0
```

See [YAML_SCHEMA.md](docs/YAML_SCHEMA.md) for complete reference.

### Dependency Resolution

Use `$<entity-type>.<identifier>` to reference entities:

```yaml
staff:
  - firstname: Jane
    lastname: Smith
    office: $office.Head Office        # Resolves to office ID
    role: $role.Branch Manager          # Resolves to role ID

loanProducts:
  - name: SME Loan
    charges:
      - $charge.Processing Fee          # Resolves to charge ID
      - $charge.Late Payment Penalty
    accounting:
      fundSource: $glAccount.1000       # Resolves to GL account ID
      loanPortfolio: $glAccount.1100
```

### Variable Substitution

```yaml
globalConfig:
  - name: amazon-S3_access_key
    value: ${env:AWS_ACCESS_KEY}        # Environment variable

  - name: smtp_password
    value: ${file:/run/secrets/smtp}    # File content

  - name: api_url
    value: ${sys:fineract.url}          # System property

# Recursive substitution supported
  - name: db_password
    value: ${file:${env:PASSWORD_FILE}} # Read file path from env
```

### Managed Resource Modes

Control how the tool manages resources:

**Full Mode** (default):
- Creates new resources from YAML
- Updates existing resources if changed
- **Deletes** resources not in YAML (if previously created by tool)

**No-Delete Mode**:
- Creates new resources from YAML
- Updates existing resources if changed
- **Preserves** resources not in YAML (no deletion)

```properties
# Per-entity configuration
import.managed.office=full           # Fully manage offices
import.managed.client=no-delete      # Preserve clients not in YAML
import.managed.loanProduct=full      # Fully manage loan products
```

---

## Supported Entities

### Entity Categories (40 total)

| Category | Entities | Count |
|----------|----------|-------|
| **System Config** | Currency, Working Days, Global Config, Codes, Account Numbering, SMS/Email, Notifications, Data Tables | 8 |
| **Security & Org** | Roles, Offices, Staff, Tellers | 4 |
| **Accounting** | Chart of Accounts, Financial Mappings, Teller Rules, Maker-Checker, Jobs, Provisioning | 6 |
| **Products** | Floating Rates, Tax Groups, Charges, Funds, Payment Types, Holidays, Loan Products, Savings Products, Collateral, Delinquency | 13 |
| **Accounts** | Clients, Savings Accounts, Loan Accounts, Collateral, Guarantors | 5 |
| **Transactions** | Deposits, Withdrawals, Repayments, Transfers | 4 |

See [ENTITY_REFERENCE.md](docs/ENTITY_REFERENCE.md) for complete details.

---

## Examples

### Example 1: Minimal Setup

```yaml
# minimal-config.yml
tenant: default

systemConfig:
  currency:
    code: USD
    name: US Dollar
    decimalPlaces: 2

offices:
  - name: Main Office
    externalId: MAIN
    openingDate: "2024-01-01"
```

```bash
java -jar fineract-config-cli.jar \
  --import.files=minimal-config.yml
```

### Example 2: Complete Demo Data

```yaml
# complete-demo.yml
tenant: default

# Import all 40 entity types
# See config-examples/complete-demo.yml for full example

systemConfig:
  currency: { code: XAF, name: Central African CFA Franc, decimalPlaces: 0 }
  workingDays: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]

roles:
  - name: Branch Manager
    permissions:
      - ALL_FUNCTIONS_READ_CLIENT
      - ALL_FUNCTIONS_LOAN

offices:
  - name: Head Office
    externalId: HO001
    openingDate: "2024-01-01"
  - name: Douala Branch
    externalId: DLA001
    parentOffice: $office.Head Office
    openingDate: "2024-02-01"

chartOfAccounts:
  - name: Cash
    glCode: "42"
    type: ASSET
    usage: DETAIL

loanProducts:
  - name: Microcredit Loan
    shortName: MSOL
    currencyCode: XAF
    principal: 500000
    numberOfRepayments: 12
    interestRate: 24.0
    accounting:
      fundSource: $glAccount.41
      loanPortfolio: $glAccount.51

clients:
  - firstname: Marie
    lastname: Ngono
    office: $office.Douala Branch
    gender: Female
    mobile: "+237677123456"
```

### Example 3: Convert Excel to YAML

```bash
# Convert existing Excel demo data to YAML
python3 scripts/excel_to_yaml.py \
  --input ../fineract-demo-data/output/fineract_demo_data.xlsx \
  --output demo-config.yml

# Validate generated YAML
python3 scripts/validate_yaml.py --config demo-config.yml

# Import
java -jar fineract-config-cli.jar --import.files=demo-config.yml
```

### Example 4: Dry-Run and Backup

```bash
# Preview changes
java -jar fineract-config-cli.jar \
  --dry-run \
  --import.files=new-config.yml

# Review output, then apply with backup
java -jar fineract-config-cli.jar \
  --backup \
  --import.files=new-config.yml
```

---

## Docker

### Quick Start with Docker

The easiest way to run Fineract Config CLI is using our convenience scripts:

```bash
# Start all services (Fineract, MySQL, CLI)
./run.sh start

# Import configuration
./run.sh import config/demo.yml

# Interactive CLI session
./run.sh cli

# Export configuration
./run.sh export data/export.yml

# View logs
./run.sh logs

# Stop services
./run.sh stop
```

### Docker Run

Pull and run the latest image from GitHub Container Registry:

```bash
# Pull image
docker pull ghcr.io/apache/fineract/fineract-config-cli:latest

# Run with environment variables
docker run --rm -it \
  -e FINERACT_BASE_URL=http://fineract:8443/fineract-provider \
  -e FINERACT_USERNAME=mifos \
  -e FINERACT_PASSWORD=password \
  -e FINERACT_TENANT=default \
  -v $(pwd)/config:/config \
  -v $(pwd)/data:/data \
  ghcr.io/apache/fineract/fineract-config-cli:latest
```

### Docker Compose (Full Stack)

Our `docker-compose.yml` includes Fineract, MySQL, and the Config CLI:

```yaml
version: '3.8'

services:
  # MySQL Database
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: mysql
      MYSQL_DATABASE: fineract_tenants
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-pmysql"]
      interval: 10s
      timeout: 5s
      retries: 10

  # Apache Fineract
  fineract:
    image: apache/fineract:latest
    environment:
      FINERACT_HIKARI_JDBC_URL: jdbc:mariadb://mysql:3306/fineract_tenants
      FINERACT_HIKARI_USERNAME: root
      FINERACT_HIKARI_PASSWORD: mysql
      FINERACT_DEFAULT_TENANTDB_HOSTNAME: mysql
      FINERACT_DEFAULT_TENANTDB_PWD: mysql
    ports:
      - "8443:8443"
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/fineract-provider/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 10

  # Fineract Config CLI
  fineract-config-cli:
    build: .
    image: fineract-config-cli:latest
    environment:
      FINERACT_BASE_URL: http://fineract:8443/fineract-provider
      FINERACT_TENANT: default
      FINERACT_USERNAME: mifos
      FINERACT_PASSWORD: password
      JAVA_OPTS: -Xmx512m -Xms256m
    volumes:
      - ./config:/config
      - ./data:/data
    depends_on:
      fineract:
        condition: service_healthy
    stdin_open: true
    tty: true

volumes:
  mysql-data:
```

**Usage**:

```bash
# Start all services
docker-compose up -d

# Wait for Fineract to be healthy (60-90 seconds)
docker-compose logs -f fineract

# Import configuration
docker exec -it fineract-config-cli java -jar /app/fineract-config-cli.jar

# Or use run script
./run.sh import config/demo.yml

# Stop services
docker-compose down

# Clean up (including volumes)
docker-compose down -v
```

### Build Docker Image

#### Using Build Script (Recommended)

```bash
# Build with default version
./build.sh

# Build with specific version
./build.sh 1.0.0

# Build and skip tests
./build.sh 1.0.0 --skip-tests

# Build without cache
./build.sh 1.0.0 --no-cache

# Build for multiple platforms
./build.sh 1.0.0 --platform linux/amd64,linux/arm64

# Build and push to registry
./build.sh 1.0.0 --push --registry ghcr.io/your-org
```

#### Manual Build

```bash
# Build multi-stage image
docker build -t fineract-config-cli:latest .

# Build with specific version
docker build -t fineract-config-cli:1.0.0 .

# Tag for registry
docker tag fineract-config-cli:1.0.0 ghcr.io/apache/fineract/fineract-config-cli:1.0.0

# Push to registry
docker push ghcr.io/apache/fineract/fineract-config-cli:1.0.0
```

### Docker Image Details

The Docker image is based on a multi-stage build:

- **Build Stage**: Maven 3.9 + OpenJDK 17 (builds the JAR)
- **Runtime Stage**: Eclipse Temurin 17 JRE Alpine (minimal runtime)

**Image Size**: ~250MB (runtime image)

**Features**:
- Non-root user for security
- Health check included
- Volumes for `/config` and `/data`
- Alpine Linux base for minimal footprint
- Multi-platform support (amd64, arm64)

### Environment Variables for Docker

```bash
# Fineract connection
FINERACT_BASE_URL=http://fineract:8443/fineract-provider
FINERACT_TENANT=default
FINERACT_USERNAME=mifos
FINERACT_PASSWORD=password

# Java options
JAVA_OPTS=-Xmx512m -Xms256m

# Spring profiles
SPRING_PROFILES_ACTIVE=dev

# Import settings
IMPORT_VALIDATE=true
IMPORT_FORCE=false
IMPORT_DRY_RUN=false
IMPORT_PARALLEL=false
IMPORT_REMOTE_STATE_ENABLED=true
```

### Docker Volumes

Mount these directories for persistence:

```bash
# Configuration files
-v $(pwd)/config:/config

# Exported data
-v $(pwd)/data:/data

# Custom application.yml (optional)
-v $(pwd)/application.yml:/app/application.yml

# Logs (optional)
-v $(pwd)/logs:/app/logs
```

---

## Documentation

### Complete Documentation

#### Getting Started
- **[README.md](README.md)** - This file (overview and quick start)
- **[USAGE.md](USAGE.md)** - Comprehensive command examples and workflows
- **[CONFIGURATION.md](CONFIGURATION.md)** - Complete reference for all 40+ entities

#### Technical Documentation
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Design patterns and extension points
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development guidelines and standards
- **[ROADMAP.md](ROADMAP.md)** - Development roadmap and completed phases
- **[DOCKER_SETUP.md](DOCKER_SETUP.md)** - Docker deployment guide

#### Feature-Specific Guides
- **[STAFF_IMPLEMENTATION.md](STAFF_IMPLEMENTATION.md)** - Staff management guide
- **[EXPORT_FUNCTIONALITY.md](EXPORT_FUNCTIONALITY.md)** - Export features and usage
- **[UPDATE_SUPPORT_IMPLEMENTATION.md](UPDATE_SUPPORT_IMPLEMENTATION.md)** - Update strategies

### Additional Resources

- [Fineract API Documentation](https://demo.fineract.dev/fineract-provider/api-docs/apiLive.htm)
- [Fineract Demo Data Toolkit](../fineract-demo-data/README.md)
- [Keycloak Config CLI](https://github.com/adorsys/keycloak-config-cli) (inspiration)

---

## Contributing

We welcome contributions! Please see [DEVELOPMENT_GUIDE.md](docs/DEVELOPMENT_GUIDE.md) for details.

### Development Setup

```bash
# Clone repository
git clone https://github.com/apache/fineract.git
cd fineract/docs/data-collection/fineract-config-cli

# Build
mvn clean install

# Run tests
mvn test

# Run integration tests (requires Docker)
mvn verify -Pintegration-tests

# Format code
mvn spotless:apply
```

### Reporting Issues

- **Bugs**: [GitHub Issues](https://github.com/apache/fineract/issues)
- **Feature Requests**: [GitHub Discussions](https://github.com/apache/fineract/discussions)
- **Security**: Email security@apache.org

---

## License

Apache License 2.0 - See [LICENSE](../../../LICENSE) for details.

---

## Changelog

### Version 1.0.0 (Planned)

- Initial release
- Support for 40 entity types
- Idempotent operations
- State management
- Docker support
- Excel → YAML conversion

See [ROADMAP.md](ROADMAP.md) for development progress and completed phases.

---

## Support

- **Documentation**: See `docs/` directory
- **Examples**: See `config-examples/` directory
- **Community**: [Apache Fineract Mailing Lists](https://fineract.apache.org/community.html)
- **Commercial Support**: Contact ADORSYS GIS

---

## Acknowledgments

- Inspired by [keycloak-config-cli](https://github.com/adorsys/keycloak-config-cli)
- Built on [Apache Fineract](https://fineract.apache.org)
- Developed by [ADORSYS GIS](https://www.adorsys.com)

---

**Project Status**: 🟡 Active Development

| Feature | Status |
|---------|--------|
| Core Import/Export | ✅ Complete |
| Staff Management | ✅ Complete |
| Enhanced Export | ✅ Complete |
| Update Support | ✅ Complete (26 loaders) |
| Documentation | 🟡 In Progress |
| Testing | 🔴 Pending |

See [ROADMAP.md](ROADMAP.md) for detailed progress and upcoming enhancements.
