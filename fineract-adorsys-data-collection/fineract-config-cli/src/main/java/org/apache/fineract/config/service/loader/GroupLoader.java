package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.model.client.Group;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.ChangeDetectionService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for groups.
 *
 * <p>Creates group records (collections of clients).
 *
 * <p>API Endpoint: POST /api/v1/groups
 */
@Slf4j
@Component
public class GroupLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");
  private static final String LOCALE = "en";
  private static final String DATE_FORMAT = "dd MMMM yyyy";

  private final FineractApiClient apiClient;
  private final ChangeDetectionService changeDetectionService;

  public GroupLoader(FineractApiClient apiClient, ChangeDetectionService changeDetectionService) {
    this.apiClient = apiClient;
    this.changeDetectionService = changeDetectionService;
  }

  /**
   * Loads groups.
   *
   * @param groups list of groups
   * @param context import context
   * @param result import result
   */
  public void load(List<Group> groups, ImportContext context, ImportResult result) {
    log.debug("Loading {} groups", groups.size());

    for (Group group : groups) {
      try {
        loadSingleGroup(group, context, result);
      } catch (Exception ex) {
        log.error("Failed to load group '{}': {}", group.getName(), ex.getMessage());
        result.recordEntity("group", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single group.
   *
   * @param group group to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleGroup(Group group, ImportContext context, ImportResult result) {
    log.debug("Loading group: {}", group.getName());

    Long groupId = null;
    Map<String, Object> existingGroup = null;

    // Check if group already exists by external ID
    if (group.getExternalId() != null) {
      try {
        // Groups search endpoint returns an array directly (not a paged response)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> searchResults =
            apiClient.get("/api/v1/groups?externalId=" + group.getExternalId(), List.class);

        if (searchResults != null && !searchResults.isEmpty()) {
          existingGroup = searchResults.get(0);
          groupId = ((Number) existingGroup.get("id")).longValue();
          log.debug("Group already exists: {} (ID: {})", group.getName(), groupId);
        }
      } catch (Exception ex) {
        // Group not found, proceed with creation
        log.debug("Group not found by external ID, will create new group: {}", ex.getMessage());
      }
    }

    if (existingGroup != null && groupId != null) {
      // Group exists - check for updates
      Map<String, Object> proposedData = buildUpdateRequest(group, context, existingGroup);

      // Detect changes
      Map<String, PlannedChange.FieldChange> changes =
          changeDetectionService.detectChangesForEntityType("group", existingGroup, proposedData);

      if (!changes.isEmpty()) {
        // Update group
        log.info("Updating group: {} ({} fields changed)", group.getName(), changes.size());
        apiClient.put("/api/v1/groups/" + groupId, proposedData, Map.class);
        result.recordEntity("group", ImportResult.EntityAction.UPDATED);
      } else {
        log.debug("Group unchanged: {}", group.getName());
        result.recordEntity("group", ImportResult.EntityAction.UNCHANGED);
      }

      // Activate group if requested and not already active
      if (Boolean.TRUE.equals(group.getActive())
          && group.getActivationDate() != null
          && !isActive(existingGroup)) {
        activateGroup(groupId, group);
      }
    } else {
      // Create new group
      Map<String, Object> request = buildRequest(group, context);
      Map<String, Object> response = apiClient.post("/api/v1/groups", request, Map.class);
      groupId = ((Number) response.get("groupId")).longValue();

      log.info("Group created: {} (ID: {})", group.getName(), groupId);
      result.recordEntity("group", ImportResult.EntityAction.CREATED);

      // Activate group if requested
      if (Boolean.TRUE.equals(group.getActive()) && group.getActivationDate() != null) {
        activateGroup(groupId, group);
      }
    }

    // Store for reference
    context.registerEntity("group", group.getName(), groupId);
    if (group.getExternalId() != null) {
      context.registerEntity("group", group.getExternalId(), groupId);
    }
  }

  /**
   * Builds API request for group.
   *
   * @param group group
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(Group group, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", group.getName());
    request.put("locale", LOCALE);
    request.put("dateFormat", DATE_FORMAT);

    if (group.getExternalId() != null) {
      request.put("externalId", group.getExternalId());
    }

    // Resolve office
    if (group.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", group.getOfficeName());
      if (officeId != null) {
        request.put("officeId", officeId);
      } else {
        throw new IllegalStateException("Office '" + group.getOfficeName() + "' not found");
      }
    }

    // Resolve staff
    if (group.getStaffName() != null) {
      Long staffId = context.resolveEntityId("staff", group.getStaffName());
      if (staffId != null) {
        request.put("staffId", staffId);
      }
    }

    // Resolve center (optional)
    if (group.getCenterName() != null) {
      Long centerId = context.resolveEntityId("center", group.getCenterName());
      if (centerId != null) {
        request.put("centerId", centerId);
      }
    }

    // Resolve client members
    if (group.getClientExternalIds() != null && !group.getClientExternalIds().isEmpty()) {
      List<Long> clientMemberIds = new ArrayList<>();
      for (String clientExternalId : group.getClientExternalIds()) {
        Long clientId = context.resolveEntityId("client", clientExternalId);
        if (clientId != null) {
          clientMemberIds.add(clientId);
        } else {
          log.warn(
              "Client '{}' not found for group '{}', skipping", clientExternalId, group.getName());
        }
      }
      if (!clientMemberIds.isEmpty()) {
        request.put("clientMembers", clientMemberIds);
      }
    }

    // Submitted date
    if (group.getSubmittedOnDate() != null) {
      request.put("submittedOnDate", group.getSubmittedOnDate().format(DATE_FORMATTER));
    }

    // Active flag
    request.put("active", Boolean.TRUE.equals(group.getActive()));

    if (Boolean.TRUE.equals(group.getActive()) && group.getActivationDate() != null) {
      request.put("activationDate", group.getActivationDate().format(DATE_FORMATTER));
    }

    return request;
  }

  /**
   * Checks if group is active.
   *
   * @param groupData group data from API
   * @return true if active
   */
  private boolean isActive(Map<String, Object> groupData) {
    Object statusObj = groupData.get("status");
    if (statusObj instanceof Map) {
      Map<String, Object> status = (Map<String, Object>) statusObj;
      String statusValue = (String) status.get("value");
      return "Active".equalsIgnoreCase(statusValue);
    }
    return false;
  }

  /**
   * Builds API request for group update.
   *
   * <p>According to ChangeDetectionService, groups can update staffId and externalId even when
   * active.
   *
   * @param group group
   * @param context import context
   * @param existingGroup existing group data from API
   * @return request map for update
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> buildUpdateRequest(
      Group group, ImportContext context, Map<String, Object> existingGroup) {
    Map<String, Object> request = new HashMap<>();

    // Check if group is active
    boolean isActive = isActive(existingGroup);

    if (isActive) {
      // Group is active - only update allowed fields (staffId, externalId)
      log.debug("Group is active - limiting updateable fields");

      if (group.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", group.getStaffName());
        if (staffId != null) {
          request.put("staffId", staffId);
        }
      }

      if (group.getExternalId() != null) {
        request.put("externalId", group.getExternalId());
      }
    } else {
      // Group not active - can update most fields (excluding immutables)
      // Immutable: id, externalId, accountNo, activationDate
      request.put("name", group.getName());

      // Resolve office (can update before activation)
      if (group.getOfficeName() != null) {
        Long officeId = context.resolveEntityId("office", group.getOfficeName());
        if (officeId != null) {
          request.put("officeId", officeId);
        }
      }

      // Resolve staff
      if (group.getStaffName() != null) {
        Long staffId = context.resolveEntityId("staff", group.getStaffName());
        if (staffId != null) {
          request.put("staffId", staffId);
        }
      }
    }

    return request;
  }

  /**
   * Activates a group.
   *
   * @param groupId group ID
   * @param group group
   */
  private void activateGroup(Long groupId, Group group) {
    try {
      Map<String, Object> activationRequest = new HashMap<>();
      activationRequest.put("locale", LOCALE);
      activationRequest.put("dateFormat", DATE_FORMAT);
      activationRequest.put("activationDate", group.getActivationDate().format(DATE_FORMATTER));

      apiClient.post(
          "/api/v1/groups/" + groupId + "?command=activate", activationRequest, Map.class);

      log.info("Group activated: {} (ID: {})", group.getName(), groupId);
    } catch (Exception ex) {
      log.warn("Failed to activate group {}: {}", groupId, ex.getMessage());
    }
  }
}
