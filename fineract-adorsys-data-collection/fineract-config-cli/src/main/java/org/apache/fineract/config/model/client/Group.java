package org.apache.fineract.config.model.client;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Group definition.
 *
 * <p>Represents a group of clients (e.g., self-help group, solidarity group).
 */
@Slf4j
@Data
public class Group {
  private String name;
  private String externalId;
  private String officeName;
  private String staffName;
  private Boolean active;
  private LocalDate activationDate;
  private LocalDate submittedOnDate;

  // Group members (client external IDs)
  private List<String> clientExternalIds = new ArrayList<>();

  // Link to center (optional)
  private String centerName;

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
