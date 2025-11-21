package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.FundSource;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for fund sources.
 *
 * <p>Creates sources of funds for loan products.
 *
 * <p>API Endpoint: POST /api/v1/funds
 */
@Slf4j
@Component
public class FundSourceLoader {

  private final FineractApiClient apiClient;
  private final UpsertService upsertService;

  public FundSourceLoader(FineractApiClient apiClient, UpsertService upsertService) {
    this.apiClient = apiClient;
    this.upsertService = upsertService;
  }

  /**
   * Loads fund sources.
   *
   * @param fundSources list of fund sources
   * @param context import context
   * @param result import result
   */
  public void load(List<FundSource> fundSources, ImportContext context, ImportResult result) {
    log.debug("Loading {} fund sources", fundSources.size());

    for (FundSource fundSource : fundSources) {
      try {
        loadSingleFundSource(fundSource, context, result);
      } catch (Exception ex) {
        log.error("Failed to load fund source '{}': {}", fundSource.getName(), ex.getMessage());
        result.recordEntity("fundSource", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single fund source.
   *
   * <p>Uses upsert logic: updates existing fund source if found (by name), creates new if not.
   *
   * @param fundSource fund source to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleFundSource(
      FundSource fundSource, ImportContext context, ImportResult result) {
    log.debug("Loading fund source: {}", fundSource.getName());

    // Build request data
    Map<String, Object> request = buildRequest(fundSource);

    // Use upsert service (search by name, update if exists, create if not)
    UpsertService.UpsertResult upsertResult =
        upsertService.upsert(
            "/api/v1/funds", request, "name", fundSource.getName(), context, result, "fundSource");

    // Store for reference (important for dependency resolution)
    context.registerEntity("fundSource", fundSource.getName(), upsertResult.getEntityId());

    if (upsertResult.wasCreated()) {
      log.info(
          "✓ Created fund source: {} (ID: {})", fundSource.getName(), upsertResult.getEntityId());
    } else if (upsertResult.wasUpdated()) {
      log.info(
          "✓ Updated fund source: {} (ID: {})", fundSource.getName(), upsertResult.getEntityId());
    } else {
      log.debug(
          "✓ Fund source unchanged: {} (ID: {})", fundSource.getName(), upsertResult.getEntityId());
    }
  }

  /**
   * Builds API request for fund source.
   *
   * @param fundSource fund source
   * @return request map
   */
  private Map<String, Object> buildRequest(FundSource fundSource) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", fundSource.getName());

    if (fundSource.getExternalId() != null) {
      request.put("externalId", fundSource.getExternalId());
    }

    return request;
  }
}
