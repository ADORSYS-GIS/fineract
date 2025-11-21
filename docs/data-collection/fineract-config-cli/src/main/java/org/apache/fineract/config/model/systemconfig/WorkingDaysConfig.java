package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Working days calendar configuration. */
@Slf4j
@Data
public class WorkingDaysConfig {
  private String recurrence;

  @JsonProperty("repaymentRescheduleType")
  @JsonAlias("repaymentReschedulingType")
  private String repaymentRescheduleType;

  private boolean extendTermForDailyRepayments;

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
