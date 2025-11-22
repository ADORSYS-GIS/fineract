package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.GuarantorType;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for guarantor type configuration.
 *
 * <p>Creates guarantor types as code values for loan guarantor management.
 *
 * <p>Note: Guarantor types in Fineract are typically managed through code values with a specific
 * code name. This loader creates code values under the "GuarantorRelationship" code.
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
public class GuarantorTypeLoader {

  private static final String GUARANTOR_CODE_NAME = "GuarantorRelationship";

  private final FineractApiClient apiClient;

  public GuarantorTypeLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads guarantor type configuration.
   *
   * @param guarantorTypes list of guarantor types
   * @param context import context
   * @param result import result
   */
  public void load(List<GuarantorType> guarantorTypes, ImportContext context, ImportResult result) {
    log.info("Loading {} guarantor types", guarantorTypes.size());

    // Find or create the guarantor code
    Long guarantorCodeId = findOrCreateGuarantorCode(context, result);
    if (guarantorCodeId == null) {
      log.error(
          "Cannot load guarantor types: failed to find/create code '{}'", GUARANTOR_CODE_NAME);
      return;
    }

    // Fetch existing code values
    Map<String, Long> existingValues = fetchExistingCodeValues(guarantorCodeId);

    for (GuarantorType type : guarantorTypes) {
      try {
        loadSingleGuarantorType(type, guarantorCodeId, existingValues, context, result);
      } catch (Exception ex) {
        log.error("Failed to load guarantor type '{}': {}", type.getName(), ex.getMessage());
        result.recordEntity("guarantorType", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Finds or creates the guarantor code.
   *
   * @param context import context
   * @param result import result
   * @return code ID or null if failed
   */
  @SuppressWarnings("unchecked")
  private Long findOrCreateGuarantorCode(ImportContext context, ImportResult result) {
    // Check context first
    Long codeId = context.getEntityId("code", GUARANTOR_CODE_NAME);
    if (codeId != null) {
      return codeId;
    }

    // Fetch codes from API
    try {
      List<Map<String, Object>> codes = apiClient.get("/api/v1/codes", List.class);
      for (Map<String, Object> code : codes) {
        if (GUARANTOR_CODE_NAME.equals(code.get("name"))) {
          codeId = ((Number) code.get("id")).longValue();
          context.registerEntity("code", GUARANTOR_CODE_NAME, codeId);
          return codeId;
        }
      }

      // Code doesn't exist, create it
      Map<String, Object> request = new HashMap<>();
      request.put("name", GUARANTOR_CODE_NAME);

      Map<String, Object> response = apiClient.post("/api/v1/codes", request, Map.class);
      codeId = ((Number) response.get("resourceId")).longValue();
      context.registerEntity("code", GUARANTOR_CODE_NAME, codeId);
      log.info("Created code '{}' (ID: {})", GUARANTOR_CODE_NAME, codeId);
      result.recordEntity("code", ImportResult.EntityAction.CREATED);

      return codeId;

    } catch (Exception ex) {
      log.error("Failed to find/create guarantor code: {}", ex.getMessage());
      return null;
    }
  }

  /**
   * Fetches existing code values for the guarantor code.
   *
   * @param codeId guarantor code ID
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
   * Loads a single guarantor type as a code value.
   *
   * @param type guarantor type configuration
   * @param codeId code ID
   * @param existingValues existing code values
   * @param context import context
   * @param result import result
   */
  private void loadSingleGuarantorType(
      GuarantorType type,
      Long codeId,
      Map<String, Long> existingValues,
      ImportContext context,
      ImportResult result) {

    String name = type.getName();
    log.debug("Loading guarantor type: {}", name);

    // Check if already exists
    if (existingValues.containsKey(name)) {
      Long valueId = existingValues.get(name);
      log.info("Guarantor type already exists: {} (ID: {})", name, valueId);
      result.recordEntity("guarantorType", ImportResult.EntityAction.UNCHANGED);
      context.registerEntity("guarantorType", name, valueId);
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

    log.info("Guarantor type created: {} (ID: {})", name, valueId);
    result.recordEntity("guarantorType", ImportResult.EntityAction.CREATED);
    context.registerEntity("guarantorType", name, valueId);
    existingValues.put(name, valueId);
  }
}
