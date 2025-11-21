# Docker Setup Guide - Fineract Config CLI

This document provides a comprehensive guide for using Fineract Config CLI with Docker.

## Quick Reference

| File | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage Docker image definition |
| `docker-compose.yml` | Full stack (MySQL + Fineract + CLI) orchestration |
| `.dockerignore` | Optimize Docker build context |
| `build.sh` | Automated build script with options |
| `run.sh` | Convenience script for common operations |
| `.github/workflows/build.yml` | CI/CD for building and testing |
| `.github/workflows/release.yml` | Automated releases with Docker images |
| `.github/workflows/docker-publish.yml` | Docker image publishing to GHCR |

---

## Docker Image

### Image Details

**Registry**: `ghcr.io/apache/fineract/fineract-config-cli`

**Available Tags**:
- `latest` - Latest snapshot build from develop branch
- `1.0.0` - Specific version releases
- `develop-<sha>` - Branch-specific builds
- `snapshot` - Latest snapshot from default branch

**Base Images**:
- Build stage: `maven:3.9-eclipse-temurin-17`
- Runtime stage: `eclipse-temurin:17-jre-alpine`

**Image Size**: ~250MB (runtime image)

**Architecture Support**:
- `linux/amd64`
- `linux/arm64`

### Features

✅ Multi-stage build for minimal size
✅ Non-root user for security (user: `fineract`, uid: 1000)
✅ Health check included
✅ Volumes pre-configured for `/config` and `/data`
✅ Environment variable configuration
✅ Alpine Linux base for minimal footprint
✅ Optimized layer caching

---

## Quick Start

### 1. Using Run Script (Easiest)

```bash
# Start all services
./run.sh start

# Wait for Fineract to be healthy (~60-90 seconds)

# Import configuration
./run.sh import config/phase1-demo-config.yml

# Start interactive CLI
./run.sh cli

# Export configuration
./run.sh export data/export.yml

# View logs
./run.sh logs

# Stop all services
./run.sh stop

# Clean up (removes containers and volumes)
./run.sh clean
```

### 2. Using Docker Compose

```bash
# Start services in background
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f fineract-config-cli

# Execute import
docker exec -it fineract-config-cli java -jar /app/fineract-config-cli.jar

# Stop services
docker-compose stop

# Remove everything (including volumes)
docker-compose down -v
```

### 3. Using Docker Run

```bash
# Pull latest image
docker pull ghcr.io/apache/fineract/fineract-config-cli:latest

# Run interactive session
docker run --rm -it \
  -e FINERACT_BASE_URL=http://fineract:8443/fineract-provider \
  -e FINERACT_USERNAME=mifos \
  -e FINERACT_PASSWORD=password \
  -v $(pwd)/config:/config \
  ghcr.io/apache/fineract/fineract-config-cli:latest
```

---

## Build Script Usage

The `build.sh` script provides comprehensive build options:

### Basic Usage

```bash
# Build with default version (1.0.0-SNAPSHOT)
./build.sh

# Build with specific version
./build.sh 1.0.0

# Build with version 2.0.0-RC1
./build.sh 2.0.0-RC1
```

### Advanced Options

```bash
# Skip Maven tests (faster build)
./build.sh 1.0.0 --skip-tests

# Force rebuild without Docker cache
./build.sh 1.0.0 --no-cache

# Build for multiple platforms
./build.sh 1.0.0 --platform linux/amd64,linux/arm64

# Build and push to registry
./build.sh 1.0.0 --push --registry ghcr.io/your-org

# Combine options
./build.sh 1.0.0 --skip-tests --no-cache --push --registry ghcr.io/your-org
```

### Build Process

The build script performs these steps:

1. **Build JAR**: Runs `mvn clean package` (optionally skipping tests)
2. **Verify JAR**: Checks that `target/fineract-config-cli.jar` exists
3. **Build Docker Image**: Creates image with specified version tag
4. **Tag Images**: Tags both `<version>` and `latest`
5. **Push (Optional)**: Pushes to registry if `--push` specified
6. **Display Info**: Shows created images and next steps

---

## Run Script Commands

The `run.sh` script provides convenient commands for common operations:

### Available Commands

| Command | Description | Example |
|---------|-------------|---------|
| `start` | Start all services (Fineract + MySQL + CLI) | `./run.sh start` |
| `stop` | Stop all services | `./run.sh stop` |
| `restart` | Restart all services | `./run.sh restart` |
| `cli` | Start interactive CLI session | `./run.sh cli` |
| `import` | Import configuration file | `./run.sh import config/demo.yml` |
| `export` | Export configuration | `./run.sh export data/export.yml` |
| `validate` | Validate configuration file | `./run.sh validate config/demo.yml` |
| `logs` | View logs (all services or specific) | `./run.sh logs` |
| `clean` | Stop and remove containers + volumes | `./run.sh clean` |
| `help` | Display help information | `./run.sh help` |

