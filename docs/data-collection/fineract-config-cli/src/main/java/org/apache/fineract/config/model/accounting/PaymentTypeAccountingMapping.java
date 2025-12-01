package org.apache.fineract.config.model.accounting;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment type accounting mapping configuration.
 *
 * <p>Maps payment types to GL accounts for fund source and fee income.
 *
 * <p>API Endpoint: PUT /api/v1/paymenttypes/{paymentTypeId}
 *
 * <p>YAML Example:
 *
 * <pre>
 * paymentTypeAccountingMappings:
 *   - paymentTypeName: Cash
 *     fundSourceGlCode: "1001"
 *     feeIncomeGlCode: "4001"
 * </pre>
 */
@Slf4j
@Data
public class PaymentTypeAccountingMapping {
  /** Name of the payment type to configure. */
  @JsonProperty("paymentTypeName")
  private String paymentTypeName;

  /**
   * GL account code for fund source (asset account).
   *
   * <p>This account is debited/credited when payments are made/received.
   */
  @JsonProperty("fundSourceGlCode")
  private String fundSourceGlCode;

  /**
   * GL account code for fee income (income account).
   *
   * <p>This account receives fee income from this payment type.
   */
  @JsonProperty("feeIncomeGlCode")
  private String feeIncomeGlCode;

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
