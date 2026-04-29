# Fineract Adorsys Pentaho Integration

This directory contains the necessary components for integrating Pentaho reporting with the Fineract platform.

## Files

-   `MifosSecurityPlugin-1.12.1/`: The extracted directory of the Mifos Security Plugin for Pentaho. This plugin is essential for enabling Fineract to securely connect to and run Pentaho reports.
-   `MifosSecurityPlugin-1.12.1.zip`: The original ZIP archive of the security plugin.

## Usage

In the `docker-compose-adorsys.yml`, the `MifosSecurityPlugin-1.12.1` directory is mounted as a volume into the `fineract` service container. This allows Fineract to access the plugin and the report definitions.

Specifically, it is mounted to:
-   `/app/plugins`: Making the security plugin available to the Fineract application.
-   `/pentahoReports`: Serving as the location for Pentaho report definitions.

The `FINERACT_PENTAHO_REPORTS_PATH` environment variable in the Docker Compose file is then set to point to the appropriate subdirectory within `/pentahoReports`, enabling Fineract to locate and execute the reports.
