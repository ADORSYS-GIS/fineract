# Fineract Plugin Deployment Strategies

This document explores different approaches for deploying plugins alongside Apache Fineract, with a focus on the Pentaho reporting plugin. It covers volume mounting, image embedding, and CI/CD integration strategies.

## Understanding Fineract Plugins

### Plugin Architecture
Apache Fineract uses Spring Boot's plugin system, where plugins are loaded via the `loader.path` JVM property. Plugins are typically JAR files containing additional functionality that extends Fineract's core capabilities.

### Pentaho Reporting Plugin
The Pentaho plugin consists of:
- **JAR files**: Core plugin libraries loaded via `loader.path`
- **Report templates**: `.prpt` files containing report definitions and SQL queries
- **Configuration files**: Properties files for report parameters

## Deployment Approaches

### 1. Volume Mounting (Current Approach)

**Description**: Mount plugin files as Docker volumes at runtime.

**Advantages**:
- **Easy updates**: Plugin files can be updated without rebuilding the image
- **Development friendly**: Quick iteration during development
- **Resource efficient**: No need to rebuild images for plugin changes

**Disadvantages**:
- **Dependency on host files**: Requires plugin files to exist on the host
- **Less portable**: Deployment requires external file management
- **Potential sync issues**: Volume mounts can have performance implications

**Implementation**:
```yaml
services:
  fineract:
    image: apache/fineract:latest
    volumes:
      - ./fineract-pentaho/MifosSecurityPlugin-1.12.1:/app/plugins
      - ./fineract-pentaho/MifosSecurityPlugin-1.12.1:/pentahoReports
    environment:
      - JAVA_TOOL_OPTIONS=-Dloader.path=/app/plugins/
      - FINERACT_PENTAHO_REPORTS_PATH=/pentahoReports/MariaDB/
```

### 2. Image Embedding (Recommended for Production)

**Description**: Build plugin files directly into the Docker image.

**Advantages**:
- **Self-contained**: Image contains everything needed to run
- **Portable**: Easy deployment across environments
- **Performance**: No volume mount overhead
- **Version consistency**: Plugin version tied to image version

**Disadvantages**:
- **Rebuild required**: Any plugin change requires image rebuild
- **Larger images**: Plugin files increase image size
- **CI/CD complexity**: Requires plugin files in build context

**Implementation**:
```dockerfile
FROM apache/fineract:latest

# Copy plugin files into the image
COPY fineract-pentaho/MifosSecurityPlugin-1.12.1 /app/plugins
COPY fineract-pentaho/MifosSecurityPlugin-1.12.1 /pentahoReports

# Set environment variables
ENV JAVA_TOOL_OPTIONS="-Dloader.path=/app/plugins/"
ENV FINERACT_PENTAHO_REPORTS_PATH="/pentahoReports/MariaDB/"
```

### 3. Hybrid Approach (Recommended)

**Description**: Embed core plugin JARs in the image, mount report templates as volumes.

**Advantages**:
- **Core stability**: Plugin JARs are version-controlled and immutable
- **Template flexibility**: Report templates can be updated without rebuilds
- **Balanced performance**: Core functionality is optimized, templates remain flexible

**Disadvantages**:
- **Mixed management**: Different update strategies for different components
- **Configuration complexity**: Managing both embedded and mounted components

**Implementation**:
```dockerfile
FROM apache/fineract:latest

# Embed plugin JARs in the image
COPY fineract-pentaho/MifosSecurityPlugin-1.12.1 /app/plugins

# Set core environment
ENV JAVA_TOOL_OPTIONS="-Dloader.path=/app/plugins/"
```

```yaml
services:
  fineract:
    build: .
    volumes:
      # Mount only report templates
      - ./report-templates:/pentahoReports
    environment:
      - FINERACT_PENTAHO_REPORTS_PATH=/pentahoReports/MariaDB/
```

## CI/CD Integration Strategies

### 1. Multi-Stage Plugin Build

