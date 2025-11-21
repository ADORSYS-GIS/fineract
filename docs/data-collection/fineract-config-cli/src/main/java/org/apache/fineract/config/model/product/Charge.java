package org.apache.fineract.config.model.product;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Charge definition.
 *
 * <p>Defines fees and penalties that can be applied to loans and savings.
 */
@Data
public class Charge {
  private String name;
  private String currencyCode;
  private String chargeAppliesTo; // LOAN, SAVINGS, CLIENT, SHARES
  private String chargeTimeType; // DISBURSEMENT, SPECIFIED_DUE_DATE, INSTALMENT_FEE, etc.
  private String chargeCalculationType; // FLAT, PERCENT_OF_AMOUNT, etc.
  private BigDecimal amount;
  private String chargePaymentMode; // REGULAR, ACCOUNT_TRANSFER
  private Boolean active;
  private Boolean penalty;
  private String incomeAccountCode; // GL account for income
  private String feeFrequency; // For recurring charges (e.g., "Monthly", "Quarterly")
  private String feeInterval; // DAYS, WEEKS, MONTHS, YEARS

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @com.fasterxml.jackson.annotation.JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    org.slf4j.LoggerFactory.getLogger(this.getClass())
        .warn(
            "Unknown field '{}' with value '{}' in {} (will be ignored). "
                + "This may indicate a missing field in the model class.",
            key,
            value,
            this.getClass().getSimpleName());
  }
}