### Command Details

#### Import Command

```bash
# Basic import
./run.sh import config/demo.yml

# Import with dry-run
./run.sh import config/demo.yml --dry-run

# Force import (ignore checksums)
./run.sh import config/demo.yml --force

# Combine flags
./run.sh import config/demo.yml --dry-run --force
```

#### Export Command

```bash
# Export all phases
./run.sh export data/export.yml

# Export specific phases (comma-separated)
./run.sh export data/phases-2-3-4.yml 2,3,4

# Export only system config
./run.sh export data/system.yml 1
```

#### Logs Command

```bash
# View all service logs
./run.sh logs

# View specific service logs
./run.sh logs fineract
./run.sh logs fineract-config-cli
./run.sh logs mysql
```

---

## Docker Compose Stack

The `docker-compose.yml` defines a complete stack with:

### Services

1. **MySQL** (`mysql`)
   - Image: `mysql:8.0`
   - Port: `3306`
   - Database: `fineract_tenants`
   - Health check: MySQL ping
   - Volume: `mysql-data` for persistence

2. **Fineract** (`fineract`)
   - Image: `apache/fineract:latest`
   - Ports: `8443` (HTTPS), `8080` (HTTP)
   - Depends on: MySQL
   - Health check: Actuator endpoint
   - Environment: MySQL connection configured

3. **Config CLI** (`fineract-config-cli`)
   - Build: Current directory (`Dockerfile`)
   - Depends on: Fineract (healthy)
   - Volumes: `./config` → `/config`, `./data` → `/data`
   - Interactive: TTY and stdin enabled
   - Environment: Fineract connection configured

### Networks

- `fineract-network` (bridge): Connects all services

### Volumes

- `mysql-data`: Persists MySQL database

---

## Environment Variables

### Fineract Connection

```bash
FINERACT_BASE_URL=http://fineract:8443/fineract-provider
FINERACT_TENANT=default
FINERACT_USERNAME=mifos
FINERACT_PASSWORD=password
```

### Java Options

```bash
JAVA_OPTS=-Xmx512m -Xms256m
```

### Spring Configuration

```bash
SPRING_PROFILES_ACTIVE=dev
```

### Import Settings

```bash
IMPORT_VALIDATE=true
IMPORT_FORCE=false
IMPORT_DRY_RUN=false
IMPORT_PARALLEL=false
IMPORT_REMOTE_STATE_ENABLED=true
IMPORT_REMOTE_STATE_CHECKSUM_BEHAVIOR=continue
```

### Managed Resource Modes

```bash
IMPORT_MANAGED_OFFICE=full
IMPORT_MANAGED_CLIENT=no-delete
IMPORT_MANAGED_LOANPRODUCT=full
```

---

## GitHub Actions Workflows

### 1. Build and Test (`build.yml`)

**Triggers**:
- Push to: `main`, `develop`, `config-demo`, `yaml-data`
- Pull requests to: `main`, `develop`

**Jobs**:
1. **build**: Maven build, tests, code coverage
2. **docker-build**: Docker image build (no push)
3. **code-quality**: Spotless formatting check

**Artifacts**:
- `fineract-config-cli-jar`: Built JAR file
- `test-results`: Test reports
- Code coverage reports uploaded to Codecov

### 2. Release (`release.yml`)

**Triggers**:
- Tags: `v*.*.*` (e.g., `v1.0.0`)
- Manual workflow dispatch with version input

**Jobs**:
1. **build-and-release**:
   - Build JAR with release version
   - Run tests
   - Generate checksums (SHA256, MD5)
   - Create GitHub Release with notes
   - Attach JAR and checksums

2. **build-and-push-docker**:
   - Build Docker image for `linux/amd64` and `linux/arm64`
   - Push to GitHub Container Registry
   - Tag: `<version>`, `<major>.<minor>`, `<major>`, `latest`

3. **publish-to-maven-central** (disabled):
   - Placeholder for Maven Central publishing
   - Requires OSSRH configuration

**Release Notes**: Auto-generated with:
- Feature list
- Installation instructions (JAR, Docker, Docker Compose)
- Quick start guide
- Documentation links
- Checksums

### 3. Docker Publish (`docker-publish.yml`)

**Triggers**:
- Push to: `main`, `develop`, `yaml-data` (on specific paths)
- Pull requests to: `main`, `develop` (Dockerfile changes)
- Weekly schedule (Mondays at 00:00 UTC)

