package org.apache.fineract.config.model.security;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Cashier assignment to a teller configuration.
 *
 * <p>Represents a cashier (staff member) assigned to work at a specific teller station with start
 * and end dates.
 *
 * <p>API Endpoint: POST /api/v1/tellers/{tellerId}/cashiers
 */
@Slf4j
@Data
public class TellerCashierMapping {
  /** Name of the teller this cashier is assigned to. */
  @JsonProperty("tellerName")
  private String tellerName;

  /** External ID of the staff member serving as cashier. */
  @JsonProperty("staffExternalId")
  private String staffExternalId;

  /** Optional description of this cashier assignment. */
  private String description;

  /**
   * Start date of cashier assignment in [year, month, day] format.
   *
   * <p>Example: [2024, 1, 1] for January 1, 2024
   */
  @JsonProperty("startDate")
  private List<Integer> startDate;

  /**
   * End date of cashier assignment in [year, month, day] format.
   *
   * <p>Example: [2024, 12, 31] for December 31, 2024
   */
  @JsonProperty("endDate")
  private List<Integer> endDate;

  /** Whether this cashier assignment is currently active. Defaults to true. */
  @JsonProperty("isFullDay")
  private Boolean isFullDay;

  /**
   * Start time for part-day assignments (format: "HH:mm").
   *
   * <p>Example: "08:00" for 8 AM
   */
  @JsonProperty("startTime")
  private String startTime;

  /**
   * End time for part-day assignments (format: "HH:mm").
   *
   * <p>Example: "17:00" for 5 PM
   */
  @JsonProperty("endTime")
  private String endTime;

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
