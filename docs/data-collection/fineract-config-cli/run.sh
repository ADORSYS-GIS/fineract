#!/bin/bash

##############################################################################
# Fineract Config CLI - Run Script
#
# This script provides convenient commands to run the Fineract Config CLI
# using Docker or Docker Compose.
#
# Usage:
#   ./run.sh [COMMAND] [OPTIONS]
#
# Commands:
#   start           Start all services (Fineract + MySQL + CLI)
#   stop            Stop all services
#   restart         Restart all services
#   cli             Start interactive CLI session
#   import          Import configuration file
#   export          Export configuration
#   validate        Validate configuration file
#   logs            View logs
#   clean           Stop and remove all containers and volumes
#
# Examples:
#   ./run.sh start                      # Start all services
#   ./run.sh cli                        # Start CLI session
#   ./run.sh import config/demo.yml     # Import configuration
#   ./run.sh export data/export.yml     # Export configuration
#   ./run.sh validate config/demo.yml   # Validate configuration
#   ./run.sh logs                       # View logs
#   ./run.sh clean                      # Clean up everything
##############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

COMMAND=${1:-"help"}

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not installed${NC}"
    exit 1
fi

# Use 'docker compose' or 'docker-compose' based on availability
DOCKER_COMPOSE="docker-compose"
if ! command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
fi

# Function to print header
print_header() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║         Fineract Config CLI - Run Script                  ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Function to check if services are running
check_services() {
    if ! $DOCKER_COMPOSE ps | grep -q "fineract"; then
        echo -e "${YELLOW}Warning: Fineract services are not running${NC}"
        echo -e "${YELLOW}Run './run.sh start' to start services${NC}"
        return 1
    fi
    return 0
}

# Function to wait for Fineract to be healthy
wait_for_fineract() {
    echo -e "${YELLOW}Waiting for Fineract to be healthy...${NC}"

    MAX_ATTEMPTS=60
    ATTEMPT=0

    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        if docker exec fineract curl -f http://localhost:8080/fineract-provider/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Fineract is healthy${NC}"
            return 0
        fi

        ATTEMPT=$((ATTEMPT + 1))
        echo -n "."
        sleep 5
    done

    echo -e "${RED}Error: Fineract did not become healthy in time${NC}"
    return 1
}

