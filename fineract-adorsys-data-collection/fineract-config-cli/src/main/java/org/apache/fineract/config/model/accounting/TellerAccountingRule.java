package org.apache.fineract.config.model.accounting;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Teller accounting rule configuration.
 *
 * <p>Maps teller cash transactions to GL accounts.
 *
 * <p>API Endpoint: POST /api/v1/accountingrules (with teller-specific mappings)
 *
 * <p>YAML Example:
 *
 * <pre>
 * tellerAccountingRules:
 *   - tellerName: Main Teller
 *     cashInGlCode: "1001"
 *     cashOutGlCode: "1002"
 *     description: "Accounting rule for Main Teller"
 * </pre>
 */
@Slf4j
@Data
public class TellerAccountingRule {
  /** Name of the teller. */
  @JsonProperty("tellerName")
  private String tellerName;

  /** GL account code for cash in transactions. */
  @JsonProperty("cashInGlCode")
  private String cashInGlCode;

  /** GL account code for cash out transactions. */
  @JsonProperty("cashOutGlCode")
  private String cashOutGlCode;

  /** Description of the accounting rule. */
  @JsonProperty("description")
  private String description;

  /** Office name for the rule scope. */
  @JsonProperty("officeName")
  private String officeName;

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
