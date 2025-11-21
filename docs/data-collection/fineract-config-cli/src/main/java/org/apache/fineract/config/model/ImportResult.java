package org.apache.fineract.config.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Result of configuration import operation.
 *
 * <p>Captures statistics and outcomes of the import process.
 */
@Data
public class ImportResult {

  private ImportStatus status;
  private String message;
  private Instant startTime;
  private Instant endTime;
  private Duration duration;

  /** Entity-level statistics */
  private Map<String, EntityStats> entityStats = new HashMap<>();

  /** Total statistics */
  private int totalEntities = 0;

  private int created = 0;
  private int updated = 0;
  private int unchanged = 0;
  private int failed = 0;

  /** Checksum of imported configuration */
  private String checksum;

  /** Dry-run mode flag */
  private boolean dryRun = false;

  /** Planned changes (dry-run mode only) */
  private List<PlannedChange> plannedChanges = new ArrayList<>();

  public ImportResult() {}

  public ImportResult(boolean dryRun) {
    this.dryRun = dryRun;
  }

  /** Import status enum */
  public enum ImportStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    NO_CHANGES
  }

  /** Statistics per entity type */
  @Data
  public static class EntityStats {
    private String entityType;
    private int total = 0;
    private int created = 0;
    private int updated = 0;
    private int unchanged = 0;
    private int failed = 0;
  }

  /** Creates success result */
  public static ImportResult success() {
    ImportResult result = new ImportResult();
    result.setStatus(ImportStatus.SUCCESS);
    result.setMessage("Import completed successfully");
    return result;
  }

  /** Creates no-changes result */
  public static ImportResult noChanges() {
    ImportResult result = new ImportResult();
    result.setStatus(ImportStatus.NO_CHANGES);
    result.setMessage("No changes detected (checksum match)");
    return result;
  }

  /** Creates failure result */
  public static ImportResult failure(String message) {
    ImportResult result = new ImportResult();
    result.setStatus(ImportStatus.FAILED);
    result.setMessage(message);
    return result;
  }

  /**
   * Records entity operation.
   *
   * @param entityType entity type
   * @param action action performed (CREATED, UPDATED, UNCHANGED, FAILED)
   */
  public void recordEntity(String entityType, EntityAction action) {
    EntityStats stats =
        entityStats.computeIfAbsent(
            entityType,
            k -> {
              EntityStats s = new EntityStats();
              s.setEntityType(k);
              return s;
            });

    stats.setTotal(stats.getTotal() + 1);
    totalEntities++;

    switch (action) {
      case CREATED:
        stats.setCreated(stats.getCreated() + 1);
        created++;
        break;
      case UPDATED:
        stats.setUpdated(stats.getUpdated() + 1);
        updated++;
        break;
      case UNCHANGED:
        stats.setUnchanged(stats.getUnchanged() + 1);
        unchanged++;
        break;
      case FAILED:
        stats.setFailed(stats.getFailed() + 1);
        failed++;
        break;
    }
  }

  /** Entity action enum */
  public enum EntityAction {
    CREATED,
    UPDATED,
    UNCHANGED,
    FAILED
  }

  /** Calculates duration and sets end time */
  public void complete() {
    this.endTime = Instant.now();
    if (this.startTime != null) {
      this.duration = Duration.between(startTime, endTime);
    }
  }

  /**
   * Adds a planned change (dry-run mode).
   *
   * @param change planned change
   */
  public void addPlannedChange(PlannedChange change) {
    this.plannedChanges.add(change);

    // Also record in statistics based on change type
    switch (change.getChangeType()) {
      case CREATE:
        recordEntity(change.getEntityType(), EntityAction.CREATED);
        break;
      case UPDATE:
        recordEntity(change.getEntityType(), EntityAction.UPDATED);
        break;
      case NO_CHANGE:
        recordEntity(change.getEntityType(), EntityAction.UNCHANGED);
        break;
      case WOULD_FAIL:
        recordEntity(change.getEntityType(), EntityAction.FAILED);
        break;
      case DELETE:
        // Deletes are not counted in current statistics
        break;
    }
  }

  /**
   * Gets summary of planned changes by type.
   *
   * @return map of change type to count
   */
  public Map<PlannedChange.ChangeType, Long> getPlannedChangesSummary() {
    Map<PlannedChange.ChangeType, Long> summary = new HashMap<>();
    for (PlannedChange.ChangeType type : PlannedChange.ChangeType.values()) {
      long count = plannedChanges.stream().filter(c -> c.getChangeType() == type).count();
      if (count > 0) {
        summary.put(type, count);
      }
    }
    return summary;
  }

  /**
   * Gets planned changes for a specific entity type.
   *
   * @param entityType entity type
   * @return list of planned changes
   */
  public List<PlannedChange> getPlannedChangesForEntityType(String entityType) {
    return plannedChanges.stream().filter(c -> c.getEntityType().equals(entityType)).toList();
  }

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @com.fasterxml.jackson.annotation.JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    org.slf4j.LoggerFactory.getLogger(this.getClass())
        .warn(
            "Unknown field '{}' with value '{}' in {} (will be ignored). "
                + "This may indicate a missing field in the model class.",
            key,
            value,
            this.getClass().getSimpleName());
  }
}
