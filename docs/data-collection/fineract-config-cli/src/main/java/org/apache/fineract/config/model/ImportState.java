package org.apache.fineract.config.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Data;

/**
 * Import state tracking.
 *
 * <p>Tracks the state of previous imports including checksums and managed resources.
 *
 * <p>Stored in Fineract as a data table entry for idempotent imports.
 */
@Data
public class ImportState {

  private String checksum;
  private Instant lastImportTime;
  private String lastImportStatus;
  private Map<String, Set<String>> managedResources = new HashMap<>();
  private Map<String, String> metadata = new HashMap<>();

  /** Optimistic locking version number for detecting concurrent modifications */
  private Long version = 1L;

  /** Unique identifier for this import instance (UUID) */
  private String importInstanceId;

  /** Timestamp when state was first created */
  private Instant stateCreatedAt;

  /** Timestamp when state was last updated */
  private Instant stateUpdatedAt;

  /**
   * Checks if a resource is managed by this configuration.
   *
   * @param entityType entity type
   * @param identifier resource identifier
   * @return true if managed
   */
  public boolean isManaged(String entityType, String identifier) {
    Set<String> resources = managedResources.get(entityType);
    return resources != null && resources.contains(identifier);
  }

  /**
   * Registers a managed resource.
   *
   * @param entityType entity type
   * @param identifier resource identifier
   */
  public void registerResource(String entityType, String identifier) {
    managedResources.computeIfAbsent(entityType, k -> new HashSet<>()).add(identifier);
  }

  /**
   * Removes a managed resource.
   *
   * @param entityType entity type
   * @param identifier resource identifier
   */
  public void unregisterResource(String entityType, String identifier) {
    Set<String> resources = managedResources.get(entityType);
    if (resources != null) {
      resources.remove(identifier);
    }
  }

  /**
   * Gets all managed resources for an entity type.
   *
   * @param entityType entity type
   * @return set of identifiers
   */
  public Set<String> getManagedResourcesForType(String entityType) {
    return managedResources.getOrDefault(entityType, new HashSet<>());
  }
}
