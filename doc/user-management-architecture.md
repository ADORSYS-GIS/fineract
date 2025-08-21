# Fineract User Management Architecture

## 1. Introduction

This document provides a detailed overview of the user management architecture in Apache Fineract. Understanding this architecture is crucial for administrators and developers who need to manage user access, define roles, and ensure the security of the Fineract platform.

The core of Fineract's user management is built on a robust Role-Based Access Control (RBAC) model. This model governs what actions a user can perform within the system, ensuring that users only have access to the functionalities necessary for their roles.

## 2. Core Concepts

Fineract's user management revolves around four key concepts: Users, Clients, Roles, and Permissions.

### 2.1. Fineract Users

**Users** are the operators of the Fineract system. These are the individuals who log in to the Fineract platform to perform administrative or operational tasks. Examples of users include loan officers, branch managers, and system administrators.

By default, a fresh installation of Fineract includes the following users:

-   `mifos`: The primary super administrator user, intended for initial setup and high-level administrative tasks.
-   `interopUser`: A user for interoperability purposes, often used for system-to-system integrations.
-   `system`: A system-level user for internal processes.

### 2.2. Fineract Clients

**Clients** are the customers of the financial institution that uses Fineract. These are the individuals or entities who receive financial services, such as loans or savings accounts. It is important to distinguish between **Users** (system operators) and **Clients** (customers).

### 2.3. Roles

**Roles** are the cornerstone of Fineract's RBAC model. A role is a collection of permissions that defines a specific set of capabilities within the system. Instead of assigning permissions directly to users, permissions are grouped into roles, and roles are then assigned to users. This makes user management more efficient and less error-prone.

By default, Fineract comes with two predefined roles:

-   `Super user`: This role has all permissions in the system and provides unrestricted access. It is assigned to the `mifos` and `interopUser` by default.
-   `Self Service User`: This role is intended for clients who access the system through a self-service portal. It is disabled by default.

### 2.4. Permissions

**Permissions** are the most granular level of access control in Fineract. Each permission corresponds to a specific action that can be performed in the system, such as creating a client, approving a loan, or viewing a report. There are hundreds of permissions available, allowing for fine-grained control over user access.

## 3. Authentication and Authorization Flow

### 3.1. Authentication with Keycloak

Fineract delegates user authentication to **Keycloak**, an open-source identity and access management solution. When a user attempts to log in, Fineract redirects them to Keycloak to verify their credentials.

The link between a Fineract user and a Keycloak user is the **username**. For a user to be authenticated, a user with the same username must exist in both Fineract and the `fineract` realm in Keycloak. The `setup_keycloak.sh` script is responsible for creating the initial `mifos` user in Keycloak to match the default Fineract user.

### 3.2. Authorization (RBAC)

Once a user is authenticated, Fineract's internal RBAC model takes over for authorization. When a user attempts to perform an action (e.g., make an API request), the following steps occur:

1.  Fineract identifies the user making the request.
2.  It retrieves the roles assigned to that user.
3.  It checks the permissions associated with those roles.
4.  If the required permission for the action is found in the user's roles, the action is authorized. Otherwise, it is denied with a `403 Forbidden` error.

## 4. Practical Walkthrough: Initial State

Let's examine the default user management configuration in a new Fineract instance.

### 4.1. Obtaining an Access Token

Before we can interact with the Fineract API, we need an access token. For administrative tasks, we can get a token for the `mifos` user using the Authorization Code flow.

**Step 1: Get the Authorization Code**

