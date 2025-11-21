package org.apache.fineract.config.model;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a planned change in dry-run mode.
 *
 * <p>Captures what would happen during a real import without actually making changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedChange {

  /** Entity type (e.g., "office", "client", "loanProduct") */
  private String entityType;

  /** Entity identifier (name, externalId, etc.) */
  private String identifier;

  /** Type of change planned */
  private ChangeType changeType;

  /** Current data (for updates) */
  private Map<String, Object> currentData;

  /** Proposed data from YAML */
  private Map<String, Object> proposedData;

  /** Changed fields (for updates) */
  private Map<String, FieldChange> changedFields;

  /** Reason if change is blocked or would fail */
  private String reason;

  /** Change type enum */
  public enum ChangeType {
    /** Entity will be created */
    CREATE,

    /** Entity will be updated */
    UPDATE,

    /** Entity exists but no changes needed */
    NO_CHANGE,

    /** Entity would be deleted (managed resource mode: full) */
    DELETE,

    /** Change would fail (validation error, dependency missing, etc.) */
    WOULD_FAIL
  }

  /** Represents a single field change */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FieldChange {
    private String fieldName;
    private Object oldValue;
    private Object newValue;

    @Override
    public String toString() {
      return String.format("%s: %s → %s", fieldName, oldValue, newValue);
    }
  }

  /**
   * Creates a planned CREATE change.
   *
   * @param entityType entity type
   * @param identifier identifier
   * @param proposedData proposed data
   * @return planned change
   */
  public static PlannedChange createChange(
      String entityType, String identifier, Map<String, Object> proposedData) {
    return PlannedChange.builder()
        .entityType(entityType)
        .identifier(identifier)
        .changeType(ChangeType.CREATE)
        .proposedData(proposedData)
        .changedFields(new LinkedHashMap<>())
        .build();
  }

  /**
   * Creates a planned UPDATE change.
   *
   * @param entityType entity type
   * @param identifier identifier
   * @param currentData current data
   * @param proposedData proposed data
   * @param changedFields changed fields
   * @return planned change
   */
  public static PlannedChange updateChange(
      String entityType,
      String identifier,
      Map<String, Object> currentData,
      Map<String, Object> proposedData,
      Map<String, FieldChange> changedFields) {
    return PlannedChange.builder()
        .entityType(entityType)
        .identifier(identifier)
        .changeType(ChangeType.UPDATE)
        .currentData(currentData)
        .proposedData(proposedData)
        .changedFields(changedFields)
        .build();
  }

  /**
   * Creates a NO_CHANGE entry.
   *
   * @param entityType entity type
   * @param identifier identifier
   * @return planned change
   */
  public static PlannedChange noChange(String entityType, String identifier) {
    return PlannedChange.builder()
        .entityType(entityType)
        .identifier(identifier)
        .changeType(ChangeType.NO_CHANGE)
        .changedFields(new LinkedHashMap<>())
        .build();
  }

  /**
   * Creates a WOULD_FAIL change.
   *
   * @param entityType entity type
   * @param identifier identifier
   * @param reason failure reason
   * @return planned change
   */
  public static PlannedChange wouldFail(String entityType, String identifier, String reason) {
    return PlannedChange.builder()
        .entityType(entityType)
        .identifier(identifier)
        .changeType(ChangeType.WOULD_FAIL)
        .reason(reason)
        .changedFields(new LinkedHashMap<>())
        .build();
  }

  /**
   * Returns a summary string for logging.
   *
   * @return summary
   */
  public String getSummary() {
    switch (changeType) {
      case CREATE:
        return String.format("[CREATE] %s: %s", entityType, identifier);
      case UPDATE:
        return String.format(
            "[UPDATE] %s: %s (%d fields changed)", entityType, identifier, changedFields.size());
      case NO_CHANGE:
        return String.format("[NO CHANGE] %s: %s", entityType, identifier);
      case DELETE:
        return String.format("[DELETE] %s: %s", entityType, identifier);
      case WOULD_FAIL:
        return String.format("[WOULD FAIL] %s: %s - %s", entityType, identifier, reason);
      default:
        return String.format("[UNKNOWN] %s: %s", entityType, identifier);
    }
  }

  /**
   * Returns detailed change information for display.
   *
   * @return detailed info
   */
  public String getDetailedInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append(getSummary()).append("\n");

    if (changeType == ChangeType.UPDATE && changedFields != null && !changedFields.isEmpty()) {
      sb.append("  Changed fields:\n");
      changedFields.values().forEach(fc -> sb.append("    - ").append(fc).append("\n"));
    }

    if (changeType == ChangeType.CREATE && proposedData != null) {
      sb.append("  Proposed data:\n");
      proposedData.forEach(
          (key, value) -> sb.append("    - ").append(key).append(": ").append(value).append("\n"));
    }

    if (reason != null) {
      sb.append("  Reason: ").append(reason).append("\n");
    }

    return sb.toString();
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
