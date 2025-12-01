package org.apache.fineract.config.model.account;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Loan collateral configuration.
 *
 * <p>Represents collateral pledged against a loan account.
 *
 * <p>API Endpoint: POST /api/v1/loans/{loanId}/collaterals
 *
 * <p>YAML Example:
 *
 * <pre>
 * loanCollaterals:
 *   - loanExternalId: LOAN-001
 *     collateralTypeName: Vehicle
 *     value: 500000.00
 *     description: "Toyota Land Cruiser 2020"
 * </pre>
 */
@Slf4j
@Data
public class LoanCollateral {
  /** External ID of the loan account. */
  @JsonProperty("loanExternalId")
  private String loanExternalId;

  /** Name of the collateral type (must match existing collateral type). */
  @JsonProperty("collateralTypeName")
  private String collateralTypeName;

  /** Value of the collateral. */
  @JsonProperty("value")
  private BigDecimal value;

  /** Description of the collateral. */
  @JsonProperty("description")
  private String description;

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
