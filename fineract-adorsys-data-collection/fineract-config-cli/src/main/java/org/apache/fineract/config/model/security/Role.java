package org.apache.fineract.config.model.security;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Role with permissions.
 *
 * <p>Defines a user role with associated permissions.
 */
@Slf4j
@Data
public class Role {
  private String name;
  private String description;
  private List<String> permissions = new ArrayList<>(); // Permission names
  private Boolean disabled;

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