case $COMMAND in
    start)
        print_header
        echo -e "${GREEN}Starting Fineract Config CLI services...${NC}"
        echo ""

        # Start services
        $DOCKER_COMPOSE up -d

        echo ""
        echo -e "${YELLOW}Services starting in background...${NC}"

        # Wait for Fineract to be healthy
        wait_for_fineract

        echo ""
        echo -e "${GREEN}✓ All services started successfully${NC}"
        echo ""
        echo -e "${YELLOW}Access points:${NC}"
        echo "  Fineract API:  http://localhost:8443/fineract-provider"
        echo "  MySQL:         localhost:3306"
        echo ""
        echo -e "${YELLOW}Next steps:${NC}"
        echo "  - Start CLI:      ./run.sh cli"
        echo "  - Import config:  ./run.sh import config/demo.yml"
        echo "  - View logs:      ./run.sh logs"
        ;;

    stop)
        print_header
        echo -e "${GREEN}Stopping Fineract Config CLI services...${NC}"
        $DOCKER_COMPOSE stop
        echo -e "${GREEN}✓ Services stopped${NC}"
        ;;

    restart)
        print_header
        echo -e "${GREEN}Restarting Fineract Config CLI services...${NC}"
        $DOCKER_COMPOSE restart
        wait_for_fineract
        echo -e "${GREEN}✓ Services restarted${NC}"
        ;;

    cli)
        print_header
        check_services || exit 1

        echo -e "${GREEN}Starting interactive CLI session...${NC}"
        echo -e "${YELLOW}Type 'help' to see available commands${NC}"
        echo -e "${YELLOW}Type 'exit' or press Ctrl+D to quit${NC}"
        echo ""

        docker exec -it fineract-config-cli java -jar /app/fineract-config-cli.jar
        ;;

    import)
        print_header
        check_services || exit 1

        CONFIG_FILE=${2:-""}
        if [ -z "$CONFIG_FILE" ]; then
            echo -e "${RED}Error: Configuration file path required${NC}"
            echo "Usage: ./run.sh import <config-file> [--dry-run] [--force]"
            exit 1
        fi

        # Extract filename from path
        FILENAME=$(basename "$CONFIG_FILE")

        # Parse additional options
        DRY_RUN=""
        FORCE=""
        shift 2 || true
        while [[ $# -gt 0 ]]; do
            case $1 in
                --dry-run)
                    DRY_RUN="--dry-run true"
                    shift
                    ;;
                --force)
                    FORCE="--force true"
                    shift
                    ;;
                *)
                    shift
                    ;;
            esac
        done

        echo -e "${GREEN}Importing configuration from: ${CONFIG_FILE}${NC}"

        # Copy file to container if it's not already in /config
        if [[ ! "$CONFIG_FILE" =~ ^/config ]]; then
            docker cp "$CONFIG_FILE" fineract-config-cli:/config/"$FILENAME"
            CONFIG_PATH="/config/$FILENAME"
        else
            CONFIG_PATH="$CONFIG_FILE"
        fi

        # Run import command
        docker exec -it fineract-config-cli java -jar /app/fineract-config-cli.jar \
            --spring.shell.command.import.file="$CONFIG_PATH" \
            $DRY_RUN $FORCE

        echo -e "${GREEN}✓ Import completed${NC}"
        ;;

    export)
        print_header
        check_services || exit 1

        OUTPUT_FILE=${2:-"data/export.yml"}
        PHASES=${3:-"all"}

        echo -e "${GREEN}Exporting configuration to: ${OUTPUT_FILE}${NC}"

        # Run export command
        docker exec fineract-config-cli java -jar /app/fineract-config-cli.jar \
            --spring.shell.command.export.output="/data/$(basename "$OUTPUT_FILE")" \
            --spring.shell.command.export.phases="$PHASES"

        # Copy exported file from container
        docker cp fineract-config-cli:/data/"$(basename "$OUTPUT_FILE")" "$OUTPUT_FILE"

        echo -e "${GREEN}✓ Export completed: ${OUTPUT_FILE}${NC}"
        ;;

    validate)
        print_header

        CONFIG_FILE=${2:-""}
        if [ -z "$CONFIG_FILE" ]; then
            echo -e "${RED}Error: Configuration file path required${NC}"
            echo "Usage: ./run.sh validate <config-file>"
            exit 1
        fi

        FILENAME=$(basename "$CONFIG_FILE")

        echo -e "${GREEN}Validating configuration: ${CONFIG_FILE}${NC}"

        # Copy file to container
        docker cp "$CONFIG_FILE" fineract-config-cli:/config/"$FILENAME"

        # Run validate command
        docker exec fineract-config-cli java -jar /app/fineract-config-cli.jar \
            --spring.shell.command.validate.file="/config/$FILENAME"

        echo -e "${GREEN}✓ Validation completed${NC}"
        ;;

    logs)
        SERVICE=${2:-""}

        if [ -z "$SERVICE" ]; then
            echo -e "${YELLOW}Showing logs for all services (Ctrl+C to stop)...${NC}"
            $DOCKER_COMPOSE logs -f
        else
            echo -e "${YELLOW}Showing logs for ${SERVICE} (Ctrl+C to stop)...${NC}"
            $DOCKER_COMPOSE logs -f "$SERVICE"
        fi
        ;;

    clean)
        print_header
        echo -e "${RED}WARNING: This will stop and remove all containers and volumes${NC}"
        read -p "Are you sure? (yes/no): " -r
        echo

        if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            echo -e "${GREEN}Cleaning up...${NC}"
            $DOCKER_COMPOSE down -v
            echo -e "${GREEN}✓ Cleanup completed${NC}"
        else
            echo -e "${YELLOW}Cleanup cancelled${NC}"
        fi
        ;;

    help|--help|-h)
        print_header
        echo "Usage: ./run.sh [COMMAND] [OPTIONS]"
        echo ""
        echo "Commands:"
        echo "  start           Start all services (Fineract + MySQL + CLI)"
        echo "  stop            Stop all services"
        echo "  restart         Restart all services"
        echo "  cli             Start interactive CLI session"
        echo "  import          Import configuration file"
        echo "  export          Export configuration"
        echo "  validate        Validate configuration file"
        echo "  logs            View logs"
        echo "  clean           Stop and remove all containers and volumes"
        echo ""
        echo "Examples:"
        echo "  ./run.sh start"
        echo "  ./run.sh cli"
        echo "  ./run.sh import config/demo.yml"
        echo "  ./run.sh import config/demo.yml --dry-run"
        echo "  ./run.sh export data/export.yml"
        echo "  ./run.sh export data/export.yml 1,2,3"
        echo "  ./run.sh validate config/demo.yml"
        echo "  ./run.sh logs"
        echo "  ./run.sh logs fineract"
        echo "  ./run.sh clean"
        ;;

    *)
        echo -e "${RED}Error: Unknown command '${COMMAND}'${NC}"
        echo "Run './run.sh help' for usage information"
        exit 1
        ;;
esac

exit 0
