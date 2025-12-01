# Fineract User and Staff Extension Module

## 1. Objective

The `userandstaff` extension module introduces a unified "Employee" concept to Fineract. It provides a simplified, single API endpoint to create and manage both a `Staff` entity and an `AppUser` entity in a single, atomic operation. This is achieved without modifying any of the core Fineract source code, showcasing a clean and maintainable way to extend Fineract's capabilities.

## 2. How It Works

The module is composed of four main parts: `api`, `data`, `service`, and `starter`.

### 2.1. The API Layer

The `api` layer provides the external-facing REST API for the module.

-   **`AdorsysUserAndStaffApiResource.java`**: This class defines the REST endpoints for the module, including `POST /v1/adorsys/employees` for creating an employee and `GET /v1/adorsys/employees/{userId}` for retrieving an employee. It uses standard JAX-RS and OpenAPI annotations to define the API.
-   **`AdorsysUserAndStaffApiResourceSwagger.java`**: This class defines the request and response schemas for the API, which are used to generate the Swagger documentation.

### 2.2. The Data Layer

The `data` layer defines the data transfer objects (DTOs) for the module.

-   **`EmployeeData.java`**: This class represents the unified "Employee" concept, containing fields from both the `Staff` and `AppUser` entities.

### 2.3. The Service Layer

The `service` layer contains the core business logic for the module.

-   **`AdorsysUserAndStaffWritePlatformServiceImpl.java`**: This class is responsible for creating and updating employees. It takes a single JSON command and orchestrates the creation of both the `Staff` and `AppUser` entities by calling the respective core Fineract services.
-   **`AdorsysUserAndStaffReadPlatformServiceImpl.java`**: This class is responsible for retrieving employee data. It fetches data from both the `Staff` and `AppUser` tables and combines them into a single `EmployeeData` object. The `EmployeeDataMapper` class is used to map the data from the `AppUserData` and `StaffData` objects into the `EmployeeData` object, including the `availableRoles` and `selectedRoles` fields.

### 2.4. The Starter Layer

The `starter` layer handles the integration of the module into the main Fineract application.

-   **`UserAndStaffEmptyMethodImplementationAutoConfiguration.java`**: This is a Spring Boot auto-configuration class that scans the `com.adorsys.fineract.userandstaff.service` package to discover and register the service implementations as Spring beans. It is also conditional on the `adorsys.userandstaff.extended.enabled` property.

## 3. Configuration

To enable the custom user and staff module, you must add the following property to your Fineract configuration. For Docker-based setups, this can be done in the `fineract/config/docker/env/fineract.env` file:

```properties
adorsys.userandstaff.extended.enabled=true
```

If this property is not present or is set to `false`, the custom module will not be activated.

## 4. API Usage

The module exposes the following endpoints:

### 4.1. Create an Employee

-   **Endpoint**: `POST /v1/adorsys/employees`
-   **Description**: Creates a new employee, which in turn creates a `Staff` and an `AppUser` entity.
-   **Mandatory Fields**: `officeId`, `firstname`, `lastname`, `username`, `roles`
-   **Optional Fields**: `isLoanOfficer`, `mobileNo`, `externalId`, `joiningDate`

### 4.2. Retrieve an Employee

-   **Endpoint**: `GET /v1/adorsys/employees/{userId}`
-   **Description**: Retrieves the details of an employee.

### 4.3. Update an Employee

-   **Endpoint**: `PUT /v1/adorsys/employees/{userId}`
-   **Description**: Updates the details of an employee.

## 5. Comprehensive Test Plan

This section provides a series of `curl` commands to test all functionality.

**Important:** After the first creation step, you will get a `resourceId` in the response. Please replace `{userId}` in all subsequent commands with that ID.

### 5.1. Test 1: Create a New Employee

```bash
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/adorsys/employees" \
  -X POST \
  -d '{
    "officeId": 1,
    "firstname": "Test",
    "lastname": "User",
    "joiningDate": "01 January 2024",
    "mobileNo": "1112223333",
    "isLoanOfficer": false,
    "externalId": "TU001",
    "username": "testuser",
    "email": "testuser@example.com",
    "roles": [1]
  }' | jq
```

### 5.2. Test 2: Verify Initial Creation

```bash
# Replace {userId} with the resourceId from the previous step
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/adorsys/employees/{userId}" | jq
```

### 5.3. Test 3: Test Partial Update (User-Only Field)

```bash
# Replace {userId} with your new employee's ID
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/adorsys/employees/{userId}" \
  -X PUT \
  -d '{
    "email": "test.user.updated@example.com"
  }' | jq
```

### 5.4. Test 4: Test Partial Update (Staff-Only Field)

```bash
# Replace {userId} with your new employee's ID
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/adorsys/employees/{userId}" \
  -X PUT \
  -d '{
    "mobileNo": "9998887777"
  }' | jq

### 5.5. Test 5: Test Partial Update (Shared Field)

Update a field that is shared between the user and staff records, the `firstname`.

```bash
# Replace {userId} with your new employee's ID
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/adorsys/employees/{userId}" \
  -X PUT \
  -d '{
    "firstname": "Test-Updated"
  }' | jq
```
**Verification:** Run the `GET` command from Test 2 again. The `firstname` should now be "Test-Updated" in both the main object and the nested `staff` object.

### 5.6. Test 6: Test Multi-Field Update

Update multiple fields at once to ensure it handles a larger, mixed payload correctly.

```bash
# Replace {userId} with your new employee's ID
curl -k \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -u mifos:password \
  "https://localhost:443/fineract-provider/api/v1/adorsys/employees/{userId}" \
  -X PUT \
  -d '{
    "lastname": "User-Final",
    "isLoanOfficer": true,
    "email": "final.test@example.com"
  }' | jq
```
**Verification:** Run the `GET` command from Test 2 one last time. You should see that the `lastname`, `isLoanOfficer`, and `email` have all been updated to their final values, and the `firstname` is still "Test-Updated".