package org.apache.fineract.config.model.accounting;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Financial Activity to GL Account Mapping.
 *
 * <p>Maps financial activities to specific GL accounts for automatic posting.
 */
@Slf4j
@Data
public class FinancialActivityMapping {
  @JsonAlias("financialActivityName")
  private String financialActivity; // Activity type (e.g., ASSET_TRANSFER, LIABILITY_TRANSFER)

  private String glAccountCode; // GL account code to map to

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
