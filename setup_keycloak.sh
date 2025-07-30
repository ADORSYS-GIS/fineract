#!/bin/bash
# create-users.sh - Add users after JSON config import

set -e

KEYCLOAK_BIN="/opt/keycloak/bin"
REALM="fineract"
USERNAME="fineract-user"
PASSWORD="fineract"
ROLE="SUPER_USER"
EMAIL="fineract@example.com"
FIRST_NAME="Fineract"
LAST_NAME="User"

echo "👤 Creating users for realm '$REALM'..."

# Login to Keycloak CLI
$KEYCLOAK_BIN/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin

# Create user with full profile
echo "🔧 Creating user '$USERNAME'..."
$KEYCLOAK_BIN/kcadm.sh create users -r $REALM \
    -s username=$USERNAME \
    -s enabled=true \
    -s emailVerified=true \
    -s email="$EMAIL" \
    -s firstName="$FIRST_NAME" \
    -s lastName="$LAST_NAME"

# Get user ID
USER_INTERNAL_ID=$($KEYCLOAK_BIN/kcadm.sh get users -r $REALM --query username=$USERNAME --fields id --format csv --noquotes)

# Set password
echo "🔒 Setting password..."
$KEYCLOAK_BIN/kcadm.sh set-password -r $REALM --userid $USER_INTERNAL_ID --new-password $PASSWORD --temporary=false

# Assign role
echo "🎭 Assigning role '$ROLE'..."
$KEYCLOAK_BIN/kcadm.sh add-roles --uusername $USERNAME -r $REALM --rolename $ROLE

echo "✅ User creation complete!"