1.  In your browser, navigate to the following URL (ensure it's a single line):
   
    ```
    http://172.17.0.1:9000/realms/fineract/protocol/openid-connect/auth
    ?client_id=web-client
    &response_type=code
    &scope=openid
    &redirect_uri=http://localhost:8080/
    ```
2.  Log in with the `mifos` user credentials (`mifos` / `password`).
3.  After login, you will be redirected to an error page. Copy the `code` from the URL in your browser's address bar. This code is single-use.

**Step 2: Exchange the Code for an Access Token**

Use the code you just copied to run the following command. Remember to replace `[YOUR_CLIENT_SECRET_HERE]` with the secret from the Keycloak Admin Console.

```bash
# Replace placeholders with your code and client secret
TOKEN=$(curl --silent --request POST \
  "http://172.17.0.1:9000/realms/fineract/protocol/openid-connect/token" \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=authorization_code' \
  --data-urlencode 'client_id=web-client' \
  --data-urlencode 'client_secret=**********' \
  --data-urlencode 'code=[THE_CODE_YOU_COPIED_FROM_THE_BROWSER]' \
  --data-urlencode 'redirect_uri=http://localhost:8080/' \
  | jq -r '.access_token')

echo "Access Token: $TOKEN"
```
This command stores the access token in the `$TOKEN` environment variable for the following examples.



### 4.2. Default Users

A `curl` request to the `/users` endpoint shows the default users:

```bash
curl --insecure -X GET \
  "https://localhost/fineract-provider/api/v1/users" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" | jq
```

This will return a list containing the `mifos`, `interopUser`, and `system` users, with the `Super user` role assigned to `mifos` and `interopUser`.

### 4.3. Default Roles

A `curl` request to the `/roles` endpoint shows the default roles:

```bash
curl --insecure -X GET \
  "https://localhost/fineract-provider/api/v1/roles" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" | jq
```

This will return the `Super user` and `Self Service User` roles.

### 4.4. Available Permissions

To see the extensive list of available permissions, you can query the `/permissions` endpoint:

```bash
curl --insecure -X GET \
  "https://localhost/fineract-provider/api/v1/permissions" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" | jq
```

This will list all the granular permissions that can be used to build custom roles.

## 5. Next Steps

This document has outlined the foundational concepts of user management in Fineract. The next steps will involve practical examples of:

-   Creating new roles with a specific set of permissions.
-   Creating new users in both Fineract and Keycloak.
-   Assigning the newly created roles to these users.
-   Testing the permissions to ensure that users can only perform the actions defined by their roles.

## 6. Building the Hierarchy: Creating Roles

Now that we understand the basic concepts, let's start building our user hierarchy. The first step is to create the necessary roles. We will create three roles: `Branch Manager`, `Loan Officer`, and `Teller`. We will use the `mifos` user's token for these operations, as it has the `Super user` role.

### 6.1. Create Branch Manager Role

The Branch Manager will have the highest level of permissions among our new roles, responsible for overseeing all operations within a branch.

```bash
# Create Branch Manager role
curl --insecure -X POST \
  "https://localhost/fineract-provider/api/v1/roles" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Branch Manager",
    "description": "Manages all branch operations, including loan approvals and user management within the branch."
  }' | jq
```

### 6.2. Create Loan Officer Role

The Loan Officer will be responsible for managing the entire loan lifecycle for clients, from application to disbursement.

```bash
# Create Loan Officer role
curl --insecure -X POST \
  "https://localhost/fineract-provider/api/v1/roles" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Loan Officer",
    "description": "Manages loan applications, client relationships, and loan disbursement."
  }' | jq
```

### 6.3. Create Teller Role

The Teller will have the most limited set of permissions, focused on client-facing transactional tasks.

```bash
# Create Teller role
curl --insecure -X POST \
  "https://localhost/fineract-provider/api/v1/roles" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Teller",
    "description": "Handles client transactions, such as deposits, withdrawals, and payments."
  }' | jq
```

After creating these roles, the next step is to assign them a specific set of permissions. We will do this in the following sections.

## 7. Assigning Permissions to Roles

Now that we have created our roles, we need to grant them the appropriate permissions. The following commands will assign a curated set of permissions to the `Teller`, `Loan Officer`, and `Branch Manager` roles.

### 7.1. Assign Permissions to Teller (ID: 3)

```bash
curl --insecure -X PUT \
  "https://localhost/fineract-provider/api/v1/roles/3/permissions" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "permissions": {
      "READ_CLIENT": true,
      "READ_LOAN": true,
      "READ_SAVINGSACCOUNT": true,
      "DEPOSIT_SAVINGSACCOUNT": true,
      "WITHDRAWAL_SAVINGSACCOUNT": true,
      "REPAYMENT_LOAN": true,
      "CREATE_OFFICETRANSACTION": true,
      "READ_OFFICETRANSACTION": true
    }
  }' | jq
```

### 7.2. Assign Permissions to Loan Officer (ID: 4)

```bash
curl --insecure -X PUT \
  "https://localhost/fineract-provider/api/v1/roles/4/permissions" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "permissions": {
      "CREATE_CLIENT": true,
      "UPDATE_CLIENT": true,
      "ACTIVATE_CLIENT": true,
      "READ_CLIENT": true,
      "CREATE_LOAN": true,
      "UPDATE_LOAN": true,
      "READ_LOAN": true,
      "CREATE_SAVINGSACCOUNT": true,
      "UPDATE_SAVINGSACCOUNT": true,
      "READ_SAVINGSACCOUNT": true,
      "DISBURSE_LOAN": true,
      "REPAYMENT_LOAN": true,
      "WAIVEINTERESTPORTION_LOAN": true,
      "WRITEOFF_LOAN": true,
      "CLOSE_LOAN": true,
      "CREATE_DOCUMENT": true,
      "UPDATE_DOCUMENT": true,
      "READ_DOCUMENT": true,
      "CREATE_GUARANTOR": true,
      "UPDATE_GUARANTOR": true,
      "READ_GUARANTOR": true,
      "CREATE_COLLATERAL": true,
      "UPDATE_COLLATERAL": true,
      "READ_COLLATERAL": true
    }
  }' | jq
```

### 7.3. Assign Permissions to Branch Manager (ID: 5)

```bash
curl --insecure -X PUT \
  "https://localhost/fineract-provider/api/v1/roles/5/permissions" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "permissions": {
      "CREATE_CLIENT": true,
      "UPDATE_CLIENT": true,
      "READ_CLIENT": true,
      "CREATE_LOAN": true,
      "UPDATE_LOAN": true,
      "READ_LOAN": true,
      "CREATE_SAVINGSACCOUNT": true,
      "UPDATE_SAVINGSACCOUNT": true,
      "READ_SAVINGSACCOUNT": true,
      "DISBURSE_LOAN": true,
      "REPAYMENT_LOAN": true,
      "WAIVEINTERESTPORTION_LOAN": true,
      "WRITEOFF_LOAN": true,
      "CLOSE_LOAN": true,
      "CREATE_DOCUMENT": true,
      "UPDATE_DOCUMENT": true,
      "READ_DOCUMENT": true,
      "CREATE_GUARANTOR": true,
      "UPDATE_GUARANTOR": true,
      "READ_GUARANTOR": true,
      "CREATE_COLLATERAL": true,
      "UPDATE_COLLATERAL": true,
      "READ_COLLATERAL": true,
      "APPROVE_LOAN": true,
      "REJECT_LOAN": true,
      "WITHDRAW_LOAN": true,
      "APPROVALUNDO_LOAN": true,
      "DISBURSALUNDO_LOAN": true,
      "CREATE_STAFF": true,
      "UPDATE_STAFF": true,
      "READ_STAFF": true,
      "CREATE_USER": true,
      "UPDATE_USER": true,
      "READ_USER": true,
      "CREATE_LOANPRODUCT": true,
      "UPDATE_LOANPRODUCT": true,
      "CREATE_SAVINGSPRODUCT": true,
      "UPDATE_SAVINGSPRODUCT": true,
      "CREATE_CHARGE": true,
      "UPDATE_CHARGE": true,
      "READ_AUDIT": true,
      "READ_MAKERCHECKER": true
    }
  }' | jq
```

## 8. Creating Users and Assigning Roles

With our roles and permissions in place, the next step is to create the users and assign them their respective roles. We will create a `Teller` user, a `Loan Officer` user, and a `Branch Manager` user.

### 8.1. Create Teller User

```bash
curl --insecure -X POST \
  "https://localhost/fineract-provider/api/v1/users" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "username": "teller",
    "firstname": "Teller",
    "lastname": "User",
    "email": "teller@example.com",
    "officeId": 1,
    "roles": [3],
    "sendPasswordToEmail": false,
    "password": "Abc1!Xyz@2Df",
    "repeatPassword": "Abc1!Xyz@2Df"
  }'
```

### 8.2. Create Loan Officer User

```bash
curl --insecure -X POST \
  "https://localhost/fineract-provider/api/v1/users" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "username": "loan",
    "firstname": "Loan",
    "lastname": "Officer",
    "email": "loan@example.com",
    "officeId": 1,
    "roles": [4],
    "sendPasswordToEmail": false,
    "password": "Abc1!Xyz@2Df",
    "repeatPassword": "Abc1!Xyz@2Df"
  }'
```

### 8.3. Create Branch Manager User

```bash
curl --insecure -X POST \
  "https://localhost/fineract-provider/api/v1/users" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "username": "manager",
    "firstname": "Branch",
    "lastname": "Manager",
    "email": "manager@example.com",
    "officeId": 1,
    "roles": [5],
    "sendPasswordToEmail": false,
    "password": "Abc1!Xyz@2Df",
    "repeatPassword": "Abc1!Xyz@2Df"
  }'
```

## 9. Creating Corresponding Users in Keycloak

For each user created in Fineract, a corresponding user must be created in Keycloak with the same username. This is because Fineract delegates authentication to Keycloak.

To create a new user in Keycloak, you will need to edit the `setup_keycloak.sh` script. Open the script and modify the following variables with the new user's information:

-   `USERNAME`
-   `FIRST_NAME`
-   `LAST_NAME`
-   `EMAIL`

For example, to create the `teller` user, you would modify the script as follows:

```bash
USERNAME="teller"
FIRST_NAME="Teller"
LAST_NAME="User"
EMAIL="teller@example.com"
```

After saving the changes, run the script to create the user in Keycloak:

```bash
docker exec fineract-keycloak-1 bash -c "$(cat setup_keycloak.sh)"
```

Repeat this process for each new user you want to create.

## 10. Testing Role-Based Access Control (RBAC)

Now that we have created our users and assigned them roles and permissions, we can test the RBAC implementation. We will attempt to perform an action that a user should not be able to do based on their assigned permissions.

### 10.1. Test Case 1: Teller Cannot Create a Client

The `Teller` role does not have the `CREATE_CLIENT` permission. Let's try to create a client using the `teller` user's token.

#### 10.1.1. Get a Token for the Teller User

To get a token for the `teller` user, follow the same Authorization Code flow.

**Step 1: Get the Authorization Code**

1.  In your browser, navigate to the following URL:
    ```
    http://172.17.0.1:9000/realms/fineract/protocol/openid-connect/auth?client_id=community-app&response_type=code&scope=openid&redirect_uri=http://localhost:8080/
    ```
2.  Log in with the `teller` user credentials (`teller` / `Abc1!Xyz@2Df`).
3.  Copy the single-use `code` from the URL in your browser's address bar after the redirect.

**Step 2: Exchange the Code for an Access Token**

Run the following command, pasting the new code and your client secret.

```bash
# Replace placeholders with your new code and client secret
TOKEN=$(curl --silent --request POST \
  "http://172.17.0.1:9000/realms/fineract/protocol/openid-connect/token" \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=authorization_code' \
  --data-urlencode 'client_id=community-app' \
  --data-urlencode 'client_secret=[YOUR_CLIENT_SECRET_HERE]' \
  --data-urlencode 'code=[THE_TELLER_CODE_FROM_THE_BROWSER]' \
  --data-urlencode 'redirect_uri=http://localhost:8080/' \
  | jq -r '.access_token')
```


#### 10.1.2. Attempt to Create a Client

```bash
# This should fail with a 403 Forbidden error
curl --insecure \
  "https://localhost/fineract-provider/api/v1/clients" \
  --header "Fineract-Platform-TenantId: default" \
  --header "Authorization: Bearer $TOKEN" \
  --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "officeId": 1,
    "firstname": "Petra",
    "lastname": "Yton",
    "externalId": "786YYH7",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en",
    "active": true,
    "activationDate": "04 March 2009",
    "submittedOnDate": "04 March 2009",
    "legalFormId": 1
  }' | jq
```

This request will be denied with a `403 Forbidden` error, confirming that our RBAC implementation is working correctly. The expected error response is:

```json
{
  "developerMessage": "The user associated with credentials passed on this request does not have sufficient privileges to perform this action.",
  "httpStatusCode": "403",
  "defaultUserMessage": "Insufficient privileges to perform this action.",
  "userMessageGlobalisationCode": "error.msg.not.authorized",
  "errors": [
    {
      "defaultUserMessage": "User has no authority to: CREATE_CLIENT",
      "parameterName": "id",
      "developerMessage": "User has no authority to: CREATE_CLIENT",
      "userMessageGlobalisationCode": "error.msg.not.authorized",
      "args": []
    }
  ]
}
```

### 10.2. Test Case 2: Loan Officer Can Create a Client

The `Loan Officer` role has the `CREATE_CLIENT` permission. Let's try to create a client using the `loan` user's token.

#### 10.2.1. Get a Token for the Loan Officer User

To get a token for the `loan` user, follow the Authorization Code flow again.

**Step 1: Get the Authorization Code**

1.  In your browser, navigate to the following URL:
    ```
    http://172.17.0.1:9000/realms/fineract/protocol/openid-connect/auth?client_id=community-app&response_type=code&scope=openid&redirect_uri=http://localhost:8080/
    ```
2.  Log in with the `loan` user credentials (`loan` / `Abc1!Xyz@2Df`).
3.  Copy the single-use `code` from the URL in your browser's address bar after the redirect.

**Step 2: Exchange the Code for an Access Token**

Run the following command, pasting the new code and your client secret.

```bash
# Replace placeholders with your new code and client secret
TOKEN=$(curl --silent --request POST \
  "http://172.17.0.1:9000/realms/fineract/protocol/openid-connect/token" \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=authorization_code' \
  --data-urlencode 'client_id=community-app' \
  --data-urlencode 'client_secret=[YOUR_CLIENT_SECRET_HERE]' \
  --data-urlencode 'code=[THE_LOAN_OFFICER_CODE_FROM_THE_BROWSER]' \
  --data-urlencode 'redirect_uri=http://localhost:8080/' \
  | jq -r '.access_token')
```

#### 10.2.2. Attempt to Create a Client

```bash
# This should succeed
curl --insecure \
  "https://localhost/fineract-provider/api/v1/clients" \
  --header "Fineract-Platform-TenantId: default" \
  --header "Authorization: Bearer $TOKEN" \
  --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "officeId": 1,
    "firstname": "Petra",
    "lastname": "Yton",
    "externalId": "786YYH7",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en",
    "active": true,
    "activationDate": "04 March 2009",
    "submittedOnDate": "04 March 2009",
    "legalFormId": 1
  }' | jq
```

Output:

``` json
{
  "officeId": 1,
  "clientId": 2,
  "resourceId": 2,
  "resourceExternalId": "786YYH7"
}
```
