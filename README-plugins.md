# Fineract with Embedded Pentaho Plugin

This directory contains the implementation of **Image Embedding** approach for deploying Fineract with the Pentaho reporting plugin.

## Files Created

- **`Dockerfile.plugins`**: Dockerfile that builds Fineract with embedded Pentaho plugin
- **`docker-compose-plugins.yml`**: Docker Compose configuration using the plugin-embedded image
- **Modified `.github/workflows/publish-ghcr.yml`**: CI/CD workflow that builds and publishes both base and plugin-enhanced images

## Quick Start

### Using Docker Compose

1. **Build and run with plugins embedded**:
   ```bash
   docker compose -f docker-compose-plugins.yml up -d
   ```

2. **Check that Fineract is running**:
   ```bash
   curl --insecure https://localhost:443/fineract-provider/actuator/health
   ```

3. **Test Pentaho reporting**:
   ```bash
   curl --location --request GET 'https://localhost:443/fineract-provider/api/v1/runreports/Expected%20Payments%20By%20Date%20-%20Formatted?tenantIdentifier=default&locale=en&dateFormat=dd%20MMMM%20yyyy&R_startDate=01%20January%202022&R_endDate=02%20January%202023&R_officeId=1&output-type=PDF&R_loanOfficerId=-1' \
     --header 'Fineract-Platform-TenantId: default' \
     --header 'Authorization: Basic bWlmb3M6cGFzc3dvcmQ=' -k -o report.pdf
   ```

### Manual Docker Build

1. **Build the image**:
   ```bash
   docker build -f Dockerfile.plugins -t fineract-with-plugins:latest .
   ```

2. **Run the container**:
   ```bash
   docker run -p 8443:8443 \
     -e FINERACT_DEFAULT_TENANT_ID=default \
     -e FINERACT_HIKARI_PASSWORD=mysql \
     fineract-with-plugins:latest
   ```

## CI/CD Integration

The GitHub Actions workflow now automatically:

1. **Builds the base Fineract image** and pushes to `ghcr.io/adorsys-gis/fineract`
2. **Builds the plugin-enhanced image** and pushes to `ghcr.io/adorsys-gis/fineract:with-plugins`

Available tags:
- `ghcr.io/adorsys-gis/fineract:latest` (base image)
- `ghcr.io/adorsys-gis/fineract:with-plugins-latest` (with plugins)
- `ghcr.io/adorsys-gis/fineract:<commit-hash>` (base image)
- `ghcr.io/adorsys-gis/fineract:with-plugins-<commit-hash>` (with plugins)

## Key Differences from Volume Mounting

### Advantages
- **Self-contained**: No external file dependencies
- **Version consistency**: Plugin version tied to image version
- **Deployment simplicity**: Single image to deploy
- **Performance**: No volume mount overhead

### Trade-offs
- **Rebuild required**: Plugin updates require image rebuild
- **Larger images**: Plugin files increase image size
- **Less flexibility**: Cannot update plugins without rebuilding

## Plugin Configuration

The embedded setup uses these environment variables:

```bash
JAVA_TOOL_OPTIONS=-Dloader.path=/app/plugins/
FINERACT_PENTAHO_REPORTS_PATH=/pentahoReports/MariaDB/
```

## Troubleshooting

### Plugin Not Loading
```bash
# Check plugin files in container
docker exec -it <container> ls -la /app/plugins/

# Check environment variables
docker exec -it <container> env | grep -E "(LOADER|PENTAHO)"
```

### Report Generation Issues
```bash
# Check Fineract logs
docker logs <container> | grep -i pentaho

# Verify report path
docker exec -it <container> ls -la /pentahoReports/MariaDB/
```

## Migration from Volume Mounting

To migrate from the current volume-based setup:

1. **Stop current containers**:
   ```bash
   docker compose down
   ```

2. **Switch to new compose file**:
   ```bash
   docker compose -f docker-compose-plugins.yml up -d
   ```

3. **Remove old volume mounts** (no longer needed)

## Future Enhancements

- **Plugin versioning**: Tag images with plugin versions
- **Multi-plugin support**: Support for additional plugins beyond Pentaho
- **Plugin configuration**: Environment-based plugin configuration
- **Health checks**: Plugin-specific health endpoints