package org.apache.fineract.config.service.loader;

import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.accounting.FinancialActivityMapping;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for financial activity mappings.
 *
 * <p>Maps financial activities to GL accounts for automatic posting.
 *
 * <p>API Endpoint: POST /api/v1/financialactivityaccounts
 */
@Slf4j
@Component
public class FinancialActivityMappingLoader {

  private final FineractApiClient apiClient;

  public FinancialActivityMappingLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads financial activity mappings.
   *
   * @param mappings list of mappings
   * @param context import context
   * @param result import result
   */
  public void load(
      List<FinancialActivityMapping> mappings, ImportContext context, ImportResult result) {
    log.debug("Loading {} financial activity mappings", mappings.size());

    for (FinancialActivityMapping mapping : mappings) {
      try {
        loadSingleMapping(mapping, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load financial activity mapping '{}': {}",
            mapping.getFinancialActivity(),
            ex.getMessage());
        result.recordEntity("financialActivityMapping", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single financial activity mapping.
   *
   * @param mapping mapping to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleMapping(
      FinancialActivityMapping mapping, ImportContext context, ImportResult result) {
    log.debug("Loading financial activity mapping: {}", mapping.getFinancialActivity());

    // Check if mapping already exists
    List<Map<String, Object>> existingMappings =
        apiClient.get("/api/v1/financialactivityaccounts", List.class);

    Integer activityId = FineractEnumMapper.mapFinancialActivity(mapping.getFinancialActivity());

    Map<String, Object> existingMapping =
        existingMappings.stream()
            .filter(
                m ->
                    activityId.equals(
                        ((Map<String, Object>) m.get("financialActivityData")).get("id")))
            .findFirst()
            .orElse(null);

    // Resolve GL account
    Long glAccountId = context.resolveEntityId("glAccount", mapping.getGlAccountCode());
    if (glAccountId == null) {
      log.error(
          "GL account '{}' not found for financial activity '{}'",
          mapping.getGlAccountCode(),
          mapping.getFinancialActivity());
      result.recordEntity("financialActivityMapping", ImportResult.EntityAction.FAILED);
      return;
    }

    if (existingMapping != null) {
      // Update existing mapping
      Long mappingId = ((Number) existingMapping.get("id")).longValue();
      Map<String, Object> request =
          RequestBuilder.forConfig().put("glAccountId", glAccountId).build();

      apiClient.put("/api/v1/financialactivityaccounts/" + mappingId, request, Map.class);

      log.info(
          "Financial activity mapping updated: {} -> {}",
          mapping.getFinancialActivity(),
          mapping.getGlAccountCode());
      result.recordEntity("financialActivityMapping", ImportResult.EntityAction.UPDATED);
    } else {
      // Create new mapping
      Map<String, Object> request =
          RequestBuilder.forConfig()
              .put("financialActivityId", activityId)
              .put("glAccountId", glAccountId)
              .build();

      apiClient.post("/api/v1/financialactivityaccounts", request, Map.class);

      log.info(
          "Financial activity mapping created: {} -> {}",
          mapping.getFinancialActivity(),
          mapping.getGlAccountCode());
      result.recordEntity("financialActivityMapping", ImportResult.EntityAction.CREATED);
    }
  }
}
