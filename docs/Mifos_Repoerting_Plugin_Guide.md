# Mifos Reporting Plugin: A Comprehensive Guide.

This document provides a complete guide to the Mifos Reporting Plugin for Apache Fineract, including installation, testing, usage, and frontend integration.

## 1. Understanding the Reporting Plugin

### 1.1. What is the purpose of this plugin and how does it work?

The Mifos Reporting Plugin integrates the **Pentaho Business Intelligence (BI) suite** with Apache Fineract. Pentaho is a powerful open-source platform for data integration, reporting, and analytics.

**High-Level Workflow:**

1.  **Report Templates:** The plugin uses pre-designed report templates created with the Pentaho Report Designer. These templates have the `.prpt` file extension and are stored in the `pentahoReports` directory. Each template defines the layout, data queries, and parameters for a specific report (e.g., "Active Loan Summary").
2.  **Fineract API:** When you request a report through the Fineract API (or the Mifos UI), you specify the report name and any required parameters (like dates, office IDs, etc.).
3.  **Pentaho Engine:** Fineract passes this request to the Pentaho reporting engine, which is loaded by the plugin.
4.  **Data Fetching:** The Pentaho engine connects to your Fineract database (MariaDB in your case), executes the SQL queries defined in the `.prpt` template, and fetches the required data.
5.  **Report Generation:** It then populates the template with the fetched data and generates the final report in the format you requested (e.g., PDF, Excel, CSV).
6.  **API Response:** Fineract sends the generated report back to you as the API response.

In short, the plugin acts as a bridge, allowing you to leverage Pentaho's advanced reporting capabilities to generate professional, data-rich reports from your Fineract data.

### 1.2. How do I use this in the day-to-day business of my microfinance?

The reports provided by this plugin are essential for monitoring the health and performance of your microfinance institution. Here are some examples of how different roles can use them:

*   **Loan Officers:**
    *   `Active Loans - Details`: To get a detailed list of all active loans they manage.
    *   `Expected Payments By Date`: To plan their collection activities for the upcoming days or weeks.
    *   `Portfolio at Risk`: To identify clients who are falling behind on their payments and require follow-up.
*   **Branch Managers:**
    *   `Active Loan Summary per Branch`: To get a high-level overview of the branch's loan portfolio.
    *   `Disbursal Report`: To track the number and amount of loans disbursed by the branch over a specific period.
    *   `Collection Report`: To monitor the daily or weekly collections and compare them against targets.
*   **Accountants & Finance Managers:**
    *   `Balance Sheet`: To get a snapshot of the institution's financial position.
    *   `Income Statement`: To track revenues and expenses and assess profitability.
    *   `Trial Balance`: To ensure the books are balanced and for auditing purposes.
*   **Management & Executives:**
    *   `Aging Summary`: To understand the overall quality of the loan portfolio and identify trends in arrears.
    *   `Client Listing`: To get demographic and other information about the institution's clients.

By regularly generating and analyzing these reports, your team can make more informed decisions, improve operational efficiency, and better manage risk.

## 2. Installation and Setup

### 2.1. Prerequisites

- Docker and Docker Compose are installed and running.
- An Apache Fineract instance is running via Docker Compose.

### 2.2. Step 1: Prepare Plugin Files

1.  **Create a directory for the plugin:**

    ```bash
    mkdir fineract-pentaho
    ```

2.  **Download the Mifos Reporting Plugin:**

    Download the appropriate version of the plugin based on your Fineract version. In this guide, we used version 1.12.1 for Fineract 1.12.0.

    ```bash
    curl -L -o fineract-pentaho/MifosSecurityPlugin-1.12.1.zip "https://sourceforge.net/projects/mifos/files/mifos-plugins/MifosReportingPlugin/MifosSecurityPlugin-1.12.1.zip/download"
    ```

3.  **Extract the plugin files:**

    ```bash
    unzip fineract-pentaho/MifosSecurityPlugin-1.12.1.zip -d fineract-pentaho/
    ```

### 2.3. Step 2: Integrate with Docker

1.  **Modify `docker-compose.yml`:**

    Open your `docker-compose.yml` file and add the following volumes and environment variables to the `fineract` service:

    ```yaml
    services:
      fineract:
        # ... existing configuration ...
        volumes:
          - /path/to/mifos-reporting-plugin/fineract-pentaho/MifosSecurityPlugin-1.12.1:/app/plugins
          - /path/to/mifos-reporting-plugin/pentahoReports:/pentahoReports
        environment:
          - FINERACT_PENTAHO_REPORTS_PATH=/pentahoReports/MariaDB/
          - JAVA_TOOL_OPTIONS=-Dloader.path=/app/plugins/
    ```

    **Note:** Replace `/path/to/mifos-reporting-plugin` with the path to your `mifos-reporting-plugin` directory.

2.  **Restart the Docker environment:**

    ```bash
    docker compose up -d
    ```

## 3. Testing and Verification

### 3.1. How can I play around with reports on the Mifos UI for testing purposes?

The Mifos web UI provides a user-friendly interface for running and viewing reports. Here's how you can typically access them:

1.  **Log in to the Mifos UI:** Open your web browser and navigate to the Mifos UI (likely running on `http://localhost:4200` based on your `docker ps` output).
2.  **Navigate to the Reports Section:** Look for a "Reports" or "Reporting" menu item in the main navigation bar, usually on the left side of the screen.
3.  **Select a Report:** The reports section will list all the available Pentaho reports. You can browse or search for the report you want to run.
4.  **Enter Parameters:** When you select a report, a form will appear asking for the required parameters (e.g., start date, end date, office, loan officer).
5.  **Generate the Report:** Fill in the parameters and click "Run Report" or a similar button. The UI will then make an API call to Fineract to generate the report.
6.  **View or Download:** Once the report is generated, you can typically view it directly in your browser or download it as a PDF, Excel, or CSV file.

