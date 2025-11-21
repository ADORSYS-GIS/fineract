package org.apache.fineract.config.model.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Rate period for floating interest rates. */
@Slf4j
@Data
public class RatePeriod {
  /** The effective date from which this interest rate applies. */
  private LocalDate fromDate;

  /** The interest rate percentage for this period. */
  private BigDecimal interestRate;

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
