package org.apache.fineract.config.service.loader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.FloatingRate;
import org.apache.fineract.config.model.product.RatePeriod;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for floating interest rate configuration.
 *
 * <p>Creates floating rate benchmarks with rate periods.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>GET /api/v1/floatingrates - List all floating rates
 *   <li>POST /api/v1/floatingrates - Create floating rate
 *   <li>PUT /api/v1/floatingrates/{id} - Update floating rate
 * </ul>
 */
@Slf4j
@Component
public class FloatingRateLoader {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

  private final FineractApiClient apiClient;

  public FloatingRateLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads floating rate configuration.
   *
   * @param floatingRates list of floating rates
   * @param context import context
   * @param result import result
   */
  public void load(List<FloatingRate> floatingRates, ImportContext context, ImportResult result) {
    log.info("Loading {} floating rates", floatingRates.size());

    // Fetch existing floating rates
    Map<String, Long> existingRates = fetchExistingFloatingRates();

    for (FloatingRate rate : floatingRates) {
      try {
        loadSingleFloatingRate(rate, existingRates, context, result);
      } catch (Exception ex) {
        log.error("Failed to load floating rate '{}': {}", rate.getName(), ex.getMessage());
        result.recordEntity("floatingRate", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Fetches existing floating rates from Fineract.
   *
   * @return map of floating rate name to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchExistingFloatingRates() {
    Map<String, Long> rates = new HashMap<>();
    try {
      List<Map<String, Object>> rateList = apiClient.get("/api/v1/floatingrates", List.class);
      for (Map<String, Object> rate : rateList) {
        String name = (String) rate.get("name");
        Number id = (Number) rate.get("id");
        if (name != null && id != null) {
          rates.put(name, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing floating rates: {}", ex.getMessage());
    }
    return rates;
  }

  /**
   * Loads a single floating rate.
   *
   * @param rate floating rate configuration
   * @param existingRates existing rates map
   * @param context import context
   * @param result import result
   */
  private void loadSingleFloatingRate(
      FloatingRate rate,
      Map<String, Long> existingRates,
      ImportContext context,
      ImportResult result) {

    String name = rate.getName();
    log.debug("Loading floating rate: {}", name);

    // Check if already exists
    if (existingRates.containsKey(name)) {
      Long rateId = existingRates.get(name);
      log.info("Floating rate already exists: {} (ID: {})", name, rateId);
      result.recordEntity("floatingRate", ImportResult.EntityAction.UNCHANGED);
      context.registerEntity("floatingRate", name, rateId);
      return;
    }

    // Build request
    Map<String, Object> request = buildCreateRequest(rate);

    // Create floating rate
    Map<String, Object> response = apiClient.post("/api/v1/floatingrates", request, Map.class);
    Long rateId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Floating rate created: {} (ID: {}) with {} rate periods",
        name,
        rateId,
        rate.getRatePeriods().size());
    result.recordEntity("floatingRate", ImportResult.EntityAction.CREATED);
    context.registerEntity("floatingRate", name, rateId);
  }

  /**
   * Builds the API request for creating a floating rate.
   *
   * @param rate floating rate configuration
   * @return request map
   */
  private Map<String, Object> buildCreateRequest(FloatingRate rate) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", rate.getName());
    request.put("locale", "en");
    request.put("dateFormat", "dd MMMM yyyy");

    // Active status
    request.put("isActive", Boolean.TRUE.equals(rate.getIsActive()));

    // Base lending rate
    request.put("isBaseLendingRate", Boolean.TRUE.equals(rate.getIsBaseLendingRate()));

    // Rate periods
    if (rate.getRatePeriods() != null && !rate.getRatePeriods().isEmpty()) {
      List<Map<String, Object>> periods = new ArrayList<>();
      for (RatePeriod period : rate.getRatePeriods()) {
        Map<String, Object> periodMap = new HashMap<>();

        // From date
        LocalDate fromDate = period.getFromDate() != null ? period.getFromDate() : LocalDate.now();
        periodMap.put("fromDate", fromDate.format(DATE_FORMAT));

        // Interest rate
        if (period.getInterestRate() != null) {
          periodMap.put("interestRate", period.getInterestRate());
        } else {
          periodMap.put("interestRate", 0);
        }

        periodMap.put("locale", "en");
        periodMap.put("dateFormat", "dd MMMM yyyy");
        periods.add(periodMap);
      }
      request.put("ratePeriods", periods);
    }

    return request;
  }
}
