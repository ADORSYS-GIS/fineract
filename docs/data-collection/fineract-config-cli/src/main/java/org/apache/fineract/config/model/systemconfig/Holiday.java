package org.apache.fineract.config.model.systemconfig;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Holiday configuration.
 *
 * <p>Defines holidays for offices including date ranges and repayment rescheduling rules.
 *
 * <p>API Endpoint: POST /api/v1/holidays
 */
@Slf4j
@Data
public class Holiday {
  /** Holiday name (e.g., "New Year 2024"). */
  private String name;

  /** Optional description of the holiday. */
  private String description;

  /**
   * Start date of the holiday.
   *
   * <p>Supports both string format (e.g., "2024-01-01") and array format [2024, 1, 1]
   */
  @JsonProperty("fromDate")
  private Object fromDate;

  /**
   * End date of the holiday.
   *
   * <p>Supports both string format (e.g., "2024-01-01") and array format [2024, 1, 1]
   */
  @JsonProperty("toDate")
  private Object toDate;

  /**
   * Date to reschedule repayments to (for specific date rescheduling).
   *
   * <p>Used when a specific date is needed instead of a rule.
   */
  @JsonProperty("repaymentsRescheduledTo")
  private Object repaymentsRescheduledTo;

  /**
   * List of office names where this holiday applies.
   *
   * <p>Example: ["Head Office", "Branch 1"]
   */
  @JsonProperty("officeNames")
  private List<String> officeNames;

  /**
   * Repayment rescheduling rule for loans due during holiday.
   *
   * <p>Valid values:
   *
   * <ul>
   *   <li>NEXT_WORKING_DAY - Reschedule to next working day
   *   <li>SAME_DAY - Keep repayment on same day
   *   <li>NEXT_MEETING_DAY - Reschedule to next meeting day
   * </ul>
   *
   * <p>Maps to Fineract enum IDs via FineractEnumMapper
   */
  @JsonProperty("repaymentSchedulingRule")
  private String repaymentSchedulingRule;

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
