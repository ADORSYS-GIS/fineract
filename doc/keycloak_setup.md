# Keycloak Setup for Fineract

This document outlines the setup of Keycloak for authentication and authorization in Fineract.

## Overview

The Keycloak setup is managed through Docker Compose and a setup script. It consists of three main parts:

1.  **Keycloak Service**: The core Keycloak server.
2.  **Keycloak Configuration**: Initial setup of realms, clients, and roles using `adorsys/keycloak-config-cli`.
3.  **User Creation**: A script to create a default user for Fineract.

## Docker Compose Configuration

The `docker-compose.yml` file defines the services required for the Keycloak integration.

### Services

-   `keycloak`:
    -   Uses the `quay.io/keycloak/keycloak:26.2.5` image.
    -   The admin console is accessible at `http://localhost:9000`.
    -   Default admin credentials are `admin` / `admin`.

-   `keycloak-config-cli`:
    -   Uses the `adorsys/keycloak-config-cli:latest` image.
    -   This service runs after the `keycloak` service is healthy.
    -   It automatically imports configuration files from the `./keycloak-config` directory into Keycloak.
    -   This is used to set up the `fineract` realm, clients, roles, and other necessary configurations.

-   `fineract`:
    -   The Fineract service is configured to wait for the `keycloak` service to start.
    -   It is configured with Keycloak-specific environment variables from `./config/docker/env/fineract-keycloak.env`.

## Initial Setup Script

The `setup_keycloak.sh` script is used to perform initial user setup after the Keycloak server is running and the initial configuration has been applied.

### Script Steps

1.  **Login to Keycloak Admin CLI**: The script logs into the `master` realm of the local Keycloak instance using the default admin credentials.

2.  **Create User**: It creates a new user in the `fineract` realm with the following details:
    -   **Username**: `mifos`
    -   **Password**: `password`
    -   **Email**: `test@example.com`
    -   **First Name**: Mifos
    -   **Last Name**: User

3.  **Set Password**: The script sets a permanent password for the newly created user.

## How to Use

### Automated Setup (Recommended)

1.  **Start the services**: Run `docker-compose up -d` to start all services, including Keycloak.

2.  **Make the script executable**: Before running the script, make it executable:
    ```bash
    chmod +x setup_keycloak.sh
    ```

3.  **Run the setup script**: Once the Keycloak container is running, execute the `setup_keycloak.sh` script to create the initial user. The following command executes the script's content inside the Keycloak container:
    ```bash
    docker exec fineract-keycloak-1 bash -c "$(cat setup_keycloak.sh)"
    ```

---

### Manual Configuration (Alternative)

This section describes how to set up Keycloak manually. Note that the `keycloak-config-cli` service in the `docker-compose.yml` file automates most of these steps by importing configurations from the `/keycloak-config` directory.

1.  **Set up Keycloak**
    From your terminal, run:
    ```bash
    docker run -p 9000:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:26.2.5 start-dev
    ```

