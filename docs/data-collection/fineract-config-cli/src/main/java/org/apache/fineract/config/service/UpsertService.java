package org.apache.fineract.config.service;

import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for upsert operations (update if exists, create if not).
 *
 * <p>Provides idempotent loading for configuration entities like GL accounts, products, and
 * charges. When an entity with the same unique identifier exists, it will be updated instead of
 * creating a duplicate.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Search by unique field (glCode, shortName, name, externalId, etc.)
 *   <li>Update existing entities with new configuration
 *   <li>Create new entities if not found
 *   <li>Handle different Fineract response formats (list, pageItems, single object)
 *   <li>Comprehensive logging for audit trail
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>
 * UpsertResult result = upsertService.upsert(
 *     "/api/v1/loanproducts",
 *     requestData,
 *     "shortName",
 *     "MSOL",
 *     context,
 *     importResult,
 *     "loanProduct"
 * );
 * </pre>
 */
@Slf4j
@Service
public class UpsertService {

  private final FineractApiClient apiClient;
  private final ChangeDetectionService changeDetectionService;

  public UpsertService(FineractApiClient apiClient, ChangeDetectionService changeDetectionService) {
    this.apiClient = apiClient;
    this.changeDetectionService = changeDetectionService;
  }

  /**
   * Upsert operation result.
   *
   * <p>Contains information about whether an entity was created or updated, and its ID.
   */
  public static class UpsertResult {
    private final Long entityId;
    private final boolean wasCreated;
    private final boolean wasUpdated;

    public UpsertResult(Long entityId, boolean wasCreated, boolean wasUpdated) {
      this.entityId = entityId;
      this.wasCreated = wasCreated;
      this.wasUpdated = wasUpdated;
    }

    public Long getEntityId() {
      return entityId;
    }

    public boolean wasCreated() {
      return wasCreated;
    }

    public boolean wasUpdated() {
      return wasUpdated;
    }

    public boolean wasUnchanged() {
      return !wasCreated && !wasUpdated;
    }
  }

  /**
   * Upserts an entity (update if exists, create if not).
   *
   * @param endpoint API endpoint (e.g., "/api/v1/loanproducts")
   * @param requestData request payload for create/update
   * @param searchField field to search by (e.g., "shortName", "glCode", "name", "externalId")
   * @param searchValue value to search for
   * @param context import context for entity registration
   * @param result import result for tracking
   * @param entityType entity type name for logging (e.g., "loanProduct", "glAccount")
   * @return UpsertResult containing entity ID and action taken
   */
  public UpsertResult upsert(
      String endpoint,
      Map<String, Object> requestData,
      String searchField,
      String searchValue,
      ImportContext context,
      ImportResult result,
      String entityType) {

    log.debug("Upsert {} by {}={}", entityType, searchField, searchValue);

    try {
      // Search for existing entity
      Map<String, Object> existing = findExistingEntity(endpoint, searchField, searchValue);

      if (existing != null) {
        // Entity exists - check if update needed
        Long entityId = extractEntityId(existing);
        log.debug("{} found with ID: {}", entityType, entityId);

        // Check if entity has changed
        if (hasChanges(existing, requestData, entityType)) {
          // Update existing entity
          updateEntity(endpoint, entityId, requestData, entityType);
          result.recordEntity(entityType, ImportResult.EntityAction.UPDATED);

          return new UpsertResult(entityId, false, true);
        } else {
          // No changes needed
          log.debug("{} {} unchanged", entityType, searchValue);
          result.recordEntity(entityType, ImportResult.EntityAction.UNCHANGED);

          return new UpsertResult(entityId, false, false);
        }

      } else {
        // Entity doesn't exist - create new
        Long entityId = createEntity(endpoint, requestData, entityType);
        result.recordEntity(entityType, ImportResult.EntityAction.CREATED);

        return new UpsertResult(entityId, true, false);
      }

    } catch (Exception ex) {
      log.error(
          "Upsert failed for {} {}={}: {}", entityType, searchField, searchValue, ex.getMessage());
      result.recordEntity(entityType, ImportResult.EntityAction.FAILED);
      throw new RuntimeException(
          String.format("Upsert failed for %s %s=%s", entityType, searchField, searchValue), ex);
    }
  }

  /**
   * Finds an existing entity by searching with a specific field.
   *
   * @param endpoint API endpoint
   * @param searchField field to search by
   * @param searchValue value to search for
   * @return existing entity map, or null if not found
   */
  private Map<String, Object> findExistingEntity(
      String endpoint, String searchField, String searchValue) {

    try {
      // Fetch all entities (Fineract doesn't support field-specific search on all endpoints)
      Object response = apiClient.get(endpoint, Object.class);

      List<Map<String, Object>> entities = extractEntityList(response);

      if (entities == null || entities.isEmpty()) {
        return null;
      }

      // Search for matching entity
      for (Map<String, Object> entity : entities) {
        Object fieldValue = entity.get(searchField);
        if (fieldValue != null && fieldValue.toString().equals(searchValue)) {
          return entity;
        }
      }

      return null;

    } catch (Exception ex) {
      log.debug("Search failed for {}={}: {}", searchField, searchValue, ex.getMessage());
      return null;
    }
  }

