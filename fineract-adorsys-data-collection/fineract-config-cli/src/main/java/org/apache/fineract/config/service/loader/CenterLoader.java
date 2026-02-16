package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.model.client.Center;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.ChangeDetectionService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for centers.
 *
 * <p>Creates center records (collections of groups).
 *
 * <p>API Endpoint: POST /api/v1/centers
 */
@Slf4j
@Component
public class CenterLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");
  private static final String LOCALE = "en";
  private static final String DATE_FORMAT = "dd MMMM yyyy";

  private final FineractApiClient apiClient;
  private final ChangeDetectionService changeDetectionService;

  public CenterLoader(FineractApiClient apiClient, ChangeDetectionService changeDetectionService) {
    this.apiClient = apiClient;
    this.changeDetectionService = changeDetectionService;
  }

  /**
   * Loads centers.
   *
   * @param centers list of centers
   * @param context import context
   * @param result import result
   */
  public void load(List<Center> centers, ImportContext context, ImportResult result) {
    log.debug("Loading {} centers", centers.size());

    for (Center center : centers) {
      try {
        loadSingleCenter(center, context, result);
      } catch (Exception ex) {
        log.error("Failed to load center '{}': {}", center.getName(), ex.getMessage());
        result.recordEntity("center", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single center.
   *
   * @param center center to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleCenter(Center center, ImportContext context, ImportResult result) {
    log.debug("Loading center: {}", center.getName());

    Long centerId = null;
    Map<String, Object> existingCenter = null;

    // Check if center already exists by external ID
    if (center.getExternalId() != null) {
      try {
        // Centers search endpoint returns an array directly (not a paged response)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> searchResults =
            apiClient.get("/api/v1/centers?externalId=" + center.getExternalId(), List.class);

        if (searchResults != null && !searchResults.isEmpty()) {
          existingCenter = searchResults.get(0);
          centerId = ((Number) existingCenter.get("id")).longValue();
          log.debug("Center already exists: {} (ID: {})", center.getName(), centerId);
        }
      } catch (Exception ex) {
        // Center not found, proceed with creation
        log.debug("Center not found by external ID, will create new center: {}", ex.getMessage());
      }
    }

    if (existingCenter != null && centerId != null) {
      // Center exists - check for updates
      Map<String, Object> proposedData = buildUpdateRequest(center, context, existingCenter);

      // Detect changes
      Map<String, PlannedChange.FieldChange> changes =
          changeDetectionService.detectChangesForEntityType("center", existingCenter, proposedData);

      if (!changes.isEmpty()) {
        // Update center
        log.info("Updating center: {} ({} fields changed)", center.getName(), changes.size());
        apiClient.put("/api/v1/centers/" + centerId, proposedData, Map.class);
        result.recordEntity("center", ImportResult.EntityAction.UPDATED);
      } else {
        log.debug("Center unchanged: {}", center.getName());
        result.recordEntity("center", ImportResult.EntityAction.UNCHANGED);
      }

      // Activate center if requested and not already active
      if (Boolean.TRUE.equals(center.getActive())
          && center.getActivationDate() != null
          && !isActive(existingCenter)) {
        activateCenter(centerId, center);
      }
    } else {
      // Create new center
      Map<String, Object> request = buildRequest(center, context);
      Map<String, Object> response = apiClient.post("/api/v1/centers", request, Map.class);
      centerId = ((Number) response.get("resourceId")).longValue();

      log.info("Center created: {} (ID: {})", center.getName(), centerId);
      result.recordEntity("center", ImportResult.EntityAction.CREATED);

      // Activate center if requested
      if (Boolean.TRUE.equals(center.getActive()) && center.getActivationDate() != null) {
        activateCenter(centerId, center);
      }
    }

    // Store for reference
    context.registerEntity("center", center.getName(), centerId);
    if (center.getExternalId() != null) {
      context.registerEntity("center", center.getExternalId(), centerId);
    }
  }

  /**
   * Builds API request for center.
   *
   * @param center center
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(Center center, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", center.getName());
    request.put("locale", LOCALE);
    request.put("dateFormat", DATE_FORMAT);

    if (center.getExternalId() != null) {
      request.put("externalId", center.getExternalId());
    }

    // Resolve office
    if (center.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", center.getOfficeName());
      if (officeId != null) {
        request.put("officeId", officeId);
      } else {
        throw new IllegalStateException("Office '" + center.getOfficeName() + "' not found");
      }
    }

    // Resolve staff
    if (center.getStaffName() != null) {
      Long staffId = context.resolveEntityId("staff", center.getStaffName());
      if (staffId != null) {
        request.put("staffId", staffId);
      }
    }

    // Submitted date
    if (center.getSubmittedOnDate() != null) {
      request.put("submittedOnDate", center.getSubmittedOnDate().format(DATE_FORMATTER));
    }

    // Active flag
    request.put("active", Boolean.TRUE.equals(center.getActive()));

    if (Boolean.TRUE.equals(center.getActive()) && center.getActivationDate() != null) {
      request.put("activationDate", center.getActivationDate().format(DATE_FORMATTER));
    }

    return request;
  }

  /**
   * Checks if center is active.
   *
   * @param centerData center data from API
   * @return true if active
   */
  private boolean isActive(Map<String, Object> centerData) {
    Object statusObj = centerData.get("status");
    if (statusObj instanceof Map) {
      Map<String, Object> status = (Map<String, Object>) statusObj;
      String statusValue = (String) status.get("value");
      return "Active".equalsIgnoreCase(statusValue);
    }
    return false;
  }

  /**
   * Builds API request for center update.
   *
   * <p>According to ChangeDetectionService, centers can update staffId and externalId even when
   * active.
   *
   * @param center center
   * @param context import context
   * @param existingCenter existing center data from API
   * @return request map for update
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> buildUpdateRequest(
      Center center, ImportContext context, Map<String, Object> existingCenter) {
    Map<String, Object> request = new HashMap<>();

    // Check if center is active
    boolean isActive = isActive(existingCenter);

    if (isActive) {
      // Center is active - only update allowed fields (staffId, externalId)
      log.debug("Center is active - limiting updateable fields");

      if (center.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", center.getStaffName());
        if (staffId != null) {
          request.put("staffId", staffId);
        }
      }

      if (center.getExternalId() != null) {
        request.put("externalId", center.getExternalId());
      }
    } else {
      // Center not active - can update most fields (excluding immutables)
      // Immutable: id, externalId, accountNo, activationDate
      request.put("name", center.getName());

      // Resolve office (can update before activation)
      if (center.getOfficeName() != null) {
        Long officeId = context.resolveEntityId("office", center.getOfficeName());
        if (officeId != null) {
          request.put("officeId", officeId);
        }
      }

      // Resolve staff
      if (center.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", center.getStaffName());
        if (staffId != null) {
          request.put("staffId", staffId);
        }
      }
    }

    return request;
  }

  /**
   * Activates a center.
   *
   * @param centerId center ID
   * @param center center
   */
  private void activateCenter(Long centerId, Center center) {
    try {
      Map<String, Object> activationRequest = new HashMap<>();
      activationRequest.put("locale", LOCALE);
      activationRequest.put("dateFormat", DATE_FORMAT);
      activationRequest.put("activationDate", center.getActivationDate().format(DATE_FORMATTER));

      apiClient.post(
          "/api/v1/centers/" + centerId + "?command=activate", activationRequest, Map.class);

      log.info("Center activated: {} (ID: {})", center.getName(), centerId);
    } catch (Exception ex) {
      log.warn("Failed to activate center {}: {}", centerId, ex.getMessage());
    }
  }
}
