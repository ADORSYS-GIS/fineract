package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.model.security.Role;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.ChangeDetectionService;
import org.apache.fineract.config.util.InputValidator;
import org.apache.fineract.config.util.InputValidator.ValidationResult;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for roles and permissions.
 *
 * <p>Creates roles and assigns permissions to them.
 *
 * <p>API Endpoints: - POST /api/v1/roles - PUT /api/v1/roles/{roleId}/permissions
 */
@Slf4j
@Component
public class RoleLoader {

  private final FineractApiClient apiClient;
  private final ChangeDetectionService changeDetectionService;
  private final InputValidator inputValidator;

  public RoleLoader(
      FineractApiClient apiClient,
      ChangeDetectionService changeDetectionService,
      InputValidator inputValidator) {
    this.apiClient = apiClient;
    this.changeDetectionService = changeDetectionService;
    this.inputValidator = inputValidator;
  }

  /**
   * Loads roles.
   *
   * @param roles list of roles
   * @param context import context
   * @param result import result
   */
  public void load(List<Role> roles, ImportContext context, ImportResult result) {
    log.debug("Loading {} roles", roles.size());

    for (Role role : roles) {
      try {
        loadSingleRole(role, context, result);
      } catch (Exception ex) {
        log.error("Failed to load role '{}': {}", role.getName(), ex.getMessage());
        result.recordEntity("role", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single role.
   *
   * @param role role to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleRole(Role role, ImportContext context, ImportResult result) {
    log.debug("Loading role: {}", role.getName());

    // Validate input data
    List<ValidationResult> validationResults = new ArrayList<>();
    validationResults.add(inputValidator.validateEntityIdentifier(role.getName(), "name"));
    validationResults.add(inputValidator.validateDescription(role.getDescription(), "description"));

    ValidationResult validation = inputValidator.validateAll(validationResults);
    if (!validation.isValid()) {
      String errors = validation.getErrorMessage();
      log.error("Validation failed for role '{}': {}", role.getName(), errors);
      throw new IllegalArgumentException("Invalid role data: " + errors);
    }

    // Check if role already exists
    List<Map<String, Object>> existingRoles = apiClient.get("/api/v1/roles", List.class);

    Map<String, Object> existingRole =
        existingRoles.stream()
            .filter(r -> role.getName().equals(r.get("name")))
            .findFirst()
            .orElse(null);

    Long roleId;
    boolean roleUpdated = false;

    if (existingRole == null) {
      // Create new role
      Map<String, Object> request = new HashMap<>();
      request.put("name", role.getName());
      request.put("description", role.getDescription());

      Map<String, Object> response = apiClient.post("/api/v1/roles", request, Map.class);
      roleId = ((Number) response.get("resourceId")).longValue();

      log.info("Role created: {} (ID: {})", role.getName(), roleId);
      result.recordEntity("role", ImportResult.EntityAction.CREATED);
    } else {
      roleId = ((Number) existingRole.get("id")).longValue();
      log.debug("Role already exists: {} (ID: {})", role.getName(), roleId);

      // Check for changes in description or disabled state
      Map<String, Object> proposedData = new HashMap<>();
      proposedData.put("description", role.getDescription());
      if (role.getDisabled() != null) {
        proposedData.put("disabled", role.getDisabled());
      }

      Map<String, PlannedChange.FieldChange> changes =
          changeDetectionService.detectChangesForEntityType("role", existingRole, proposedData);

      if (!changes.isEmpty()) {
        // Update role properties
        log.info("Updating role: {} ({} fields changed)", role.getName(), changes.size());
        apiClient.put("/api/v1/roles/" + roleId, proposedData, Map.class);
        roleUpdated = true;
      }
    }

    // Store for reference
    context.registerEntity("role", role.getName(), roleId);

    // Merge maker-checker permissions from MakerCheckerConfigLoader
    List<String> allPermissions = new ArrayList<>();
    if (role.getPermissions() != null) {
      allPermissions.addAll(role.getPermissions());
    }

    // Add maker-checker permissions if available in context
    List<String> makerCheckerPermissions = getMakerCheckerPermissions(role.getName(), context);
    if (!makerCheckerPermissions.isEmpty()) {
      log.info(
          "  Adding {} maker-checker permission(s) to role '{}'",
          makerCheckerPermissions.size(),
          role.getName());
      for (String perm : makerCheckerPermissions) {
        if (!allPermissions.contains(perm)) {
          allPermissions.add(perm);
          log.debug("    + {}", perm);
        }
      }
    }

    // Assign all permissions (base + maker-checker) - check for changes
    if (!allPermissions.isEmpty()) {
      boolean permissionsUpdated = assignPermissionsIfChanged(roleId, allPermissions, existingRole);
      if (permissionsUpdated) {
        roleUpdated = true;
      }
    }

    // Record final status
    if (existingRole != null && roleUpdated) {
      result.recordEntity("role", ImportResult.EntityAction.UPDATED);
    } else if (existingRole != null) {
      result.recordEntity("role", ImportResult.EntityAction.UNCHANGED);
    }
  }

  /**
   * Gets maker-checker permissions for a role from the import context.
   *
   * <p>MakerCheckerConfigLoader stores role-permission mappings in context under the key
   * "makerCheckerMappings". This method retrieves those mappings for the current role.
   *
   * @param roleName role name
   * @param context import context
   * @return list of permission codes for this role
   */
  @SuppressWarnings("unchecked")
  private List<String> getMakerCheckerPermissions(String roleName, ImportContext context) {
    Map<String, Object> customData = context.getCustomData();
    if (!customData.containsKey("makerCheckerMappings")) {
      return List.of();
    }

    Map<String, List<String>> makerCheckerMappings =
        (Map<String, List<String>>) customData.get("makerCheckerMappings");

    return makerCheckerMappings.getOrDefault(roleName, List.of());
  }

  /**
   * Assigns permissions to a role if they have changed.
   *
   * @param roleId role ID
   * @param permissionCodes list of desired permission codes
   * @param existingRole existing role data (may be null for new roles)
   * @return true if permissions were updated, false if unchanged
   */
  @SuppressWarnings("unchecked")
  private boolean assignPermissionsIfChanged(
      Long roleId, List<String> permissionCodes, Map<String, Object> existingRole) {
    log.debug("Checking permissions for role ID {}", roleId);

    try {
      // Get current permissions for the role from the API (to ensure we have Fresh
      // status)
      Map<String, Object> roleWithPerms =
          apiClient.get("/api/v1/roles/" + roleId + "/permissions", Map.class);
      List<Map<String, Object>> usageData =
          (List<Map<String, Object>>) roleWithPerms.get("permissionUsageData");

      if (usageData == null) {
        log.warn("No permission usage data found for role {}", roleId);
        return false;
      }

      // Identify permissions that need to be enabled
      Map<String, Boolean> permissionsToEnable = new HashMap<>();

      for (String code : permissionCodes) {
        // Find the permission in usageData
        Map<String, Object> permissionInfo =
            usageData.stream().filter(p -> code.equals(p.get("code"))).findFirst().orElse(null);

        if (permissionInfo != null) {
          Boolean selected = (Boolean) permissionInfo.get("selected");
          if (selected == null || !selected) {
            permissionsToEnable.put(code, true);
          }
        } else {
          log.warn("Permission code not found in system: {}", code);
          // Optionally still try to enable it if it's valid but missing from response
          // (unlikely)
        }
      }

      if (!permissionsToEnable.isEmpty()) {
        Map<String, Object> request = new HashMap<>();
        request.put("permissions", permissionsToEnable);

        apiClient.put("/api/v1/roles/" + roleId + "/permissions", request, Map.class);

        log.info(
            "Updated permissions for role ID {} (enabling {} permissions)",
            roleId,
            permissionsToEnable.size());
        return true;
      }

      log.debug("Permissions unchanged for role ID {}", roleId);
      return false;

    } catch (Exception ex) {
      log.error("Failed to check/assign permissions to role {}: {}", roleId, ex.getMessage());
      // Print stack trace for debugging
      ex.printStackTrace();
      return false;
    }
  }

  /**
   * Disables a role.
   *
   * @param roleId role ID
   */
  private void disableRole(Long roleId) {
    try {
      Map<String, Object> request = new HashMap<>();
      request.put("disabled", true);

      apiClient.put("/api/v1/roles/" + roleId, request, Map.class);
      log.info("Role ID {} disabled", roleId);
    } catch (Exception ex) {
      log.error("Failed to disable role {}: {}", roleId, ex.getMessage());
    }
  }
}
