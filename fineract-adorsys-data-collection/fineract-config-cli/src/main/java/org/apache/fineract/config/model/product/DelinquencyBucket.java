package org.apache.fineract.config.model.product;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Delinquency bucket configuration.
 *
 * <p>A delinquency bucket groups delinquency ranges to classify loans by how many days overdue they
 * are. For example, a bucket might contain ranges for: 0-30 days, 31-60 days, 61-90 days, and 90+
 * days overdue. This classification helps MFIs track and manage loan portfolio health and apply
 * appropriate remedial actions.
 */
@Slf4j
@Data
public class DelinquencyBucket {
  /**
   * The name of the delinquency bucket (e.g., "Default Delinquency Bucket", "Accelerated
   * Remediation Bucket").
   */
  private String name;

  /**
   * List of delinquency ranges that define the age brackets for this bucket.
   *
   * <p>Each range specifies a classification and the minimum/maximum number of days overdue that
   * fall into that classification. Ranges should be ordered by ascending minAgeDays and should not
   * overlap. For example:
   *
   * <pre>
   * ranges:
   *   - classification: "0-30 Days"
   *     minAgeDays: 0
   *     maxAgeDays: 30
   *   - classification: "31-60 Days"
   *     minAgeDays: 31
   *     maxAgeDays: 60
   *   - classification: "60+ Days"
   *     minAgeDays: 61
   *     maxAgeDays: null  # null means unbounded upper limit
   * </pre>
   */
  private List<DelinquencyRange> ranges = new ArrayList<>();

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
