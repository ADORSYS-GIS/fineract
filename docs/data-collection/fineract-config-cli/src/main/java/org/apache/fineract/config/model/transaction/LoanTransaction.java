package org.apache.fineract.config.model.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Loan Transaction definition.
 *
 * <p>Represents a transaction on a loan account (repayment, waiver, etc.).
 */
@Slf4j
@Data
public class LoanTransaction {
  private String loanAccountExternalId;
  private String transactionType; // REPAYMENT, WAIVER, WRITEOFF
  private LocalDate transactionDate;
  private BigDecimal amount;
  private String paymentTypeName;
  private String note;

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
