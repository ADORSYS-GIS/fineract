# Local Development Setup Guide

Assuming you have cloned our Adorsys Fineract repository and you have `cd` into the Fineract project root, here are the steps to start Fineract with all the services defined in `fineract/docker-compose-adorsys.yml` in **Basic Auth mode ONLY**.

## 1. Verify Configuration

The environment is configured for Basic Authentication by default in `fineract/config/docker/env/fineract-adorsys.env`.

You can verify that the following variables are set as follows:

```properties
FINERACT_SECURITY_BASICAUTH_ENABLED=true
FINERACT_SECURITY_OAUTH_ENABLED=false
```

## 2. Start the Services

Run the following command to start all services (Database, Fineract, Keycloak, etc.):

```bash
docker compose -f fineract/docker-compose-adorsys.yml up -d
```

Note: Even in Basic Auth mode, the Keycloak services defined in the compose file will still start unless you explicitly exclude them, but Fineract will use Basic Auth for login.

## 3. Access the Application

Once the services are running and healthy (this may take a few minutes):

-   **Mifos Web App (Community App):** http://localhost:4200
    -   Login with default credentials: `mifos` / `password`

## 4. Fineract Config CLI Authentication

The `fineract-config-cli` tool is configured to use Basic Authentication by default (`FINERACT_AUTH_TYPE=basic`).
It also supports OAuth2. To switch to OAuth2 mode, update the following variables in `fineract/config/docker/env/fineract-adorsys.env`:

```properties
FINERACT_AUTH_TYPE=oauth2
FINERACT_AUTH_OAUTH2_TOKEN_URL=http://keycloak:8080/realms/fineract/protocol/openid-connect/token
FINERACT_AUTH_OAUTH2_CLIENT_ID=setup-app-client
FINERACT_AUTH_OAUTH2_GRANT_TYPE=password
```

Ensure `FINERACT_USERNAME` and `FINERACT_PASSWORD` are correct, as they are used for the `password` grant type.

## 5. Running in OAuth Mode

To run Fineract and the Frontends in OAuth mode (using Keycloak):

1.  **Configure Environment:**
    Update `fineract/config/docker/env/fineract-adorsys.env`:
    ```properties
    FINERACT_SECURITY_BASICAUTH_ENABLED=false
    FINERACT_SECURITY_OAUTH_ENABLED=true
    ```

    **Important:** The legacy **Mifos Web App** relies on Basic Authentication and **will not work** in this OAuth-only mode.

    **To use Mifos Web App (e.g., to verify data import):**
    You must temporarily switch back to **Basic Auth Mode**:
    1.  Set `FINERACT_SECURITY_BASICAUTH_ENABLED=true` and `FINERACT_SECURITY_OAUTH_ENABLED=false` in the env file.
    2.  **Apply changes:**
        ```bash
        docker compose -f fineract/docker-compose-adorsys.yml stop fineract
        docker compose -f fineract/docker-compose-adorsys.yml up -d fineract
        ```
        *(Note: Do not use `restart` command as it may not pick up the changes in the environment file).*

2.  **Start Services:**
    Restart the services:
    ```bash
    docker compose -f fineract/docker-compose-adorsys.yml up -d
    ```

3.  **Access Applications:**
    In OAuth mode, you must access applications through the **Apache Proxy** (port 80) to handle authentication.

    -   **Cashier App:** http://localhost/cashier
    -   **Account Manager:** http://localhost/account

    You will be redirected to Keycloak to login (default: `mifos` / `password`).
