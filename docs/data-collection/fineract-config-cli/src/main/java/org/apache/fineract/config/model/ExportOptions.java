package org.apache.fineract.config.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * Options for exporting Fineract configuration.
 *
 * <p>Provides filtering and customization options for export operations.
 */
@Data
@Builder
public class ExportOptions {

  /** Phases to export (1-6), empty set means all phases */
  @Builder.Default private Set<Integer> phases = new HashSet<>();

  /** Entity types to include (empty set means all types) */
  @Builder.Default private Set<String> entityTypes = new HashSet<>();

  /** Entity types to exclude (takes precedence over entityTypes) */
  @Builder.Default private Set<String> excludeEntityTypes = new HashSet<>();

  /** Only export active entities (where applicable) */
  @Builder.Default private boolean activeOnly = false;

  /** Include sensitive data (passwords, etc.) - default false for security */
  @Builder.Default private boolean includeSensitiveData = false;

  /** Include system-defined entities (codes, roles, etc.) */
  @Builder.Default private boolean includeSystemDefined = false;

  /** Add detailed comments to exported YAML */
  @Builder.Default private boolean includeComments = true;

  /** Pretty print YAML with proper indentation */
  @Builder.Default private boolean prettyPrint = true;

  /** Export format version (for future compatibility) */
  @Builder.Default private String formatVersion = "1.0";

  /**
   * Checks if a specific phase should be exported.
   *
   * @param phase phase number (1-6)
   * @return true if phase should be exported
   */
  public boolean shouldExportPhase(int phase) {
    return phases.isEmpty() || phases.contains(phase);
  }

  /**
   * Checks if a specific entity type should be exported.
   *
   * @param entityType entity type name
   * @return true if entity type should be exported
   */
  public boolean shouldExportEntityType(String entityType) {
    if (excludeEntityTypes.contains(entityType)) {
      return false;
    }
    return entityTypes.isEmpty() || entityTypes.contains(entityType);
  }

  /**
   * Creates default export options (all phases, all entities, sanitized).
   *
   * @return default export options
   */
  public static ExportOptions defaultOptions() {
    return ExportOptions.builder().build();
  }

  /**
   * Creates export options for a specific phase only.
   *
   * @param phase phase number (1-6)
   * @return export options for single phase
   */
  public static ExportOptions forPhase(int phase) {
    Set<Integer> phases = new HashSet<>();
    phases.add(phase);
    return ExportOptions.builder().phases(phases).build();
  }

  /**
   * Creates export options for configuration only (no operational data).
   *
   * @return export options for configuration only
   */
  public static ExportOptions configurationOnly() {
    Set<String> exclude = new HashSet<>();
    exclude.add("savingsTransactions");
    exclude.add("loanTransactions");
    exclude.add("journalEntries");
    return ExportOptions.builder().excludeEntityTypes(exclude).build();
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
