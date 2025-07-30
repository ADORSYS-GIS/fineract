
# Fineract-Keycloak Integration Guide

This document provides a detailed, step-by-step guide on how to integrate Fineract with Keycloak for centralized authentication and authorization. By the end of this guide, you will be able to log in to Fineract using credentials managed in Keycloak.

## 1. The Goal: Why Integrate Keycloak?

By default, Fineract manages its own users and permissions. Integrating with an external Identity and Access Management (IAM) solution like Keycloak offers several advantages:

-   **Single Sign-On (SSO):** Users can use one set of credentials to access multiple applications.
-   **Centralized User Management:** Administrators can manage all users and their permissions in one place (Keycloak).
-   **Advanced Security:** Leverage Keycloak's features like multi-factor authentication (MFA), identity brokering, and social logins.

This integration works by configuring Fineract to trust Keycloak. When a user tries to access a Fineract API with a Keycloak token, Fineract validates the token and uses the information within it to determine the user's identity and grant permissions.

---

## 2. Prerequisites

-   Docker and `docker-compose` are installed.
-   You have a running Fineract instance based on the project's `docker-compose.yml` file.

---

## 3. Part 1: Keycloak Configuration

First, we need to set up the necessary components within Keycloak.

### 3.1. Start Your Services

If not already running, start the Fineract and Keycloak containers.

```bash
docker-compose up -d
```

### 3.2. Create a Keycloak Realm

A **Realm** in Keycloak is a dedicated space that manages a set of users, credentials, roles, and clients. We create a new realm for Fineract to keep its configuration isolated from other applications.

1.  Navigate to the Keycloak Admin Console at `http://localhost:8180` and log in (`admin`/`admin`).
2.  Hover over the **"Master"** realm name (top-left) and click **"Add realm"**.
3.  Enter the **Realm name:** `fineract` and click **"Create"**.

### 3.3. Create a Fineract Role

Roles in Keycloak represent permissions. For Fineract to grant access, the role name in the Keycloak token **must exactly match** a role name that exists inside Fineract's own database.

-   **Why `SUPER_USER`?** Fineract comes with a default, all-powerful role named `Super User`. We create a role in Keycloak with the name `SUPER_USER` to map to it. The space is replaced with an underscore by convention in many systems, but here we will use the exact name Fineract expects.
-   **What if I want a `SUPER_GOD` role?** You would first need to create the `SUPER_GOD` role inside the **Fineract UI** (Admin -> Roles) and assign it specific permissions (e.g., `CREATE_CLIENT`, `READ_LOAN`, etc.). Then, you would create a role with the *exact same name* in Keycloak.

1.  In the `fineract` realm, click **"Roles"** on the left menu.
2.  Click **"Add Role"**.
3.  **Role Name:** `SUPER_USER`
4.  Click **"Save"**.

### 3.4. Create and Configure the Fineract Client

A **Client** in Keycloak is an application or service that requests authentication. Here, our "client" is the Fineract application itself.

1.  Click **"Clients"** on the left menu, then click **"Create"**.
2.  **Client ID:** `fineract-client`
3.  **Client Protocol:** `openid-connect`
4.  Click **"Save"**.
5.  On the settings page that appears:
    -   Set **Access Type** to `confidential`. This means the client (Fineract) can securely store a secret and will use it to authenticate itself to Keycloak.
    -   Set **Valid Redirect URIs** to `https://localhost/fineract-provider/*`. This is a security measure to ensure Keycloak only redirects users back to a trusted Fineract URL after login.
6.  Click **"Save"**.
7.  A **"Credentials"** tab will appear. Click it and **copy the Client Secret**. You will need this for Fineract's configuration file.

### 3.5. Create a User and Assign the Role

1.  Click **"Users"** on the left menu, then **"Add user"**.
2.  **Username:** `fineract-user`
3.  Click **"Save"**.
4.  Go to the **"Credentials"** tab for the new user, set a password, and **turn the "Temporary" switch OFF**.
5.  Go to the **"Role Mappings"** tab. From the "Available Roles" box, select `SUPER_USER` and click **"Add selected"**.

### 3.6. Configure the Token Mapper

This is a critical step to ensure the user's roles are included in the access token in a way Fineract can understand.

