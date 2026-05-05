# Fineract Adorsys Keycloak Theme

This directory contains the theme for the Keycloak identity and access management server.

## Files

-   `theme-1.0.1.jar`: The packaged theme that gets mounted into the Keycloak container. This JAR file contains all the necessary resources, such as HTML templates, CSS stylesheets, and images, to customize the look and feel of the Keycloak user interface.

## Usage

The `docker-compose-adorsys.yml` file mounts the `theme-1.0.1.jar` file into the `/opt/keycloak/providers/` directory within the Keycloak container. Keycloak automatically detects and deploys themes from this directory, making it available for selection in the realm settings.

## Modifications

For any changes to the theme, please visit the forked repository: https://github.com/NkwaTambe/adorsys-gis-theme

The official repository for the theme can be found at: https://github.com/ADORSYS-GIS/adorsys-gis-theme
