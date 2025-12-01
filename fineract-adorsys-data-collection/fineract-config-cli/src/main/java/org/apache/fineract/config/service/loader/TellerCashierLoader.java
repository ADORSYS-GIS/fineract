package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.security.TellerCashierMapping;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for teller-cashier assignments.
 *
 * <p>Assigns staff members as cashiers to teller stations.
 *
 * <p>API Endpoint: POST /api/v1/tellers/{tellerId}/cashiers
 */
@Slf4j
@Component
public class TellerCashierLoader {

  private final FineractApiClient apiClient;

  public TellerCashierLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads teller-cashier mappings.
   *
   * @param mappings list of teller-cashier mappings
   * @param context import context
   * @param result import result
   */
  public void load(
      List<TellerCashierMapping> mappings, ImportContext context, ImportResult result) {
    log.info("Loading {} teller-cashier mappings", mappings.size());

    // Build teller name to ID map
    Map<String, Long> tellerMap = fetchTellerMap();
    // Build staff external ID to staff ID map
    Map<String, Long> staffMap = fetchStaffMap();

    for (TellerCashierMapping mapping : mappings) {
      try {
        loadSingleMapping(mapping, tellerMap, staffMap, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load teller-cashier mapping for teller '{}': {}",
            mapping.getTellerName(),
            ex.getMessage());
        result.recordEntity("tellerCashier", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Fetches teller name to ID map.
   *
   * @return map of teller name to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchTellerMap() {
    Map<String, Long> map = new HashMap<>();
    try {
      List<Map<String, Object>> tellers = apiClient.get("/api/v1/tellers", List.class);
      for (Map<String, Object> teller : tellers) {
        String name = (String) teller.get("name");
        Number id = (Number) teller.get("id");
        if (name != null && id != null) {
          map.put(name, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch tellers: {}", ex.getMessage());
    }
    return map;
  }

  /**
   * Fetches staff external ID to staff ID map.
   *
   * @return map of external ID to staff ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchStaffMap() {
    Map<String, Long> map = new HashMap<>();
    try {
      List<Map<String, Object>> staffList = apiClient.get("/api/v1/staff", List.class);
      for (Map<String, Object> staff : staffList) {
        String externalId = (String) staff.get("externalId");
        String displayName = (String) staff.get("displayName");
        Number id = (Number) staff.get("id");
        if (id != null) {
          if (externalId != null) {
            map.put(externalId, id.longValue());
          }
          if (displayName != null) {
            map.put(displayName, id.longValue());
          }
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch staff: {}", ex.getMessage());
    }
    return map;
  }

  /**
   * Loads a single teller-cashier mapping.
   *
   * @param mapping teller-cashier mapping
   * @param tellerMap teller name to ID map
   * @param staffMap staff external ID to staff ID map
   * @param context import context
   * @param result import result
   */
  private void loadSingleMapping(
      TellerCashierMapping mapping,
      Map<String, Long> tellerMap,
      Map<String, Long> staffMap,
      ImportContext context,
      ImportResult result) {

    String tellerName = mapping.getTellerName();
    String staffExternalId = mapping.getStaffExternalId();

    // Skip if no staff specified - cannot create mapping without staff
    if (staffExternalId == null || staffExternalId.isEmpty()) {
      log.warn(
          "Skipping teller cashier mapping for '{}': no staffExternalId specified", tellerName);
      result.recordEntity("tellerCashier", ImportResult.EntityAction.FAILED);
      return;
    }

    log.debug("Loading cashier assignment: {} -> {}", staffExternalId, tellerName);

    // Resolve teller ID
    Long tellerId = tellerMap.get(tellerName);
    if (tellerId == null) {
      tellerId = context.getEntityId("teller", tellerName);
    }
    if (tellerId == null) {
      log.error("Cannot create cashier mapping: teller '{}' not found", tellerName);
      result.recordEntity("tellerCashier", ImportResult.EntityAction.FAILED);
      return;
    }

    // Resolve staff ID
    Long staffId = staffMap.get(staffExternalId);
    if (staffId == null) {
      staffId = context.getEntityId("staff", staffExternalId);
    }
    if (staffId == null) {
      log.error("Cannot create cashier mapping: staff '{}' not found", staffExternalId);
      result.recordEntity("tellerCashier", ImportResult.EntityAction.FAILED);
      return;
    }

    // Check if cashier already assigned
    if (isCashierAlreadyAssigned(tellerId, staffId)) {
      log.info(
          "Cashier already assigned: {} -> {} (teller ID: {})",
          staffExternalId,
          tellerName,
          tellerId);
      result.recordEntity("tellerCashier", ImportResult.EntityAction.UNCHANGED);
      return;
    }

    // Build request
    Map<String, Object> request = buildRequest(mapping, staffId);

    // Create cashier assignment
    Map<String, Object> response =
        apiClient.post("/api/v1/tellers/" + tellerId + "/cashiers", request, Map.class);

    log.info("Cashier assigned: {} -> {} (teller ID: {})", staffExternalId, tellerName, tellerId);
    result.recordEntity("tellerCashier", ImportResult.EntityAction.CREATED);
  }

  /**
   * Checks if a cashier is already assigned to a teller.
   *
   * @param tellerId teller ID
   * @param staffId staff ID
   * @return true if already assigned
   */
  @SuppressWarnings("unchecked")
  private boolean isCashierAlreadyAssigned(Long tellerId, Long staffId) {
    try {
      // API returns: {"tellerId":1,"tellerName":"...","cashiers":[{...}]}
      Object response = apiClient.get("/api/v1/tellers/" + tellerId + "/cashiers", Object.class);

      List<Map<String, Object>> cashiers = null;
      if (response instanceof List) {
        cashiers = (List<Map<String, Object>>) response;
      } else if (response instanceof Map) {
        Map<String, Object> tellerResponse = (Map<String, Object>) response;
        // Check for "cashiers" array (Fineract teller/cashiers endpoint)
        if (tellerResponse.containsKey("cashiers")) {
          cashiers = (List<Map<String, Object>>) tellerResponse.get("cashiers");
        }
        // Check for "pageItems" (paginated response)
        else if (tellerResponse.containsKey("pageItems")) {
          cashiers = (List<Map<String, Object>>) tellerResponse.get("pageItems");
        }
      }

      if (cashiers != null) {
        for (Map<String, Object> cashier : cashiers) {
          Number existingStaffId = (Number) cashier.get("staffId");
          if (existingStaffId != null && existingStaffId.longValue() == staffId) {
            return true;
          }
        }
      }
    } catch (Exception ex) {
      log.debug("Could not check existing cashiers: {}", ex.getMessage());
    }
    return false;
  }

  /**
   * Builds the API request for creating a cashier assignment.
   *
   * @param mapping teller-cashier mapping
   * @param staffId staff ID
   * @return request map
   */
  private Map<String, Object> buildRequest(TellerCashierMapping mapping, Long staffId) {
    Map<String, Object> request = new HashMap<>();

    request.put("staffId", staffId);
    request.put("locale", "en");
    request.put("dateFormat", "dd MMMM yyyy");

    // Start date
    if (mapping.getStartDate() != null && mapping.getStartDate().size() >= 3) {
      List<Integer> date = mapping.getStartDate();
      request.put("startDate", formatDate(date.get(0), date.get(1), date.get(2)));
    } else {
      // Default to today
      java.time.LocalDate today = java.time.LocalDate.now();
      request.put(
          "startDate", today.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    }

    // End date - required by API
    if (mapping.getEndDate() != null && mapping.getEndDate().size() >= 3) {
      List<Integer> date = mapping.getEndDate();
      request.put("endDate", formatDate(date.get(0), date.get(1), date.get(2)));
    } else {
      // Default to 1 year from start date if not specified
      java.time.LocalDate endDate = java.time.LocalDate.now().plusYears(1);
      request.put(
          "endDate", endDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    }

    // Full day
    request.put("isFullDay", mapping.getIsFullDay() != null ? mapping.getIsFullDay() : true);

    // Times for part-day
    if (Boolean.FALSE.equals(mapping.getIsFullDay())) {
      if (mapping.getStartTime() != null) {
        request.put("startTime", mapping.getStartTime());
      }
      if (mapping.getEndTime() != null) {
        request.put("endTime", mapping.getEndTime());
      }
    }

    if (mapping.getDescription() != null) {
      request.put("description", mapping.getDescription());
    }

    return request;
  }

  /**
   * Formats a date in Fineract format.
   *
   * @param year year
   * @param month month
   * @param day day
   * @return formatted date string
   */
  private String formatDate(int year, int month, int day) {
    java.time.LocalDate date = java.time.LocalDate.of(year, month, day);
    return date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"));
  }
}
