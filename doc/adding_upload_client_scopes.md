# Keycloak Client Scopes Configuration for Fineract

## 1. Introduction

This document extends the Fineract User Management Architecture by detailing the configuration of Keycloak client scopes. Client scopes in Keycloak provide a mechanism to define and control which claims and permissions are included in access tokens. This is essential for enabling specific functionalities in Fineract, such as document uploads and advanced feature access.

Building upon the foundational user management concepts covered in the predecessor document, this guide demonstrates how to properly configure client scopes to ensure that Fineract applications receive the necessary authorization scopes in their access tokens.

## 2. Prerequisites

Before proceeding with this configuration, ensure that:

- You have completed the initial Fineract and Keycloak setup as described in the User Management Architecture document
- You have administrator access to the Keycloak Admin Console
- The `fineract` realm exists in Keycloak
- The `community-app` client is properly configured in the `fineract` realm
- You understand the basic concepts of OAuth 2.0 scopes and Keycloak client configuration

## 3. Understanding Client Scopes in Keycloak

### 3.1. What are Client Scopes?

Client scopes in Keycloak are a way to define a common set of protocol mappers and role scope mappings that can be applied to multiple clients. They allow you to:

- Define which claims are included in tokens
- Control the scope of permissions granted to applications
- Standardize token content across multiple clients
- Enable fine-grained access control

### 3.2. Scope Types

Keycloak supports two types of client scope assignments:

- **Default Scopes**: Automatically included in tokens for the client
- **Optional Scopes**: Only included when explicitly requested by the client application

### 3.3. Relation to Fineract Permissions

Client scopes work in conjunction with Fineract's Role-Based Access Control (RBAC) system. While Fineract roles and permissions control what actions a user can perform within the Fineract application, client scopes control what permissions are included in the OAuth access tokens issued by Keycloak.

## 4. Required Client Scopes for Document Management

For proper document management functionality in Fineract, including Excel document uploads, the following client scopes must be configured:

### 4.1. ALL_FUNCTIONS Scope

This scope provides comprehensive access to all Fineract functions and should be included in tokens for users who need full system access.

### 4.2. CREATE_DOCUMENT Scope

This scope specifically enables document creation capabilities, which is essential for file upload functionality.

## 5. Step-by-Step Configuration Guide

### 5.1. Accessing the Keycloak Admin Console

1. Navigate to your Keycloak Admin Console (typically at `http://172.17.0.1:9000`)
2. Sign in with your administrator credentials
3. Ensure you are in the correct realm by checking the realm selector in the top-left corner

### 5.2. Navigating to the Fineract Realm

1. Click on the realm dropdown in the top-left corner
2. Select "Manage Realms" if you need to see all available realms
3. Locate and click on the `fineract` realm
4. You should now be in the fineract realm administration interface

### 5.3. Creating Client Scopes

#### 5.3.1. Create the ALL_FUNCTIONS Client Scope

1. In the left sidebar, navigate to **Configure** > **Client Scopes**
2. Click the **Create** button to add a new client scope
3. Fill in the following details:
    - **Name**: `ALL_FUNCTIONS`
    - **Description**: `Provides access to all Fineract functions and capabilities`
    - **Type**: `Default`
    - **Protocol**: `openid-connect`
4. In the **Settings** tab, ensure the following options are enabled:
    - **Display on consent screen**: `ON`
    - **Include in token scope**: `ON`
    - **GUI order**: Leave blank or set to a desired order number
5. Click **Save** to create the client scope

#### 5.3.2. Create the CREATE_DOCUMENT Client Scope

1. While still in the **Client Scopes** section, click **Create** again
2. Fill in the following details:
    - **Name**: `CREATE_DOCUMENT`
    - **Description**: `Enables document creation and upload capabilities`
    - **Type**: `Default`
    - **Protocol**: `openid-connect`
3. In the **Settings** tab, ensure the following options are enabled:
    - **Display on consent screen**: `ON`
    - **Include in token scope**: `ON`
    - **GUI order**: Leave blank or set to a desired order number
4. Click **Save** to create the client scope

### 5.4. Assigning Client Scopes to the Community App

#### 5.4.1. Navigate to the Client Configuration

1. In the left sidebar, navigate to **Configure** > **Clients**
2. Locate and click on the `community-app` client
3. You should now be in the client configuration interface

#### 5.4.2. Add Client Scopes

1. Navigate to the **Client Scopes** tab within the community-app client configuration
2. Click the **Add client scope** button
3. In the **Available Client Scopes** list, locate the scopes you just created:
    - `ALL_FUNCTIONS`
    - `CREATE_DOCUMENT`
4. Select both scopes by checking their checkboxes
5. Click the **Add** button at the bottom of the dialog
6. In the resulting dropdown, select **Default** to make these scopes automatically included in tokens
7. Confirm the addition by clicking **Add** in the final confirmation dialog

### 5.5. Verification

After completing the configuration, verify that the client scopes have been properly assigned:

1. In the **Client Scopes** tab of the community-app client, you should see both `ALL_FUNCTIONS` and `CREATE_DOCUMENT` listed under **Assigned Default Client Scopes**
2. The **Assigned Type** column should show "Default" for both scopes

## 6. Testing the Configuration

### 6.1. Obtaining a New Access Token

