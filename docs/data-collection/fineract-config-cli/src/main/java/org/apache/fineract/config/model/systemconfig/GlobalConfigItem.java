package org.apache.fineract.config.model.systemconfig;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Global configuration item. */
@Slf4j
@Data
public class GlobalConfigItem {
  private String name;
  private boolean enabled;
  private Long value;
  private String stringValue;
  private LocalDate dateValue;

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
