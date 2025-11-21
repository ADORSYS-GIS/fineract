package org.apache.fineract.config.model.product;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Delinquency range configuration.
 *
 * <p>Defines an age range (in days) for classifying loans by delinquency status. Each delinquency
 * range has a classification name and specifies the minimum and maximum number of days overdue that
 * fall into this range. For example, a range might classify loans between 1-30 days overdue as
 * "Early Delinquency" and loans 31-60 days overdue as "Established Delinquency".
 */
@Slf4j
@Data
public class DelinquencyRange {
  /**
   * Classification name for this delinquency range (e.g., "0-30 Days Overdue", "Early
   * Delinquency").
   */
  private String classification;

  /**
   * Minimum number of days a loan is overdue to be classified into this range (inclusive).
   *
   * <p>For example, if minAgeDays is 1 and maxAgeDays is 30, loans that are 1-30 days overdue fall
   * into this range.
   */
  private Integer minAgeDays;

  /**
   * Maximum number of days a loan is overdue to remain in this range (inclusive).
   *
   * <p>For example, if minAgeDays is 31 and maxAgeDays is 60, loans that are 31-60 days overdue
   * fall into this range. If this is null, it means "maxAgeDays and above" (unbounded upper limit).
   */
  private Integer maxAgeDays;

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
