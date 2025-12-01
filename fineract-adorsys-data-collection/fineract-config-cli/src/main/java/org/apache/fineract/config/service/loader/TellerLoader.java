package org.apache.fineract.config.service.loader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.security.Teller;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for teller configuration.
 *
 * <p>Creates tellers for offices to enable cash management.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>GET /api/v1/tellers - List all tellers
 *   <li>POST /api/v1/tellers - Create teller
 *   <li>PUT /api/v1/tellers/{id} - Update teller
 * </ul>
 */
@Slf4j
@Component
public class TellerLoader {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final FineractApiClient apiClient;

  public TellerLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads teller configuration.
   *
   * @param tellers list of tellers
   * @param context import context
   * @param result import result
   */
  public void load(List<Teller> tellers, ImportContext context, ImportResult result) {
    log.info("Loading {} tellers", tellers.size());

    // Fetch existing tellers for idempotency check
    Map<String, Long> existingTellers = fetchExistingTellers();

    for (Teller teller : tellers) {
      try {
        loadSingleTeller(teller, existingTellers, context, result);
      } catch (Exception ex) {
        log.error("Failed to load teller '{}': {}", teller.getName(), ex.getMessage());
        result.recordEntity("teller", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Fetches existing tellers from Fineract.
   *
   * @return map of teller name to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchExistingTellers() {
    Map<String, Long> tellers = new HashMap<>();

    try {
      List<Map<String, Object>> tellerList = apiClient.get("/api/v1/tellers", List.class);
      for (Map<String, Object> teller : tellerList) {
        String name = (String) teller.get("name");
        Number id = (Number) teller.get("id");
        if (name != null && id != null) {
          tellers.put(name, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing tellers: {}", ex.getMessage());
    }

    return tellers;
  }

  /**
   * Loads a single teller.
   *
   * @param teller teller configuration
   * @param existingTellers map of existing teller names to IDs
   * @param context import context
   * @param result import result
   */
  private void loadSingleTeller(
      Teller teller,
      Map<String, Long> existingTellers,
      ImportContext context,
      ImportResult result) {

    String name = teller.getName();
    log.debug("Loading teller: {}", name);

    // Check if teller already exists
    if (existingTellers.containsKey(name)) {
      Long tellerId = existingTellers.get(name);
      log.info("Teller already exists: {} (ID: {})", name, tellerId);
      result.recordEntity("teller", ImportResult.EntityAction.UNCHANGED);
      context.registerEntity("teller", name, tellerId);
      return;
    }

    // Resolve office ID
    Long officeId = resolveOfficeId(teller.getOfficeName(), context);
    if (officeId == null) {
      log.error("Cannot create teller '{}': office '{}' not found", name, teller.getOfficeName());
      result.recordEntity("teller", ImportResult.EntityAction.FAILED);
      return;
    }

    // Build request
    Map<String, Object> request = buildCreateRequest(teller, officeId);

    // Create teller
    Map<String, Object> response = apiClient.post("/api/v1/tellers", request, Map.class);
    Long tellerId = ((Number) response.get("resourceId")).longValue();

    log.info("Teller created: {} (ID: {}) at office {}", name, tellerId, teller.getOfficeName());
    result.recordEntity("teller", ImportResult.EntityAction.CREATED);
    context.registerEntity("teller", name, tellerId);
  }

  /**
   * Resolves office name to office ID.
   *
   * @param officeName office name
   * @param context import context
   * @return office ID or null if not found
   */
  @SuppressWarnings("unchecked")
  private Long resolveOfficeId(String officeName, ImportContext context) {
    if (officeName == null) {
      return null;
    }

    // Try from context first
    Long officeId = context.getEntityId("office", officeName);
    if (officeId != null) {
      return officeId;
    }

    // Fetch from API
    try {
      List<Map<String, Object>> offices = apiClient.get("/api/v1/offices", List.class);
      for (Map<String, Object> office : offices) {
        if (officeName.equals(office.get("name"))) {
          Long id = ((Number) office.get("id")).longValue();
          context.registerEntity("office", officeName, id);
          return id;
        }
      }
    } catch (Exception ex) {
      log.debug("Could not look up office '{}': {}", officeName, ex.getMessage());
    }

    return null;
  }

  /**
   * Builds the API request for creating a teller.
   *
   * @param teller teller configuration
   * @param officeId office ID
   * @return request map
   */
  private Map<String, Object> buildCreateRequest(Teller teller, Long officeId) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", teller.getName());
    request.put("officeId", officeId);
    request.put("locale", "en");
    request.put("dateFormat", "yyyy-MM-dd");

    if (teller.getDescription() != null) {
      request.put("description", teller.getDescription());
    }

    // Start date defaults to today
    LocalDate startDate = teller.getStartDate() != null ? teller.getStartDate() : LocalDate.now();
    request.put("startDate", startDate.format(DATE_FORMAT));

    if (teller.getEndDate() != null) {
      request.put("endDate", teller.getEndDate().format(DATE_FORMAT));
    }

    // Status defaults to ACTIVE (300)
    String status = teller.getStatus() != null ? teller.getStatus() : "ACTIVE";
    request.put("status", mapTellerStatus(status));

    return request;
  }

  /**
   * Maps teller status string to Fineract status ID.
   *
   * @param status status string
   * @return status ID
   */
  private Integer mapTellerStatus(String status) {
    if (status == null) {
      return 300; // Default to ACTIVE
    }
    return switch (status.toUpperCase().trim()) {
      case "INACTIVE" -> 100;
      case "PENDING" -> 200;
      case "ACTIVE" -> 300;
      case "CLOSED" -> 400;
      default -> 300; // Default to ACTIVE
    };
  }
}
