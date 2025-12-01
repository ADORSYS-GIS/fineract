# Fineract Adorsys Keycloak Configuration

This directory contains the Keycloak realm configuration for the Fineract platform.

## Files

-   `realm-export.json`: An export of the `fineract` realm configuration. This file includes all the necessary settings, such as clients, roles, and authentication flows, to properly configure Keycloak for use with Fineract.

## Usage

The `docker-compose-adorsys.yml` file mounts this directory into the `keycloak-config-cli` container, which then imports the `realm-export.json` file to configure the Keycloak server automatically. The `IMPORT_FILES_LOCATIONS` environment variable is set to `/config/`, which is the directory where this file is located inside the container.