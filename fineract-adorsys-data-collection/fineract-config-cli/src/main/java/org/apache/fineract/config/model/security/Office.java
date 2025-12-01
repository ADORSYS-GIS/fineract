package org.apache.fineract.config.model.security;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Office (branch/location).
 *
 * <p>Represents an organizational unit such as head office, branch, or regional office.
 */
@Slf4j
@Data
public class Office {
  private String name;
  private String externalId;
  private LocalDate openingDate;
  private String parentName; // Reference to parent office
  private String hierarchy; // Optional: explicit hierarchy path

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