**Jobs**:
1. **build-and-push**:
   - Build Docker image for multiple platforms
   - Push to GHCR (if not PR)
   - Run Trivy security scanner
   - Upload security results to GitHub Security
   - Test image functionality

**Image Tags**:
- `main` → `main-<sha>`, `snapshot`
- `develop` → `develop-<sha>`
- `yaml-data` → `yaml-data-<sha>`

---

## Security

### Non-Root User

The Docker image runs as a non-root user:
- User: `fineract`
- UID: `1000`
- GID: `1000`

### Trivy Scanning

All images are scanned for vulnerabilities using Trivy:
- Runs on every build
- Results uploaded to GitHub Security tab
- Blocks deployment if critical vulnerabilities found

### Secrets Management

For production, use Docker secrets or environment files:

```bash
# Using environment file
docker run --rm -it \
  --env-file .env.production \
  -v $(pwd)/config:/config \
  ghcr.io/apache/fineract/fineract-config-cli:latest

# Using Docker secrets (Swarm)
docker service create \
  --name fineract-config-cli \
  --secret fineract_password \
  --env FINERACT_PASSWORD_FILE=/run/secrets/fineract_password \
  ghcr.io/apache/fineract/fineract-config-cli:latest
```

---

## Troubleshooting

### Service Not Starting

```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs fineract

# Check health
docker inspect fineract | jq '.[0].State.Health'
```

### Connection Issues

```bash
# Test Fineract connectivity from host
curl http://localhost:8443/fineract-provider/actuator/health

# Test from CLI container
docker exec fineract-config-cli curl http://fineract:8443/fineract-provider/actuator/health
```

### Build Failures

```bash
# Clean Docker build cache
docker builder prune -a

# Rebuild without cache
./build.sh --no-cache

# Check Docker disk space
docker system df
```

### Import Failures

```bash
# Run with dry-run first
./run.sh import config/demo.yml --dry-run

# Check CLI logs
docker-compose logs fineract-config-cli

# Validate YAML
./run.sh validate config/demo.yml
```

---

## Performance Tuning

### Java Memory Settings

```bash
# Increase heap size for large imports
docker run --rm -it \
  -e JAVA_OPTS="-Xmx1g -Xms512m" \
  ghcr.io/apache/fineract/fineract-config-cli:latest
```

### Parallel Imports

```bash
# Enable parallel processing
docker run --rm -it \
  -e IMPORT_PARALLEL=true \
  -e IMPORT_PARALLEL_THREAD_POOL_SIZE=8 \
  ghcr.io/apache/fineract/fineract-config-cli:latest
```

### MySQL Tuning

```yaml
# In docker-compose.yml
mysql:
  command:
    - --max_connections=500
    - --innodb_buffer_pool_size=2G
    - --innodb_log_file_size=512M
```

---

## Production Deployment

### Recommended Setup

1. **Use specific version tags** (not `latest`)
2. **Enable resource limits**:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2'
         memory: 1G
       reservations:
         cpus: '1'
         memory: 512M
   ```

3. **Use Docker secrets** for credentials
4. **Enable logging driver**:
   ```yaml
   logging:
     driver: "json-file"
     options:
       max-size: "10m"
       max-file: "3"
   ```

5. **Set up monitoring** (Prometheus, Grafana)
6. **Configure backups** for MySQL volume
7. **Use health checks** for auto-restart

### Example Production Compose

```yaml
version: '3.8'

services:
  fineract-config-cli:
    image: ghcr.io/apache/fineract/fineract-config-cli:1.0.0
    environment:
      FINERACT_BASE_URL: https://fineract.example.com/fineract-provider
      FINERACT_USERNAME_FILE: /run/secrets/fineract_username
      FINERACT_PASSWORD_FILE: /run/secrets/fineract_password
    secrets:
      - fineract_username
      - fineract_password
    volumes:
      - ./config:/config:ro
      - ./data:/data
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 1G
      restart_policy:
        condition: on-failure
        max_attempts: 3
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

secrets:
  fineract_username:
    external: true
  fineract_password:
    external: true
```

---

## Next Steps

1. **Test the setup**: `./run.sh start`
2. **Import demo data**: `./run.sh import config/phase1-demo-config.yml`
3. **Explore CLI**: `./run.sh cli`
4. **Read documentation**: See `README.md`, `ROADMAP.md`
5. **Customize configuration**: Edit `docker-compose.yml` for your needs

---

**For more information, see:**
- [README.md](README.md) - Main documentation
- [ROADMAP.md](ROADMAP.md) - Development roadmap
- [Dockerfile](Dockerfile) - Image definition
- [docker-compose.yml](docker-compose.yml) - Stack configuration
