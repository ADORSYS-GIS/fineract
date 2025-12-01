package org.apache.fineract.config.model.product;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Loan provisioning criteria category configuration. */
@Slf4j
@Data
public class LoanProvisioning {
  /** Category name (e.g., STANDARD, SUB-STANDARD, DOUBTFUL, LOSS). */
  private String categoryName;

  /** Provisioning percentage to set aside (0-100). */
  private BigDecimal provisioningPercentage;

  /** GL account ID for provisioning liability. */
  private Integer liabilityAccountId;

  /** GL account ID for provisioning expense. */
  private Integer expenseAccountId;

  /** Minimum days overdue for this provisioning category. */
  @JsonProperty("minDaysOverdue")
  private Integer minDaysOverdue;

  /** Maximum days overdue for this provisioning category. */
  @JsonProperty("maxDaysOverdue")
  private Integer maxDaysOverdue;

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