To test that the client scopes are properly included in access tokens, obtain a new token using the Authorization Code flow as described in the predecessor document:

1. Navigate to the authorization URL in your browser
2. Log in with appropriate credentials (e.g., `mifos` user)
3. Extract the authorization code from the redirect URL
4. Exchange the code for an access token using the curl command

### 6.2. Examining Token Contents

You can decode and examine the access token to verify that the required scopes are included:

```bash
# If you have the token in the $TOKEN variable
echo $TOKEN | cut -d. -f2 | base64 -d | jq .
```

Look for a `scope` claim in the decoded token that includes the scopes you configured.

### 6.3. Testing Document Upload Functionality

With the properly configured client scopes, test the document upload functionality:

1. Attempt to upload an Excel document through the Fineract interface
2. Verify that the upload completes successfully without authorization errors
3. If you were previously encountering scope-related errors, they should now be resolved

## 7. Troubleshooting

### 7.1. Common Issues

#### 7.1.1. Scopes Not Appearing in Tokens

If the configured scopes are not appearing in access tokens:

- Verify that the client scopes are assigned as **Default** scopes, not Optional
- Ensure that **Include in token scope** is enabled for both client scopes
- Check that you're obtaining a fresh token after making the configuration changes

#### 7.1.2. Document Upload Still Failing

If document uploads continue to fail after configuration:

- Verify that the user account has the appropriate Fineract permissions (as covered in the RBAC section of the predecessor document)
- Check the Fineract application logs for specific error messages
- Ensure that the client secret used in token requests is correct

#### 7.1.3. Authorization Errors

If you encounter 403 Forbidden errors:

- Confirm that the user exists in both Keycloak and Fineract with matching usernames
- Verify that the user has been assigned appropriate roles in Fineract
- Check that the client scopes include the necessary permissions for the attempted action

### 7.2. Validation Steps

To validate your configuration:

1. **Client Scope Creation**: Verify both scopes appear in the Client Scopes list
2. **Client Assignment**: Confirm both scopes are listed as Default in the community-app client
3. **Token Content**: Decode a fresh access token to verify scope inclusion
4. **Functional Testing**: Test the specific functionality that required these scopes

## 8. Security Considerations

### 8.1. Principle of Least Privilege

While the `ALL_FUNCTIONS` scope provides comprehensive access, consider whether all users actually need this level of access. For production environments, you may want to create more granular scopes that align with specific role requirements.

### 8.2. Scope Management

Regularly review and audit your client scopes to ensure they align with your organization's security policies and access control requirements.

### 8.3. Token Lifetime

Consider the implications of token lifetime settings when configuring scopes. Longer-lived tokens with broad scopes may pose higher security risks.

## 9. Advanced Configuration

### 9.1. Custom Protocol Mappers

For advanced use cases, you may need to configure custom protocol mappers within your client scopes to include additional claims or transform existing ones.

### 9.2. Conditional Scopes

Consider implementing conditional logic for scope assignment based on user attributes or group membership for more dynamic access control.

### 9.3. Integration with External Systems

When integrating Fineract with external systems, you may need to configure additional client scopes to support specific integration requirements.

## 10. Best Practices

### 10.1. Naming Conventions

- Use clear, descriptive names for client scopes
- Follow a consistent naming pattern across your organization
- Include the purpose or functionality in the scope name

### 10.2. Documentation

- Maintain documentation of what each client scope enables
- Document the business justification for each scope
- Keep track of which clients use which scopes

### 10.3. Testing

- Always test scope changes in a development environment first
- Verify that existing functionality continues to work after scope modifications
- Test both positive and negative cases to ensure proper access control

## 11. Conclusion

Proper configuration of Keycloak client scopes is essential for enabling full functionality in Fineract applications, particularly for features like document management and file uploads. By following the steps outlined in this document, you can ensure that your Fineract deployment has the necessary OAuth scopes configured to support all required functionalities.

This configuration works in conjunction with the Role-Based Access Control system described in the Fineract User Management Architecture document to provide comprehensive security and access control for your Fineract deployment.

## 12. Related Documentation

- **Fineract User Management Architecture**: The predecessor document covering users, roles, permissions, and basic authentication
- **Keycloak Administration Guide**: Official Keycloak documentation for advanced configuration options
- **OAuth 2.0 and OpenID Connect Specifications**: For understanding the underlying protocols and standards

## 13. Appendix: Quick Reference Commands

### 13.1. Token Verification

```bash
# Decode JWT token payload
echo $TOKEN | cut -d. -f2 | base64 -d | jq .

# Check for specific scopes in token
echo $TOKEN | cut -d. -f2 | base64 -d | jq '.scope'
```

### 13.2. Test Document Upload

```bash
# Example API call to test document upload capability
curl -X POST 'https://localhost:443/fineract-provider/api/v1/offices/uploadtemplate' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Fineract-Platform-TenantId: default' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@/home/menkene-koufan/dev/fin/fineract-setup-app/src/main/resources/data/Offices.xls;type=application/vnd.ms-excel' \
  -F 'locale=en' \
  -F 'dateFormat=dd MMMM yyyy' \
  -k | jq
```

This completes the comprehensive configuration guide for Keycloak client scopes in your Fineract deployment.