This interface is a great way to explore the available reports and understand the data they provide without having to use the API directly.

### 3.2. Testing with the API

1.  **Verify Fineract is running:**

    Check that the `fineract` container is healthy:

    ```bash
    docker compose ps
    ```

2.  **Run a test report:**

    Execute the following `curl` command to generate a test report. The output will be saved to `report.pdf`.

    ```bash
    curl --location --request GET 'https://localhost:443/fineract-provider/api/v1/runreports/Expected%20Payments%20By%20Date%20-%20Formatted?tenantIdentifier=default&locale=en&dateFormat=dd%20MMMM%20yyyy&R_startDate=01%20January%202022&R_endDate=02%20January%202023&R_officeId=1&output-type=PDF&R_loanOfficerId=-1' \
    --header 'Fineract-Platform-TenantId: default' \
    --header 'Authorization: Basic bWlmb3M6cGFzc3dvcmQ=' -k -o report.pdf
    ```

    If the command is successful, a `report.pdf` file will be created in your current directory.

## 4. Frontend Integration Tips

When building a custom frontend for your microfinance institution, here are some tips for integrating the reporting features effectively:

*   **Create a Dedicated Reporting Module:** Design a dedicated section in your application for reports. This will make it easy for users to find and access them.
*   **User-Friendly Parameter Selection:**
    *   Use date pickers for selecting date ranges.
    *   Provide dropdowns with lists of offices, loan officers, and other filterable options. You can fetch this data from the relevant Fineract APIs.
    *   Set sensible default values for parameters (e.g., the current date).
*   **Asynchronous Report Generation:** Some reports can take a long time to generate. To avoid making the user wait, consider implementing an asynchronous workflow:
    1.  The user submits the report request.
    2.  Your frontend makes the API call to Fineract and shows a "loading" or "processing" indicator.
    3.  Once the report is ready, you can either automatically download it or provide a link for the user to click.
*   **Handle Different Output Formats:** Allow users to choose their preferred output format (PDF for printing, Excel/CSV for data analysis). You can do this by changing the `output-type` parameter in the API call.
*   **Role-Based Access Control (RBAC):** Not all users should have access to all reports. Implement RBAC to control which reports are visible and accessible to different user roles (e.g., a loan officer should only see reports related to their clients).
*   **Displaying Reports:**
    *   For PDFs, you can embed them in an `<iframe>` or use a library like `pdf.js` to render them directly in the browser.
    *   For Excel/CSV files, you can provide a download link.
*   **API Integration:**
    *   Familiarize yourself with the Fineract reporting API endpoint: `/runreports/{reportName}`.
    *   Your frontend will need to construct the correct URL with all the required parameters.
    *   Ensure you handle API errors gracefully and show meaningful error messages to the user.

By following these tips, you can create a powerful and user-friendly reporting experience for your microfinance staff, enabling them to leverage the full potential of the Fineract reporting plugin.

## 5. Troubleshooting

-   **`curl` command fails to connect:**
    -   Ensure the `fineract` container is running and healthy.
    -   Verify the port mapping in your `docker-compose.yml`. The Fineract API should be accessible on the mapped host port (e.g., `443`).
-   **PDF is invalid or contains an error:**
    -   Check the `fineract` container logs for any errors related to Pentaho or the reporting plugin.
    -   Verify that the `FINERACT_PENTAHO_REPORTS_PATH` environment variable in your `docker-compose.yml` points to the correct directory (e.g., `/pentahoReports/MariaDB/`).

## 7. Building a Custom Fineract Docker Image with the Reporting Plugin

For a more portable and self-contained deployment, you can build a custom Docker image that includes the Fineract application and the reporting plugin.

### 7.1. Create a `Dockerfile`

In your `mifos-reporting-plugin` directory, create a file named `Dockerfile` with the following content:

```dockerfile
# Use the official Fineract image as the base
FROM apache/fineract:latest

# Set the working directory
WORKDIR /app

# Copy the plugin JAR files into the image
COPY fineract-pentaho/MifosSecurityPlugin-1.12.1 /app/plugins
```

This `Dockerfile` uses the official Fineract image as its starting point and copies the extracted plugin files into the `/app/plugins` directory inside the image.

### 7.2. Build the Custom Docker Image

Run the following command from your `mifos-reporting-plugin` directory to build the image. You can replace `my-fineract-with-reporting:latest` with your preferred image name and tag.

```bash
docker build -t my-fineract-with-reporting:latest .
```

### 7.3. Use the Custom Image in Docker Compose

Now, you can update your `docker-compose.yml` to use your new custom image instead of the official one. You will also no longer need the volume mount for the plugin files, as they are now part of the image.

1.  **Modify `docker-compose.yml`:**

    Update the `fineract` service in your `docker-compose.yml` to use the new image and remove the plugin volume mount:

    ```yaml
    services:
      fineract:
        # ... existing configuration ...
        image: my-fineract-with-reporting:latest # Use your custom image
        volumes:
          # The plugin volume is no longer needed
          - /path/to/mifos-reporting-plugin/pentahoReports:/pentahoReports
        environment:
          - FINERACT_PENTAHO_REPORTS_PATH=/pentahoReports/MariaDB/
          - JAVA_TOOL_OPTIONS=-Dloader.path=/app/plugins/
    ```

    **Note:** You should still mount the `pentahoReports` directory as a volume. This allows you to update the report templates without having to rebuild the entire Docker image.

2.  **Restart the Docker environment:**

    ```bash
    docker compose up -d
    ```

Your Fineract instance will now be running from a custom image with the reporting plugin built-in, making your setup more robust and easier to manage.