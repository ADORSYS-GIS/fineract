package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.CollateralType;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for collateral type configuration.
 *
 * <p>Creates collateral types for loan collateral management.
 *
 * <p>Note: Collateral types in Fineract are typically managed through code values with a specific
 * code name. This loader creates code values under the "CollateralType" or "LoanCollateral" code.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>GET /api/v1/codes - List all codes
 *   <li>POST /api/v1/codes/{codeId}/codevalues - Create code value
 * </ul>
 */
@Slf4j
@Component
public class CollateralTypeLoader {

  private static final String COLLATERAL_CODE_NAME = "LoanCollateral";

  private final FineractApiClient apiClient;

  public CollateralTypeLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads collateral type configuration.
   *
   * @param collateralTypes list of collateral types
   * @param context import context
   * @param result import result
   */
  public void load(
      List<CollateralType> collateralTypes, ImportContext context, ImportResult result) {
    log.info("Loading {} collateral types", collateralTypes.size());

    // Find or create the collateral code
    Long collateralCodeId = findOrCreateCollateralCode(context, result);
    if (collateralCodeId == null) {
      log.error(
          "Cannot load collateral types: failed to find/create code '{}'", COLLATERAL_CODE_NAME);
      return;
    }

    // Fetch existing code values
    Map<String, Long> existingValues = fetchExistingCodeValues(collateralCodeId);

    for (CollateralType type : collateralTypes) {
      try {
        loadSingleCollateralType(type, collateralCodeId, existingValues, context, result);
      } catch (Exception ex) {
        log.error("Failed to load collateral type '{}': {}", type.getName(), ex.getMessage());
        result.recordEntity("collateralType", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Finds or creates the collateral code.
   *
   * @param context import context
   * @param result import result
   * @return code ID or null if failed
   */
  @SuppressWarnings("unchecked")
  private Long findOrCreateCollateralCode(ImportContext context, ImportResult result) {
    // Check context first
    Long codeId = context.getEntityId("code", COLLATERAL_CODE_NAME);
    if (codeId != null) {
      return codeId;
    }

    // Fetch codes from API
    try {
      List<Map<String, Object>> codes = apiClient.get("/api/v1/codes", List.class);
      for (Map<String, Object> code : codes) {
        if (COLLATERAL_CODE_NAME.equals(code.get("name"))) {
          codeId = ((Number) code.get("id")).longValue();
          context.registerEntity("code", COLLATERAL_CODE_NAME, codeId);
          return codeId;
        }
      }

      // Code doesn't exist, create it
      Map<String, Object> request = new HashMap<>();
      request.put("name", COLLATERAL_CODE_NAME);

      Map<String, Object> response = apiClient.post("/api/v1/codes", request, Map.class);
      codeId = ((Number) response.get("resourceId")).longValue();
      context.registerEntity("code", COLLATERAL_CODE_NAME, codeId);
      log.info("Created code '{}' (ID: {})", COLLATERAL_CODE_NAME, codeId);
      result.recordEntity("code", ImportResult.EntityAction.CREATED);

      return codeId;

    } catch (Exception ex) {
      log.error("Failed to find/create collateral code: {}", ex.getMessage());
      return null;
    }
  }

  /**
   * Fetches existing code values for the collateral code.
   *
   * @param codeId collateral code ID
   * @return map of code value name to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchExistingCodeValues(Long codeId) {
    Map<String, Long> values = new HashMap<>();
    try {
      List<Map<String, Object>> valueList =
          apiClient.get("/api/v1/codes/" + codeId + "/codevalues", List.class);
      for (Map<String, Object> value : valueList) {
        String name = (String) value.get("name");
        Number id = (Number) value.get("id");
        if (name != null && id != null) {
          values.put(name, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing code values: {}", ex.getMessage());
    }
    return values;
  }

  /**
   * Loads a single collateral type as a code value.
   *
   * @param type collateral type configuration
   * @param codeId code ID
   * @param existingValues existing code values
   * @param context import context
   * @param result import result
   */
  private void loadSingleCollateralType(
      CollateralType type,
      Long codeId,
      Map<String, Long> existingValues,
      ImportContext context,
      ImportResult result) {

    String name = type.getName();
    log.debug("Loading collateral type: {}", name);

    // Check if already exists
    if (existingValues.containsKey(name)) {
      Long valueId = existingValues.get(name);
      log.info("Collateral type already exists: {} (ID: {})", name, valueId);
      result.recordEntity("collateralType", ImportResult.EntityAction.UNCHANGED);
      context.registerEntity("collateralType", name, valueId);
      return;
    }

    // Create code value (note: locale is not supported by this API)
    Map<String, Object> request = new HashMap<>();
    request.put("name", name);

    if (type.getDescription() != null) {
      request.put("description", type.getDescription());
    }

    // Position determines order in dropdown
    request.put("position", existingValues.size() + 1);
    request.put("isActive", true);

    Map<String, Object> response =
        apiClient.post("/api/v1/codes/" + codeId + "/codevalues", request, Map.class);
    Long valueId = ((Number) response.get("resourceId")).longValue();

    log.info("Collateral type created: {} (ID: {})", name, valueId);
    result.recordEntity("collateralType", ImportResult.EntityAction.CREATED);
    context.registerEntity("collateralType", name, valueId);
    existingValues.put(name, valueId);
  }
}
