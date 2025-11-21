package org.apache.fineract.config.model.product;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Floating interest rate configuration.
 *
 * <p>Defines variable interest rate benchmarks with one or more rate periods. Rate periods define
 * the interest rate that applies from a specific date. This allows creating floating rates that
 * change over time, such as a prime lending rate that adjusts quarterly.
 */
@Slf4j
@Data
public class FloatingRate {
  /** The name of the floating rate benchmark (e.g., "Prime Lending Rate"). */
  private String name;

  /** Whether this floating rate is currently active. */
  private Boolean isActive;

  /** Whether this is the base lending rate used as reference for other rates. */
  @JsonProperty("isBaseLendingRate")
  private Boolean isBaseLendingRate;

  /**
   * List of rate periods defining the interest rate for different time periods.
   *
   * <p>Each rate period specifies an effective date (fromDate) and the interest rate that applies
   * from that date onwards. Rate periods must be ordered by date, with the earliest date first.
   */
  private List<RatePeriod> ratePeriods = new ArrayList<>();

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