  /**
   * Extracts entity list from various Fineract response formats.
   *
   * @param response API response
   * @return list of entities
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> extractEntityList(Object response) {
    if (response instanceof List) {
      // Response is already a list
      return (List<Map<String, Object>>) response;
    } else if (response instanceof Map) {
      Map<String, Object> responseMap = (Map<String, Object>) response;

      // Check for pageItems (paginated response)
      if (responseMap.containsKey("pageItems")) {
        return (List<Map<String, Object>>) responseMap.get("pageItems");
      }

      // Check if response is a single object with 'id' field
      if (responseMap.containsKey("id")) {
        return List.of(responseMap);
      }
    }

    return List.of();
  }

  /**
   * Extracts entity ID from response map.
   *
   * @param entity entity map
   * @return entity ID
   */
  private Long extractEntityId(Map<String, Object> entity) {
    Object idObj = entity.get("id");
    if (idObj instanceof Number) {
      return ((Number) idObj).longValue();
    }
    throw new IllegalStateException("Entity ID not found or invalid format");
  }

  /**
   * Checks if request data has changes compared to existing entity.
   *
   * <p>Uses ChangeDetectionService for intelligent field-by-field comparison with entity-specific
   * immutable field configuration.
   *
   * @param existing existing entity
   * @param requestData new request data
   * @param entityType entity type for entity-specific configuration
   * @return true if changes detected
   */
  private boolean hasChanges(
      Map<String, Object> existing, Map<String, Object> requestData, String entityType) {

    Map<String, PlannedChange.FieldChange> changes =
        changeDetectionService.detectChangesForEntityType(entityType, existing, requestData);

    if (!changes.isEmpty()) {
      log.debug("Detected {} changes for {}", changes.size(), entityType);
      changes
          .values()
          .forEach(
              fc ->
                  log.debug(
                      "  {} changed: {} → {}",
                      fc.getFieldName(),
                      fc.getOldValue(),
                      fc.getNewValue()));
    }

    return !changes.isEmpty();
  }

  /**
   * Updates an existing entity.
   *
   * @param endpoint API endpoint
   * @param entityId entity ID
   * @param requestData request data
   * @param entityType entity type for logging
   */
  private void updateEntity(
      String endpoint, Long entityId, Map<String, Object> requestData, String entityType) {

    try {
      String updateEndpoint = endpoint + "/" + entityId;
      Map<String, Object> response = apiClient.put(updateEndpoint, requestData, Map.class);

      log.info("→ Updated {}: {} (ID: {})", entityType, requestData.get("name"), entityId);
      log.debug("Update response: {}", response);

    } catch (Exception ex) {
      log.error("Failed to update {} ID {}: {}", entityType, entityId, ex.getMessage());
      throw ex;
    }
  }

  /**
   * Creates a new entity.
   *
   * @param endpoint API endpoint
   * @param requestData request data
   * @param entityType entity type for logging
   * @return created entity ID
   */
  private Long createEntity(String endpoint, Map<String, Object> requestData, String entityType) {

    try {
      Map<String, Object> response = apiClient.post(endpoint, requestData, Map.class);

      // Extract entity ID from response
      Long entityId = extractCreatedEntityId(response);

      log.info("→ Created {}: {} (ID: {})", entityType, requestData.get("name"), entityId);
      log.debug("Create response: {}", response);

      return entityId;

    } catch (Exception ex) {
      log.error("Failed to create {}: {}", entityType, ex.getMessage());
      throw ex;
    }
  }

  /**
   * Extracts entity ID from create response.
   *
   * <p>Fineract returns different response formats:
   *
   * <ul>
   *   <li>resourceId (most common)
   *   <li>loanProductId, savingsProductId, etc. (specific entity IDs)
   *   <li>changes.id (for some endpoints)
   * </ul>
   *
   * @param response API response
   * @return entity ID
   */
  @SuppressWarnings("unchecked")
  private Long extractCreatedEntityId(Map<String, Object> response) {
    // Check for resourceId (most common)
    if (response.containsKey("resourceId")) {
      return ((Number) response.get("resourceId")).longValue();
    }

    // Check for specific entity IDs
    String[] idFields = {
      "loanProductId",
      "savingsProductId",
      "chargeId",
      "glAccountId",
      "officeId",
      "staffId",
      "clientId",
      "groupId",
      "savingsAccountId",
      "loanId"
    };

    for (String idField : idFields) {
      if (response.containsKey(idField)) {
        return ((Number) response.get(idField)).longValue();
      }
    }

    // Check for changes.id
    if (response.containsKey("changes")) {
      Map<String, Object> changes = (Map<String, Object>) response.get("changes");
      if (changes.containsKey("id")) {
        return ((Number) changes.get("id")).longValue();
      }
    }

    throw new IllegalStateException("Could not extract entity ID from response: " + response);
  }
}
