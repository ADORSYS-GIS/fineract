package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.Code;
import org.apache.fineract.config.model.systemconfig.CodeValue;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for codes and code values.
 *
 * <p>Creates codes (categories) and their associated code values (options).
 *
 * <p>API Endpoints: - POST /api/v1/codes - POST /api/v1/codes/{codeId}/codevalues
 */
@Slf4j
@Component
public class CodesLoader {

  private final FineractApiClient apiClient;
  private final UpsertService upsertService;

  public CodesLoader(FineractApiClient apiClient, UpsertService upsertService) {
    this.apiClient = apiClient;
    this.upsertService = upsertService;
  }

  /**
   * Loads codes and code values.
   *
   * @param codes list of codes
   * @param context import context
   * @param result import result
   */
  public void load(List<Code> codes, ImportContext context, ImportResult result) {
    log.debug("Loading {} codes", codes.size());

    for (Code code : codes) {
      try {
        loadSingleCode(code, context, result);
      } catch (Exception ex) {
        log.error("Failed to load code '{}': {}", code.getName(), ex.getMessage());
        result.recordEntity("code", ImportResult.EntityAction.FAILED);
        // Continue with other codes
      }
    }
  }

  /**
   * Loads a single code and its values.
   *
   * <p>Uses upsert logic for codes: updates existing code if found (by name), creates new if not.
   *
   * @param code code to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleCode(Code code, ImportContext context, ImportResult result) {
    log.debug("Loading code: {}", code.getName());

    // Build request data for code
    Map<String, Object> request = new HashMap<>();
    request.put("name", code.getName());

    // Use upsert service (search by name, update if exists, create if not)
    UpsertService.UpsertResult upsertResult =
        upsertService.upsert(
            "/api/v1/codes", request, "name", code.getName(), context, result, "code");

    Long codeId = upsertResult.getEntityId();

    // Store code ID for reference (important for dependency resolution)
    context.registerEntity("code", code.getName(), codeId);

    if (upsertResult.wasCreated()) {
      log.info("✓ Created code: {} (ID: {})", code.getName(), codeId);
    } else if (upsertResult.wasUpdated()) {
      log.info("✓ Updated code: {} (ID: {})", code.getName(), codeId);
    } else {
      log.debug("✓ Code unchanged: {} (ID: {})", code.getName(), codeId);
    }

    // Load code values
    if (code.getValues() != null && !code.getValues().isEmpty()) {
      loadCodeValues(codeId, code.getValues(), context, result);
    }
  }

  /**
   * Loads code values for a code.
   *
   * @param codeId code ID
   * @param codeValues list of code values
   * @param context import context
   * @param result import result
   */
  private void loadCodeValues(
      Long codeId, List<CodeValue> codeValues, ImportContext context, ImportResult result) {
    log.debug("Loading {} code values for code ID {}", codeValues.size(), codeId);

    // Get existing code values directly from the codevalues endpoint
    // (the /codes/{id} endpoint doesn't include codeValues in Fineract 1.x)
    List<Map<String, Object>> existingValues;
    try {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> values =
          apiClient.get("/api/v1/codes/" + codeId + "/codevalues", List.class);
      existingValues = values != null ? values : new java.util.ArrayList<>();
    } catch (Exception ex) {
      log.debug("Could not retrieve existing code values for code {}: {}", codeId, ex.getMessage());
      existingValues = new java.util.ArrayList<>();
    }

    for (CodeValue codeValue : codeValues) {
      try {
        loadSingleCodeValue(codeId, codeValue, existingValues, context, result);
      } catch (Exception ex) {
        log.error("Failed to load code value '{}': {}", codeValue.getName(), ex.getMessage());
        result.recordEntity("codeValue", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single code value.
   *
   * <p>Uses upsert logic: updates existing code value if found (by name), creates new if not.
   *
   * @param codeId code ID
   * @param codeValue code value to load
   * @param existingValues existing code values
   * @param context import context
   * @param result import result
   */
  private void loadSingleCodeValue(
      Long codeId,
      CodeValue codeValue,
      List<Map<String, Object>> existingValues,
      ImportContext context,
      ImportResult result) {

    // Build request data
    Map<String, Object> request = new HashMap<>();
    request.put("name", codeValue.getName());

    if (codeValue.getPosition() != null) {
      request.put("position", codeValue.getPosition());
    }

    if (codeValue.getDescription() != null) {
      request.put("description", codeValue.getDescription());
    }

    if (codeValue.getIsActive() != null) {
      request.put("isActive", codeValue.getIsActive());
    }

    // Check if code value already exists
    Map<String, Object> existingValue =
        existingValues.stream()
            .filter(v -> codeValue.getName().equals(v.get("name")))
            .findFirst()
            .orElse(null);

    Long codeValueId;
    boolean created = false;
    boolean updated = false;

    if (existingValue != null) {
      // Code value exists - update it
      codeValueId = ((Number) existingValue.get("id")).longValue();
      apiClient.put("/api/v1/codes/" + codeId + "/codevalues/" + codeValueId, request, Map.class);
      updated = true;
      result.recordEntity("codeValue", ImportResult.EntityAction.UPDATED);
    } else {
      // Code value doesn't exist - create it
      Map<String, Object> response =
          apiClient.post("/api/v1/codes/" + codeId + "/codevalues", request, Map.class);
      codeValueId = ((Number) response.get("resourceId")).longValue();
      created = true;
      result.recordEntity("codeValue", ImportResult.EntityAction.CREATED);
    }

    // Store for reference (important for dependency resolution)
    context.registerEntity("codeValue", codeValue.getName(), codeValueId);

    if (created) {
      log.info("  ✓ Created code value: {} (ID: {})", codeValue.getName(), codeValueId);
    } else if (updated) {
      log.info("  ✓ Updated code value: {} (ID: {})", codeValue.getName(), codeValueId);
    } else {
      log.debug("  ✓ Code value unchanged: {} (ID: {})", codeValue.getName(), codeValueId);
    }
  }
}
