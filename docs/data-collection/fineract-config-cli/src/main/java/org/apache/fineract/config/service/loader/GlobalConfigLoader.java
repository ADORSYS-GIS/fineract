package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.GlobalConfigItem;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for global configuration items.
 *
 * <p>Updates global configuration settings for the Fineract tenant.
 *
 * <p>API Endpoint: PUT /api/v1/configurations/{configId}
 */
@Slf4j
@Component
public class GlobalConfigLoader {

  private final FineractApiClient apiClient;

  public GlobalConfigLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads global configuration items.
   *
   * @param globalConfigItems list of global config items
   * @param context import context
   * @param result import result
   */
  public void load(
      List<GlobalConfigItem> globalConfigItems, ImportContext context, ImportResult result) {
    log.debug("Loading {} global configuration items", globalConfigItems.size());

    for (GlobalConfigItem item : globalConfigItems) {
      try {
        loadSingleItem(item, context, result);
      } catch (Exception ex) {
        log.error("Failed to load global config item '{}': {}", item.getName(), ex.getMessage());
        result.recordEntity("globalConfig", ImportResult.EntityAction.FAILED);
        // Continue with other items
      }
    }
  }

  /**
   * Loads a single global configuration item.
   *
   * @param item configuration item
   * @param context import context
   * @param result import result
   */
  private void loadSingleItem(GlobalConfigItem item, ImportContext context, ImportResult result) {
    log.debug("Loading global config: {}", item.getName());

    // First, get all configurations to find the config ID by name
    // API returns: {"globalConfiguration": [...]}
    Map<String, Object> response = apiClient.get("/api/v1/configurations", Map.class);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> configs =
        (List<Map<String, Object>>) response.get("globalConfiguration");

    Map<String, Object> matchingConfig =
        configs.stream()
            .filter(c -> item.getName().equals(c.get("name")))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Global configuration not found: " + item.getName()));

    Long configId = ((Number) matchingConfig.get("id")).longValue();

    // Update the configuration
    Map<String, Object> request = new HashMap<>();
    request.put("enabled", item.isEnabled());

    if (item.getValue() != null) {
      request.put("value", item.getValue());
    }

    if (item.getStringValue() != null) {
      request.put("stringValue", item.getStringValue());
    }

    if (item.getDateValue() != null) {
      request.put("dateValue", item.getDateValue());
    }

    apiClient.put("/api/v1/configurations/" + configId, request, Map.class);

    log.info(
        "Global config updated: {} = {} (enabled: {})",
        item.getName(),
        item.getValue() != null ? item.getValue() : item.getStringValue(),
        item.isEnabled());

    result.recordEntity("globalConfig", ImportResult.EntityAction.UPDATED);
  }
}
