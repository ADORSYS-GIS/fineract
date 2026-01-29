package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.security.MakerCheckerConfig;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for maker-checker configuration.
 *
 * <p>
 * Enables or disables maker-checker (dual authorization) for critical
 * operations in Fineract.
 *
 * <p>
 * NOTE: Fineract's maker-checker is boolean-only (enabled/disabled). When
 * enabled, ALL
 * operations of that type will require approval. Amount-based thresholds are
 * NOT supported by the
 * native Fineract API.
 *
 * <p>
 * API Endpoint: PUT /api/v1/permissions
 *
 * <p>
 * How it works:
 *
 * <ul>
 * <li>Reads maker-checker configurations with enabled=true
 * <li>Builds permission code (e.g., "APPROVE_LOAN" from action="APPROVE" and
 * entity="Loan")
 * <li>Enables the permission via PUT /api/v1/permissions
 * <li>Tracks maker and checker roles for permission auto-assignment (see
 * RoleLoader)
 * </ul>
 */
@Slf4j
@Component
public class MakerCheckerConfigLoader {

  private final FineractApiClient apiClient;

  public MakerCheckerConfigLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads maker-checker configuration.
   *
   * <p>
   * Processes all configurations (enabled=true to enable, enabled=false to
   * disable).
   *
   * @param configs list of maker-checker configurations
   * @param context import context (stores role-permission mappings for
   *                RoleLoader)
   * @param result  import result
   */
  public void load(List<MakerCheckerConfig> configs, ImportContext context, ImportResult result) {
    log.info("=" + "=".repeat(79));
    log.info("PROCESSING MAKER-CHECKER (DUAL AUTHORIZATION)");
    log.info("=" + "=".repeat(79));

    log.info("Processing {} configured maker-checker permissions", configs.size());
    log.info("");

    // Fetch available permissions from Fineract for validation
    Set<String> availablePermissions = fetchAvailablePermissions();

    int successCount = 0;

    for (MakerCheckerConfig config : configs) {
      try {
        boolean success = processSingleMakerChecker(config, availablePermissions, context, result);
        if (success) {
          successCount++;
        }
      } catch (Exception ex) {
        log.error("Failed to process maker-checker '{}': {}", config.getTaskName(), ex.getMessage());
        result.recordEntity("makerCheckerConfig", ImportResult.EntityAction.FAILED);
      }
    }

    // Summary
    log.info("");
    log.info(
        "✓ Successfully processed maker-checker for {}/{} requested operations",
        successCount,
        configs.size());
    log.info("");
  }

  /**
   * Fetches available permissions from Fineract for validation.
   *
   * @return set of available permission codes
   */
  private Set<String> fetchAvailablePermissions() {
    try {
      List<Map<String, Object>> permissions = apiClient.get("/api/v1/permissions", List.class);
      Set<String> permissionCodes = permissions.stream()
          .map(p -> (String) p.get("code"))
          .filter(code -> code != null)
          .collect(Collectors.toSet());

      log.debug("Found {} permissions in Fineract", permissionCodes.size());
      return permissionCodes;

    } catch (Exception ex) {
      log.warn("Could not fetch available permissions: {}", ex.getMessage());
      return Set.of();
    }
  }