1.  Click **"Client Scopes"** on the left menu.
2.  Click on the scope named **`roles`**.
3.  Click the **"Mappers"** tab.
4.  Click **"Add Mapper"** -> **"By configuration"** and select **"User Realm Role"**.
5.  Configure the mapper:
    -   **Name:** `Realm Roles`
    -   **Token Claim Name:** `roles`. This is the name of the field in the token that will contain the list of roles. It must match what Fineract is configured to look for.
    -   **Add to access token:** Ensure this is **ON**.
6.  Click **"Add"**.

---

## 4. Part 2: Fineract Configuration

Now we configure Fineract to use our new Keycloak setup.

### 4.1. Update Fineract's Environment

We need to provide Fineract with the Keycloak details via environment variables.

1.  **Ensure `docker-compose.yml` includes the env file.** The `fineract` service definition should have this line in its `env_file` list:
    ```yaml
    - ./config/docker/env/fineract-keycloak.env
    ```

2.  **Create/Verify the `fineract-keycloak.env` file.** The content should be:
    ```ini
    FINERACT_SECURITY_OAUTH_ENABLED=true
    FINERACT_SECURITY_OAUTH_PROVIDER_URL=http://keycloak:8080/realms/fineract
    FINERACT_SECURITY_OAUTH_CLIENT_ID=fineract-client
    FINERACT_SECURITY_OAUTH_CLIENT_SECRET=<your-client-secret>
    FINERACT_SECURITY_OAUTH_PRINCIPAL_ATTRIBUTE=preferred_username
    FINERACT_SECURITY_OAUTH_AUTHORITIES_ATTRIBUTE=roles
    ```
    -   **Explanation of Variables:**
        -   `FINERACT_SECURITY_OAUTH_PROVIDER_URL`: The URL Fineract uses to communicate with Keycloak. We use the service name `keycloak` because they are on the same Docker network.
        -   `FINERACT_SECURITY_OAUTH_CLIENT_SECRET`: **Paste the secret you copied from Step 3.6 here.**
        -   `FINERACT_SECURITY_OAUTH_PRINCIPAL_ATTRIBUTE`: Tells Fineract which field in the token identifies the user. `preferred_username` contains the value `fineract-user`.
        -   `FINERACT_SECURITY_OAUTH_AUTHORITIES_ATTRIBUTE`: Tells Fineract which field contains the list of permissions. This must match the `Token Claim Name` from Step 3.6.

### 4.2. Create the Matching User in Fineract

This is the most common point of failure. **Fineract authenticates using the token, but it authorizes using its own internal user database.** It links the two using the username.

1.  Log in to the Fineract UI (e.g., `https://localhost`) as the `mifos` user.
2.  Navigate to **Admin** -> **Users**.
3.  Click **"Create User"**.
4.  Create a user with the **exact same username** as the one in Keycloak:
    -   **Username:** `fineract-user`
    -   **Office:** `Head Office`
    -   **Roles:** Assign the **`Super User`** role.
    -   Set any password.
5.  Click **"Submit"**.

### 4.3. Restart Fineract

To apply all the new settings, restart the Docker services.

```bash
docker-compose down
docker-compose up
```

---

## 5. Part 3: Testing the Integration

Now we can get a token and use it to call the API.

**1. Get an Access Token**

Use `curl` to request a token from Keycloak using the credentials of the user you created.

```bash
# Remember to replace <your-client-secret>
TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/fineract/protocol/openid-connect/token' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=fineract-client' \
  --data-urlencode 'client_secret=<your-client-secret>' \
  --data-urlencode 'username=fineract-user' \
  --data-urlencode 'password=fineract' | jq -r .access_token)

echo $TOKEN
```

**2. Make an Authenticated API Call**

Use the `$TOKEN` variable to make a request to the Fineract API.

```bash
curl -X POST 'https://localhost/fineract-provider/api/v1/clients' \
--header "Authorization: Bearer $TOKEN" \
--header 'Content-Type: application/json' \
--header 'Fineract-Platform-TenantId: default' \
--data-raw '{
    "officeId": 1,
    "firstname": "John",
    "lastname": "Doe",
    "externalId": "JD-001",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en",
    "active": true,
    "activationDate": "01 January 2024"
}' \
--insecure
```

If successful, you will receive a `200 OK` response with the details of the newly created client. Congratulations, your integration is complete!
