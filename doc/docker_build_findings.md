# Fineract Docker Build and Publishing Investigation

## Current Build Process

Apache Fineract uses the [Jib Gradle plugin](https://github.com/GoogleContainerTools/jib) to build Docker images without requiring a Dockerfile. This approach simplifies the build process and ensures reproducible builds.

### Local Docker Build
To build a Docker image locally:
```bash
./gradlew :fineract-provider:jibDockerBuild -x test
```

This creates a local Docker image named `fineract` (as configured in the workflow).

### CI/CD Build and Test
The `.github/workflows/build-docker.yml` workflow:
- Builds the image using Jib
- Tests it with both MariaDB and PostgreSQL databases
- Does NOT push the image anywhere - it's only for testing

## Current Publishing Process

The `.github/workflows/publish-dockerhub.yml` workflow handles publishing to DockerHub:
- Triggers on pushes to `develop` branch and version tags (`1.*`)
- Uses the `jib` task instead of `jibDockerBuild` to push directly to registry
- Authenticates using DockerHub credentials stored as secrets
- Supports multi-platform builds (linux/amd64, linux/arm64)
- Tags images appropriately (branch name, git hashes for develop)

Key Jib parameters used:
- `-Djib.to.auth.username=${{secrets.DOCKERHUB_USER}}`
- `-Djib.to.auth.password=${{secrets.DOCKERHUB_TOKEN}}`
- `-Djib.from.platforms=linux/amd64,linux/arm64`
- `-Djib.to.image=apache/fineract`
- `-Djib.to.tags=$TAGS`

## Publishing to GitHub Container Registry (ghcr.io)

To publish to ghcr.io instead of DockerHub:

### Authentication
- Use `GITHUB_TOKEN` for authentication (automatically available in GitHub Actions)
- No need for separate username/password secrets

### Image Naming
- Format: `ghcr.io/{owner}/{repository}`
- For our forked repo: `ghcr.io/{our-org}/fineract`

### Required Permissions
- Add `packages: write` to workflow permissions
- The repository must allow packages to be published

### Jib Configuration Changes
- Change `-Djib.to.image` to `ghcr.io/{our-org}/fineract`
- Remove DockerHub auth parameters
- Jib automatically uses GITHUB_TOKEN when publishing to ghcr.io

### Workflow Modifications Needed
1. Update permissions to include `packages: write`
2. Change image name to `ghcr.io/{our-org}/fineract`
3. Remove DockerHub authentication parameters
4. Optionally rename the workflow file to reflect ghcr.io publishing

## Changes Made to Workflow

### Modified File: `.github/workflows/publish-dockerhub.yml`

**Changes:**
1. **Workflow Name**: Changed from "Fineract Publish to DockerHub" to "Fineract Publish to GitHub Container Registry"
2. **Permissions**: Added `packages: write` permission required for publishing to ghcr.io
3. **Image Registry**: Changed `-Djib.to.image` from `apache/fineract` to `ghcr.io/${{ steps.git_hashes.outputs.owner_lowercase }}/fineract`
4. **Authentication**: Added GHCR-specific authentication using `github.actor` and `secrets.GITHUB_TOKEN`
5. **Develocity**: Removed `DEVELOCITY_ACCESS_KEY` environment variable since it's not used in our setup

**Why these changes:**
- **packages: write**: Required by GitHub Actions to publish packages to GitHub Container Registry
- **ghcr.io registry**: Allows our organization to host and control our own Docker images instead of depending on Apache's DockerHub images
- **GHCR authentication**: Uses `github.actor` as username and `secrets.GITHUB_TOKEN` as password for proper authentication to GitHub Container Registry
- **Dynamic owner**: Uses `${{ steps.git_hashes.outputs.owner_lowercase }}` to automatically use the correct organization/user name in lowercase (required for Docker image references)
- **Removed Develocity**: Since we don't use Develocity build acceleration, removed the unnecessary environment variable to avoid potential secret requirements

### Repository Requirements
- Ensure the repository has "Packages" visibility set appropriately (public/private as needed)
- The workflow will now publish images to `ghcr.io/{our-org}/fineract` with appropriate tags
- **Important**: Docker image references cannot contain uppercase letters. The workflow automatically converts the repository owner name to lowercase to comply with Docker naming requirements

## Recommendations

1. **Test the modified workflow** by pushing to develop branch or creating a test tag
2. **Verify image availability** at `ghcr.io/{our-org}/fineract`
3. **Update any documentation** referencing DockerHub images to point to ghcr.io
4. **Consider security implications** of package visibility settings

This approach gives us full control over our Docker images while maintaining the existing build and test infrastructure.