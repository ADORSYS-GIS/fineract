# Automated Keycloak Setup for Fineract Integration

This document provides a comprehensive guide to using the `setup_keycloak.sh` script to automate the configuration of Keycloak for Fineract integration.

## 1. Purpose of the Script

The primary goal of this script is to provide a reliable, repeatable, and fast way to configure a Keycloak instance for Fineract. Manual configuration through a web UI is prone to human error and is not suitable for automated testing or deployment environments. This script solves that by codifying the entire setup process.

**Key Aims:**
-   **Consistency:** Ensures that the Keycloak configuration is identical every time it is run.
-   **Speed:** Sets up the entire realm in seconds, compared to several minutes of manual work.
-   **Automation:** Enables the integration of Keycloak setup into larger CI/CD pipelines or automated testing frameworks.
-   **Idempotency:** The script is designed to be idempotent, meaning it can be run multiple times without causing errors. It achieves this by deleting the old `fineract` realm before starting, ensuring a clean slate for each execution.
-   **Complete Automation:** The script now includes protocol mapper creation, eliminating all manual steps.

---

## 2. The `setup_keycloak.sh` Script Explained

This is a line-by-line explanation of the commands within the script and why they are necessary.

```bash
#!/bin/bash

# Automated Keycloak setup for Fineract integration
# This script creates everything needed including the protocol mapper

set -e

# --- Configuration ---
# These variables make the script easy to read and modify.
KEYCLOAK_BIN="/opt/keycloak/bin"
REALM="fineract"
CLIENT_ID="fineract-client"
USERNAME="fineract-user"
PASSWORD="fineract"
ROLE="SUPER_USER"

# --- Login ---
# The script must first authenticate to the Keycloak Admin CLI.
# It logs into the default 'master' realm as the admin user.
echo "🔑 Logging into Keycloak CLI..."
$KEYCLOAK_BIN/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin

# --- Cleanup ---
# To ensure a clean run, the script checks if the 'fineract' realm already exists.
# If it does, it is deleted. This makes the script safe to re-run.
echo "🧹 Checking for existing realm '$REALM'..."
if $KEYCLOAK_BIN/kcadm.sh get realms/$REALM > /dev/null 2>&1; then
  echo "🗑️  Realm '$REALM' exists. Deleting it for a clean setup..."
  $KEYCLOAK_BIN/kcadm.sh delete realms/$REALM
fi

# --- Create Realm ---
# Creates the isolated environment for Fineract's users and clients.
echo "🏗️  Creating realm '$REALM'..."
$KEYCLOAK_BIN/kcadm.sh create realms -s realm=$REALM -s enabled=true

# --- Create Role ---
# Creates the SUPER_USER role. This name must exactly match a role in Fineract.
echo "👤 Creating role '$ROLE'..."
$KEYCLOAK_BIN/kcadm.sh create roles -r $REALM -s name=$ROLE

# --- Create & Configure Client ---
# This uses a robust create-then-update pattern to avoid command-line parsing issues.
# 1. A minimal client is created with just its ID.
echo "🔧 Creating client '$CLIENT_ID'..."
$KEYCLOAK_BIN/kcadm.sh create clients -r $REALM -s clientId=$CLIENT_ID -s enabled=true

# 2. The script retrieves the unique internal ID assigned by Keycloak to the new client.
CLIENT_INTERNAL_ID=$($KEYCLOAK_BIN/kcadm.sh get clients -r $REALM --query clientId=$CLIENT_ID --fields id --format csv --noquotes)

# 3. The script uses the internal ID to update the client with its full configuration.
echo "⚙️  Configuring client '$CLIENT_ID'..."
$KEYCLOAK_BIN/kcadm.sh update clients/$CLIENT_INTERNAL_ID -r $REALM \
    -s "redirectUris=[\"https://localhost/fineract-provider/*\",\"https://localhost:443/fineract-provider/*\"]" \
    -s "clientAuthenticatorType=client-secret" \
    -s "publicClient=false" \
    -s "serviceAccountsEnabled=true" \
    -s "authorizationServicesEnabled=false"

# --- Create the Protocol Mapper (FULLY AUTOMATED!) ---
# This eliminates the previous manual step by creating the protocol mapper via CLI.
echo "🗺️  Creating protocol mapper for roles..."
$KEYCLOAK_BIN/kcadm.sh create clients/$CLIENT_INTERNAL_ID/protocol-mappers/models -r $REALM \
    -s name="realm-roles" \
    -s protocol="openid-connect" \
    -s protocolMapper="oidc-usermodel-realm-role-mapper" \
    -s 'config."multivalued"="true"' \
    -s 'config."userinfo.token.claim"="true"' \
    -s 'config."id.token.claim"="true"' \
    -s 'config."access.token.claim"="true"' \
    -s 'config."claim.name"="roles"' \
    -s 'config."jsonType.label"="String"'

# --- Create User & Assign Role ---
# Creates the user that will be used to log in and assigns the SUPER_USER role to it.
echo "👨‍💻 Creating user '$USERNAME'..."
$KEYCLOAK_BIN/kcadm.sh create users -r $REALM -s username=$USERNAME -s enabled=true

USER_INTERNAL_ID=$($KEYCLOAK_BIN/kcadm.sh get users -r $REALM --query username=$USERNAME --fields id --format csv --noquotes)

echo "🔒 Setting password for user '$USERNAME'..."
$KEYCLOAK_BIN/kcadm.sh set-password -r $REALM --userid $USER_INTERNAL_ID --new-password $PASSWORD --temporary=false

echo "🎭 Assigning role '$ROLE' to user '$USERNAME'..."
$KEYCLOAK_BIN/kcadm.sh add-roles --uusername $USERNAME -r $REALM --rolename $ROLE

# --- Final Output ---
# The script finishes by retrieving and printing the client secret and all necessary information.
echo "🔐 Retrieving client secret..."
CLIENT_SECRET=$($KEYCLOAK_BIN/kcadm.sh get clients/$CLIENT_INTERNAL_ID/client-secret -r $REALM | grep -o '"value": "[^"]*"' | cut -d'"' -f4)

echo "
✅ SETUP COMPLETE! 
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 REALM: $REALM
🔧 CLIENT: $CLIENT_ID  
👤 USER: $USERNAME
🔑 PASSWORD: $PASSWORD
🎭 ROLE: $ROLE
🔐 CLIENT SECRET: $CLIENT_SECRET

📋 NEXT STEPS:
1. Update your fineract-keycloak.env file with:
   FINERACT_SECURITY_OAUTH_CLIENT_SECRET=$CLIENT_SECRET

2. Restart Fineract:
   docker-compose restart fineract

3. Test login at: https://localhost:443/fineract-provider

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"
```

