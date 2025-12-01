package org.apache.fineract.config.model.product;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Tax component configuration.
 *
 * <p>Defines a tax component that can be added to a tax group.
 *
 * <p>API Endpoint: POST /api/v1/taxes/component
 */
@Slf4j
@Data
public class TaxComponent {
  /** Tax component name. */
  private String name;

  /** Tax percentage rate. */
  private BigDecimal percentage;

  /**
   * Start date of the tax component.
   *
   * <p>Supports both string format ("2024-01-01") and array format [2024, 1, 1].
   */
  private Object startDate;

  /** Credit account type (e.g., "ASSET", "LIABILITY"). */
  private String creditAccountType;

  /** Credit account GL code. */
  @JsonProperty("creditGlAccount")
  private String creditGlAccount;

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
