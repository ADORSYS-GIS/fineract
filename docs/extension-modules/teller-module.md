# Fineract Teller Extension Module

## 1. Objective

The primary goal of the `teller` extension module is to provide a custom, enhanced implementation for managing teller and cashier data within Fineract. It is designed to override the default Fineract service for reading cashier information, allowing for more flexible and specific queries without altering the core Fineract codebase.

This module serves as a clear example of how to extend Fineract's functionality in a non-invasive, modular way.

## 2. How It Works

The module is composed of two main parts: a **service** that contains the custom business logic and a **starter** that handles the integration of this service into the main Fineract application.

### 2.1. The Service Logic: Overriding Core Behavior

The core of the module is the `CustomTellerManagementReadPlatformServiceImpl.java` class.

-   **What it does:** This class extends Fineract's default `TellerManagementReadPlatformServiceImpl` and is marked with the `@Primary` annotation. This is a key feature of the Spring Framework. It signals that if multiple implementations (beans) of the same service interface exist, this "primary" one should be the default choice for dependency injection throughout the application.

-   **How it works:** The class specifically overrides the `getCashierData` method. Instead of relying on Fineract's default logic for fetching cashier information, this custom module substitutes its own implementation. It constructs and executes a more detailed SQL query that allows for filtering cashiers by `officeId`, `tellerId`, `staffId`, and `date`.

In essence, this module cleanly replaces Fineract's standard method of retrieving cashier data with its own enhanced version.

### 2.2. The Integration: Plugging into Fineract

The seamless integration of this custom service into the main Fineract application is managed by the `TellerEmptyMethodImplementationAutoConfiguration.java` class.

-   **What it is:** This is a Spring Boot auto-configuration class. Its purpose is to automatically configure the necessary components for the `teller` module when the Fineract application starts up.

-   **How it works:**
    -   `@ComponentScan("com.adorsys.fineract.teller.service")`: This annotation instructs Spring to scan the specified package for components. This is how it discovers the `CustomTellerManagementReadPlatformServiceImpl` and registers it as a `@Primary` bean in the application context.
    -   `@ConditionalOnProperty("adorsys.teller.extended.enabled")`: This is a powerful feature that makes the entire custom module conditional. The custom teller service will **only be activated** if a property named `adorsys.teller.extended.enabled` is set to `true` in one of Fineract's configuration files (e.g., `application.properties` or an environment-specific file). This provides a simple and effective way to enable or disable this custom functionality without modifying any code.

## 3. Configuration

To enable the custom teller module, you must add the following property to your Fineract configuration. For Docker-based setups, this can be done in the `fineract/config/docker/env/fineract.env` file:

```properties
adorsys.teller.extended.enabled=true
```

If this property is not present or is set to `false`, the custom module will not be activated, and Fineract will continue to use its default `TellerManagementReadPlatformServiceImpl`.

## 4. API Usage

The `teller` module enhances the following API endpoints:

### 4.1. Get Cashier Data

-   **Endpoint**: `GET /v1/cashiers`
-   **Description**: Retrieves a list of cashiers, with enhanced filtering capabilities provided by the custom module.
-   **Query Parameters**:
    -   `officeId` (optional): The ID of the office to filter by.
    -   `tellerId` (optional): The ID of the teller to filter by.
    -   `staffId` (optional): The ID of the staff member to filter by.
    -   `date` (optional): The date to filter by (in `yyyyMMdd` format).

### 4.2. Get Cashiers for a Teller

-   **Endpoint**: `GET /v1/tellers/{tellerId}/cashiers`
-   **Description**: Retrieves a list of cashiers for a specific teller.
-   **Query Parameters**:
    -   `fromdate` (optional): The start date of the date range to filter by (in `yyyyMMdd` format).
    -   `todate` (optional): The end date of the date range to filter by (in `yyyyMMdd` format).

## 5. Comprehensive Test Plan

This section provides a series of `curl` commands to test all functionality.

### 5.1. Test 1: Get All Cashiers

```bash
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/cashiers" | jq
```

### 5.2. Test 2: Get Cashiers by Office

```bash
# Replace {officeId} with a valid office ID
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/cashiers?officeId={officeId}" | jq
```

### 5.3. Test 3: Get Cashiers by Teller

```bash
# Replace {tellerId} with a valid teller ID
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/cashiers?tellerId={tellerId}" | jq
```

### 5.4. Test 4: Get Cashiers for a Teller

```bash
# Replace {tellerId} with a valid teller ID
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/tellers/{tellerId}/cashiers" | jq
```