2.  **Login to Admin Console**
    Go to [http://localhost:9000/admin](http://localhost:9000/admin) and log in with `admin` / `admin`.

3.  **Create a New Realm**
    -   Click 'Manage realms', then 'Create realm'.
    -   Enter `fineract` for the realm name.

4.  **Create a User**
    -   Click on the 'Users' tab on the left, then 'Create new user'.
    -   **Username**: `mifos`
    -   **Email**: `test@example.com`
    -   **First name**: `Mifos`
    -   **Last name**: `User`
    -   Click on the 'Credentials' tab at the top, and set the password to `password`, turning the 'temporary' setting to off.

5.  **Create a Client**
    -   Click on the 'Clients' tab on the left, and create a client with ID `community-app`.
    -   In the 'Settings' tab, set 'Valid redirect URIs' to `http://localhost*`.
    -   Enable 'Client authentication'.
    -   Check 'Direct access grants'.
    -   Click 'Save'. A 'Credentials' tab will appear.
    -   In the 'Credentials' tab, copy the string in the 'secret' field. This will be needed to request the access token.

6.  **Configure Token Mapper**
    We need to change the Keycloak configuration so that it uses the username as the subject of the token.
    -   Choose the `community-app` client in the 'Clients' tab.
    -   Click on the 'Client scopes' tab, then `community-app-dedicated`.
    -   Go to the 'Mappers' tab, click 'Configure a new mapper' and choose 'User Property'.
    -   **Name**: `usernameInSub`
    -   **Property**: `username`
    -   **Token Claim Name**: `sub`

You are now ready to test out OAuth.

---

## How to Manually Get an Access Token for Testing

Since the system uses the OAuth 2.0 Authorization Code flow, getting a token for a user requires an interactive login via a web browser. This guide explains how to do this manually for development and testing purposes.

### Step 1: Get the Authorization Code

1.  Make sure all Docker services are running (`docker-compose up -d`).
2.  Open your web browser and navigate to the following URL. This link is broken into multiple lines for readability; you should combine them into a single URL.

    ```
    http://172.17.0.1:9000/realms/fineract/protocol/openid-connect/auth
    ?client_id=community-app
    &response_type=code
    &scope=openid
    &redirect_uri=http://localhost:8080/
    ```

3.  Keycloak will present a login page. Sign in with a user's credentials (e.g., `mifos` / `password`).
4.  After login, your browser will be redirected to a non-existent page (`http://localhost:8080/...`) and will show a connection error. This is expected.
5.  Copy the `code` parameter from your browser's address bar. It will look like this:
    `http://localhost:8080/?session_state=...&code=A_VERY_LONG_STRING...`

    **Important:** This authorization code is for a **single use** and will expire. If you need to get another token, you must repeat this step to get a new code.

### Step 2: Exchange the Code for an Access Token

Now, use the single-use code from the previous step to get an access token. You will also need the `client_secret`, which you can find in the Keycloak Admin Console (`Clients` > `community-app` > `Credentials` tab.

```bash
# Replace placeholders with your actual code and client secret
TOKEN=$(curl --silent --request POST \
  "http://172.17.0.1:9000/realms/fineract/protocol/openid-connect/token" \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=authorization_code' \
  --data-urlencode 'client_id=community-app' \
  --data-urlencode 'client_secret=[YOUR_CLIENT_SECRET_HERE]' \
  --data-urlencode 'code=[THE_CODE_YOU_COPIED_FROM_THE_BROWSER]' \
  --data-urlencode 'redirect_uri=http://localhost:8080/' \
  | jq -r ".access_token")

echo "Access Token: $TOKEN"
```

### Step 3: Use the Access Token

You can now use the value in the `$TOKEN` variable to make authenticated requests to the Fineract API.

```bash
curl --insecure \
  "https://localhost/fineract-provider/api/v1/clients" \
  --header "Fineract-Platform-TenantId: default" \
  --header "Authorization: Bearer $TOKEN"
```

> **Note on Code Lifespan:** For convenience during manual testing, the lifespan of the authorization code has been increased to 10 minutes. This was done by changing the `accessCodeLifespan` property to `600` in the `/keycloak-config/realm-export.json` file. This gives you more time to complete the manual steps of copying the code and executing the `curl` command.

---

## Fineract Configuration for Keycloak

By default, Fineract is configured to use Basic Authentication. To enable Keycloak integration, we need to override the default security properties.

### Overriding Default Properties

The `fineract-provider/src/main/resources/application.properties` file sets the following defaults:

```properties
fineract.security.basicauth.enabled=true
fineract.security.oauth.enabled=false
```

To change this, the `docker-compose.yml` for the `fineract` service is configured to use an environment file:

```yaml
  fineract:
    ...
    env_file:
      - ./config/docker/env/fineract.env
      - ./config/docker/env/fineract-common.env
      - ./config/docker/env/fineract-mariadb.env
      - ./config/docker/env/fineract-keycloak.env
```

The `config/docker/env/fineract-keycloak.env` file contains the necessary overrides:

```bash
FINERACT_SECURITY_BASICAUTH_ENABLED=false
FINERACT_SECURITY_OAUTH_ENABLED=true
FINERACT_SERVER_OAUTH_RESOURCE_URL=http://172.17.0.1:9000/realms/fineract
```

### Key Configuration Points

1.  **`FINERACT_SECURITY_BASICAUTH_ENABLED=false`**: Disables Fineract's built-in basic authentication.
2.  **`FINERACT_SECURITY_OAUTH_ENABLED=true`**: Enables OAuth 2.0 authentication.
3.  **`FINERACT_SERVER_OAUTH_RESOURCE_URL`**: This is the most critical setting. It tells the Fineract resource server where to find the Keycloak authorization server to validate JWT tokens.

    -   **Why `http://172.17.0.1:9000`?**
        Inside a Docker container, `localhost` refers to the container itself, not the host machine where other containers are running. To enable the `fineract` container to communicate with the `keycloak` container, we need to use an IP address that is reachable from within the container. `172.17.0.1` is typically the default IP address of the Docker host on the bridge network, allowing the Fineract container to connect to the Keycloak service exposed on port 9000 on the host machine. This is similar in concept to using the special DNS name `host.docker.internal`.
