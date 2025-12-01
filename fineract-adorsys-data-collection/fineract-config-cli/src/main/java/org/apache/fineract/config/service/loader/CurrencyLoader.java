package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.CurrencyConfig;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for currency configuration.
 *
 * <p>Configures the currency for the Fineract tenant.
 *
 * <p>API Endpoint: PUT /api/v1/currencies
 */
@Slf4j
@Component
public class CurrencyLoader {

  private final FineractApiClient apiClient;

  public CurrencyLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads currency configuration.
   *
   * @param currencyConfig currency configuration
   * @param context import context
   * @param result import result
   */
  public void load(CurrencyConfig currencyConfig, ImportContext context, ImportResult result) {
    log.debug("Loading currency: {}", currencyConfig.getCode());

    try {
      // Fineract currency API expects a list of currency codes
      Map<String, Object> request = new HashMap<>();
      request.put("currencies", List.of(currencyConfig.getCode()));

      // Update currency configuration (system singleton - always an update)
      Map<String, Object> response = apiClient.put("/api/v1/currencies", request, Map.class);

      log.info("Currency configured: {} ({})", currencyConfig.getName(), currencyConfig.getCode());

      result.recordEntity("currency", ImportResult.EntityAction.UPDATED);

    } catch (Exception ex) {
      log.error("Failed to configure currency: {}", ex.getMessage(), ex);
      result.recordEntity("currency", ImportResult.EntityAction.FAILED);
      throw ex;
    }
  }
}
