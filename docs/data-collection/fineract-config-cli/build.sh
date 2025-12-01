#!/bin/bash

##############################################################################
# Fineract Config CLI - Build Script
#
# This script builds the Fineract Config CLI application and Docker image.
#
# Usage:
#   ./build.sh [VERSION] [OPTIONS]
#
# Arguments:
#   VERSION     Optional version tag (default: 1.0.0-SNAPSHOT)
#
# Options:
#   --skip-tests        Skip running tests during build
#   --no-cache          Build Docker image without using cache
#   --push              Push Docker image to registry after build
#   --platform          Specify platform (e.g., linux/amd64,linux/arm64)
#
# Examples:
#   ./build.sh                          # Build with default version
#   ./build.sh 1.0.0                    # Build with specific version
#   ./build.sh 1.0.0 --skip-tests       # Build and skip tests
#   ./build.sh 1.0.0 --push             # Build and push to registry
##############################################################################

set -e  # Exit on error
set -o pipefail  # Exit on pipe failure

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
VERSION=${1:-"1.0.0-SNAPSHOT"}
SKIP_TESTS=false
NO_CACHE=false
PUSH=false
PLATFORM=""
IMAGE_NAME="fineract-config-cli"
REGISTRY=""  # Set to your Docker registry (e.g., "ghcr.io/apache/fineract")

# Parse command line arguments
shift || true
while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --no-cache)
      NO_CACHE=true
      shift
      ;;
    --push)
      PUSH=true
      shift
      ;;
    --platform)
      PLATFORM="$2"
      shift 2
      ;;
    --registry)
      REGISTRY="$2"
      shift 2
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Print banner
echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║         Fineract Config CLI - Build Script                ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Print build configuration
echo -e "${YELLOW}Build Configuration:${NC}"
echo "  Version:        ${VERSION}"
echo "  Skip Tests:     ${SKIP_TESTS}"
echo "  No Cache:       ${NO_CACHE}"
echo "  Push:           ${PUSH}"
echo "  Platform:       ${PLATFORM:-default}"
echo "  Registry:       ${REGISTRY:-none}"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed${NC}"
    exit 1
fi

# Step 1: Build JAR with Maven
echo -e "${GREEN}Step 1: Building JAR with Maven...${NC}"
if [ "$SKIP_TESTS" = true ]; then
    mvn clean package -DskipTests
else
    mvn clean package
fi

# Verify JAR was created
if [ ! -f "target/fineract-config-cli.jar" ]; then
    echo -e "${RED}Error: JAR file not found at target/fineract-config-cli.jar${NC}"
    exit 1
fi

echo -e "${GREEN}✓ JAR build successful${NC}"
echo ""

# Step 2: Build Docker image
echo -e "${GREEN}Step 2: Building Docker image...${NC}"

DOCKER_BUILD_ARGS=""

if [ "$NO_CACHE" = true ]; then
    DOCKER_BUILD_ARGS="$DOCKER_BUILD_ARGS --no-cache"
fi

if [ -n "$PLATFORM" ]; then
    DOCKER_BUILD_ARGS="$DOCKER_BUILD_ARGS --platform $PLATFORM"
fi

# Determine full image name
if [ -n "$REGISTRY" ]; then
    FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}"
else
    FULL_IMAGE_NAME="${IMAGE_NAME}"
fi

# Build the image
docker build $DOCKER_BUILD_ARGS \
    -t "${FULL_IMAGE_NAME}:${VERSION}" \
    -t "${FULL_IMAGE_NAME}:latest" \
    .

echo -e "${GREEN}✓ Docker image build successful${NC}"
echo ""

# Step 3: Display image information
echo -e "${GREEN}Step 3: Image Information${NC}"
docker images | grep "${IMAGE_NAME}" | head -3
echo ""

# Step 4: Push to registry (if requested)
if [ "$PUSH" = true ]; then
    if [ -z "$REGISTRY" ]; then
        echo -e "${YELLOW}Warning: --push specified but no registry configured${NC}"
        echo -e "${YELLOW}Skipping push step${NC}"
    else
        echo -e "${GREEN}Step 4: Pushing to registry...${NC}"
        docker push "${FULL_IMAGE_NAME}:${VERSION}"
        docker push "${FULL_IMAGE_NAME}:latest"
        echo -e "${GREEN}✓ Push successful${NC}"
    fi
    echo ""
fi

# Print success message
echo -e "${GREEN}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                  Build Complete!                           ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "${YELLOW}Images created:${NC}"
echo "  - ${FULL_IMAGE_NAME}:${VERSION}"
echo "  - ${FULL_IMAGE_NAME}:latest"
echo ""

echo -e "${YELLOW}Next steps:${NC}"
echo "  1. Test the image:"
echo -e "     ${BLUE}docker run -it ${FULL_IMAGE_NAME}:${VERSION}${NC}"
echo ""
echo "  2. Run with Docker Compose:"
echo -e "     ${BLUE}docker-compose up -d${NC}"
echo ""
echo "  3. Import configuration:"
echo -e "     ${BLUE}docker exec -it fineract-config-cli java -jar /app/fineract-config-cli.jar${NC}"
echo ""

exit 0