  /**
   * Processes a single maker-checker configuration (enable or disable).
   *
   * @param config               maker-checker configuration
   * @param availablePermissions set of available permissions (for validation)
   * @param context              import context (for storing role-permission
   *                             mappings)
   * @param result               import result
   * @return true if successfully processed, false otherwise
   */
  private boolean processSingleMakerChecker(
      MakerCheckerConfig config,
      Set<String> availablePermissions,
      ImportContext context,
      ImportResult result) {

    String entity = config.getEntity().toUpperCase();
    String action = config.getAction().toUpperCase();
    String permissionCode = action + "_" + entity;
    boolean shouldEnable = Boolean.TRUE.equals(config.getEnabled());
    String actionName = shouldEnable ? "Enabling" : "Disabling";

    log.info("  {}: {}", actionName, config.getTaskName());
    log.info("    Permission code: {}", permissionCode);

    // Check if permission exists in available permissions
    if (!availablePermissions.isEmpty() && !availablePermissions.contains(permissionCode)) {
      log.warn("    ⚠ Permission {} not found in available permissions", permissionCode);
      log.info("    Searching for similar permissions...");

      // Search for similar permissions
      List<String> similar = availablePermissions.stream()
          .filter(p -> p.contains(entity) || p.contains(action))
          .limit(5)
          .collect(Collectors.toList());

      if (!similar.isEmpty()) {
        log.info("    Similar permissions found: {}", similar);
      }

      result.recordEntity("makerCheckerConfig", ImportResult.EntityAction.FAILED);
      return false;
    }

    try {
      // Enable or disable this single permission
      Map<String, Object> request = new HashMap<>();
      Map<String, Object> permissions = new HashMap<>();
      permissions.put(permissionCode, shouldEnable);
      request.put("permissions", permissions);

      apiClient.put("/api/v1/permissions", request, Map.class);

      log.info("    ✓ {} successfully", shouldEnable ? "Enabled" : "Disabled");
      if (shouldEnable) {
        log.info("      Maker: {}, Checker: {}", config.getMakerRole(), config.getCheckerRole());
        log.info("      Note: ALL {} operations on {} will require approval", action, entity);
      } else {
        log.info("      Note: Operations on {} will NOT require approval", entity);
      }

      result.recordEntity("makerCheckerConfig", ImportResult.EntityAction.UPDATED);

      // Store maker and checker role mappings for RoleLoader to use ONLY if enabling
      if (shouldEnable) {
        storeMakerCheckerRoleMapping(config, permissionCode, context);
      }

      return true;

    } catch (Exception ex) {
      String errorMsg = ex.getMessage().toLowerCase();

      if (errorMsg.contains("404") && errorMsg.contains("does not exist")
          || errorMsg.contains("permission with code")) {
        log.warn("    ⚠ Permission not available in this Fineract version: {}", permissionCode);
        result.recordEntity("makerCheckerConfig", ImportResult.EntityAction.FAILED);
      } else {
        log.warn("    ⚠ Failed: {}", ex.getMessage());
        result.recordEntity("makerCheckerConfig", ImportResult.EntityAction.FAILED);
      }

      return false;
    }
  }

  /**
   * Stores maker-checker role-permission mapping in context for RoleLoader.
   *
   * <p>
   * This allows RoleLoader to automatically assign:
   *
   * <ul>
   * <li>Base permission (e.g., APPROVE_LOAN) to maker role
   * <li>Checker permission (e.g., CHECKER_APPROVE_LOAN) to checker role
   * </ul>
   *
   * @param config         maker-checker configuration
   * @param permissionCode permission code (e.g., "APPROVE_LOAN")
   * @param context        import context
   */
  @SuppressWarnings("unchecked")
  private void storeMakerCheckerRoleMapping(
      MakerCheckerConfig config, String permissionCode, ImportContext context) {

    // Get or create maker-checker mappings in context
    Map<String, List<String>> makerCheckerMappings = (Map<String, List<String>>) context.getCustomData()
        .computeIfAbsent("makerCheckerMappings", k -> new HashMap<>());

    String checkerPermissionCode = "CHECKER_" + permissionCode;

    // Add base permission to maker role
    if (config.getMakerRole() != null) {
      List<String> makerPermissions = makerCheckerMappings.computeIfAbsent(
          config.getMakerRole(), k -> new java.util.ArrayList<>());
      if (!makerPermissions.contains(permissionCode)) {
        makerPermissions.add(permissionCode);
      }
    }

    // Add checker permission to checker role
    if (config.getCheckerRole() != null) {
      List<String> checkerPermissions = makerCheckerMappings.computeIfAbsent(
          config.getCheckerRole(), k -> new java.util.ArrayList<>());
      if (!checkerPermissions.contains(checkerPermissionCode)) {
        checkerPermissions.add(checkerPermissionCode);
      }
    }

    log.debug(
        "    Stored role mappings: {} -> [{}], {} -> [{}]",
        config.getMakerRole(),
        permissionCode,
        config.getCheckerRole(),
        checkerPermissionCode);
  }
}