**Description**: Use GitHub Actions to build plugin-enhanced images.

**Workflow Example**:
```yaml
name: Build Fineract with Plugins
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Download and extract Pentaho plugin
        run: |
          mkdir -p fineract-pentaho
          curl -L -o fineract-pentaho/plugin.zip "https://sourceforge.net/projects/mifos/files/mifos-plugins/MifosReportingPlugin/MifosSecurityPlugin-1.12.1.zip/download"
          unzip fineract-pentaho/plugin.zip -d fineract-pentaho/

      - name: Build custom image
        run: |
          docker build -t ${{ github.repository }}:latest .

      - name: Push to GHCR
        run: |
          echo ${{ secrets.GITHUB_TOKEN }} | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker tag ${{ github.repository }}:latest ghcr.io/${{ github.repository_owner }}/fineract:latest
          docker push ghcr.io/${{ github.repository_owner }}/fineract:latest
```

### 2. Plugin as Separate Service

**Description**: Run plugins in separate containers, communicate via APIs.

**Advantages**:
- **Isolation**: Plugin failures don't affect core Fineract
- **Scaling**: Plugins can be scaled independently
- **Technology flexibility**: Plugins can use different tech stacks

**Disadvantages**:
- **Complexity**: Requires inter-service communication
- **Network overhead**: API calls between services
- **Development complexity**: Managing multiple services

### 3. GitHub Actions Artifact Strategy

**Description**: Store plugins as GitHub Actions artifacts or releases.

**Implementation**:
```yaml
- name: Upload plugin artifact
  uses: actions/upload-artifact@v4
  with:
    name: pentaho-plugin
    path: fineract-pentaho/

- name: Download plugin artifact
  uses: actions/download-artifact@v4
  with:
    name: pentaho-plugin
    path: ./plugins
```

## Best Practices

### 1. Plugin Version Management
- **Semantic versioning**: Tag plugin versions clearly
- **Compatibility matrix**: Document which plugin versions work with which Fineract versions
- **Automated testing**: Include plugin compatibility tests in CI/CD

### 2. Security Considerations
- **Plugin validation**: Scan plugins for vulnerabilities
- **Access control**: Limit plugin access to necessary resources
- **Update strategy**: Regular plugin updates for security patches

### 3. Performance Optimization
- **Layer caching**: Structure Dockerfiles to maximize layer reuse
- **Minimal images**: Use multi-stage builds to reduce image size
- **Resource limits**: Set appropriate CPU/memory limits for plugin containers

### 4. Monitoring and Observability
- **Plugin metrics**: Monitor plugin performance and errors
- **Health checks**: Include plugin health endpoints
- **Logging**: Centralized logging for plugin activities

## Recommendations

### For Development
Use **volume mounting** for rapid iteration and easy plugin updates.

### For Production
Use **image embedding** for the hybrid approach:
- Embed core plugin JARs in the image for stability
- Mount report templates as volumes for flexibility

### For CI/CD
Implement automated plugin building and testing in your pipeline to ensure consistency across environments.

## Migration Strategy

1. **Assess current setup**: Evaluate existing plugin deployment method
2. **Choose target approach**: Select based on your operational requirements
3. **Implement gradually**: Start with non-critical environments
4. **Test thoroughly**: Validate plugin functionality in new setup
5. **Update documentation**: Reflect new deployment approach in team docs

## Troubleshooting

### Common Issues
- **Plugin not loading**: Check `loader.path` configuration
- **Report templates not found**: Verify `FINERACT_PENTAHO_REPORTS_PATH`
- **Permission errors**: Ensure proper file permissions in containers
- **Version conflicts**: Check plugin-Fineract version compatibility

### Debug Commands
```bash
# Check plugin loading
docker exec -it fineract-container ls -la /app/plugins/

# Verify environment variables
docker exec -it fineract-container env | grep -E "(LOADER|PENTAHO)"

# Check Fineract logs for plugin errors
docker logs fineract-container | grep -i plugin