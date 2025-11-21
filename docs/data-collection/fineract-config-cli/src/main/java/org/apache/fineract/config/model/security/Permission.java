package org.apache.fineract.config.model.security;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Permission definition.
 *
 * <p>Represents a system permission that can be assigned to roles.
 *
 * <p>Note: Permissions in Fineract are mostly pre-defined. This model is for documentation and
 * validation purposes.
 */
@Slf4j
@Data
public class Permission {
  private String code; // Permission code (e.g., "READ_CLIENT", "CREATE_LOAN")
  private String grouping; // Permission group (e.g., "portfolio", "organisation")
  private String entityName; // Entity type (e.g., "CLIENT", "LOAN")
  private String actionName; // Action (e.g., "READ", "CREATE", "UPDATE", "DELETE")
  private String description;

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    log.warn(
        "Unknown field '{}' with value '{}' in {} (will be ignored). "
            + "This may indicate a missing field in the model class.",
        key,
        value,
        this.getClass().getSimpleName());
  }
}
