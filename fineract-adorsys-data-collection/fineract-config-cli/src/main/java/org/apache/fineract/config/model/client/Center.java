package org.apache.fineract.config.model.client;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Center definition.
 *
 * <p>Represents a center (collection of groups) in the system.
 */
@Slf4j
@Data
public class Center {
  private String name;
  private String externalId;
  private String officeName;
  private String staffName;
  private Boolean active;
  private LocalDate activationDate;
  private LocalDate submittedOnDate;

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