---

## 3. How to Run the Script (Ultra-Simple Method)

The script execution has been streamlined to just **two simple commands**:

### **Step 1: Make the Script Executable (One-time setup)**

```bash
chmod +x setup_keycloak.sh
```

### **Step 2: Run the Script (Works every time)**

```bash
docker exec fineract-keycloak-1 bash -c "$(cat setup_keycloak.sh)"
```

That's it! The script will:
- ✅ Automatically find the Keycloak container
- ✅ Execute inside the container with all dependencies available
- ✅ Create the realm, client, user, roles, and protocol mapper
- ✅ Display the client secret you need for configuration
- ✅ Provide clear next steps

### **Alternative: One-Liner Runner Script**

For even more convenience, you can create a simple runner script:

**Create `run-keycloak-setup.sh`:**
```bash
#!/bin/bash
# Ultra-simple runner for Keycloak setup

echo "🚀 Running Keycloak setup..."
docker exec fineract-keycloak-1 bash -c "$(cat setup_keycloak.sh)"
```

**Make it executable and run:**
```bash
chmod +x run-keycloak-setup.sh
./run-keycloak-setup.sh
```

---

## 4. What Changed from Previous Version

### **✅ Eliminated Manual Steps**
- **Before:** Required manual creation of protocol mapper via web UI
- **Now:** Protocol mapper is created automatically via CLI

### **✅ Simplified Execution**
- **Before:** Required copying script into container and multiple commands
- **Now:** Single command execution from host machine

### **✅ Enhanced Configuration**
- Added support for both `https://localhost/` and `https://localhost:443/` redirect URIs
- Enabled service accounts for better OAuth2 support
- Improved client secret extraction method

### **✅ Better User Experience**
- Added emoji indicators for each step
- Clear, formatted output with all necessary information
- Explicit next steps for completing the setup

---

## 5. Complete Workflow

Here's the complete workflow from start to finish:

1. **Start your Docker services:**
   ```bash
   docker-compose up -d
   ```

2. **Wait for services to be healthy:**
   ```bash
   docker-compose ps
   ```
   (Wait until all services show "healthy" status)

3. **Run the Keycloak setup:**
   ```bash
   chmod +x setup_keycloak.sh
   docker exec fineract-keycloak-1 bash -c "$(cat setup_keycloak.sh)"
   ```

4. **Update your environment file:**
   Copy the client secret from the script output and add it to `config/docker/env/fineract-keycloak.env`:
   ```
   FINERACT_SECURITY_OAUTH_CLIENT_SECRET=your-generated-secret-here
   ```

5. **Restart Fineract:**
   ```bash
   docker-compose restart fineract
   ```

6. **Test the setup:**
   Navigate to `https://localhost:443/fineract-provider` and verify OAuth integration works.

---

## 6. How to Update the Script

To modify the script (e.g., to change the username, add more roles, or create another client), simply edit the `setup_keycloak.sh` file on your local machine.

-   **To add another role:** Add another `kcadm.sh create roles` command.
-   **To change the username:** Modify the `USERNAME` variable at the top of the script.
-   **To add more users:** Duplicate the user creation section with different usernames.

After saving your changes, re-run the script using the same simple command:
```bash
docker exec fineract-keycloak-1 bash -c "$(cat setup_keycloak.sh)"
```

Because the script is idempotent, it will safely delete the old realm and create a new one with your updated configuration.

**Important:** If you re-run the script, a **new client secret will be generated**. You must update the `FINERACT_SECURITY_OAUTH_CLIENT_SECRET` variable in your `config/docker/env/fineract-keycloak.env` file with the new secret and restart the Fineract container (`docker-compose restart fineract`).