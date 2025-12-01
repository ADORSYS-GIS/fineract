package org.apache.fineract.config.model.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Savings Transaction definition.
 *
 * <p>Represents a transaction on a savings account (deposit, withdrawal, etc.).
 */
@Slf4j
@Data
public class SavingsTransaction {
  /** External ID of the savings account (YAML: accountExternalId) */
  private String accountExternalId;

  /** Transaction type: DEPOSIT, WITHDRAWAL */
  private String transactionType;

  /** Transaction date */
  private LocalDate transactionDate;

  /** Transaction amount (YAML: transactionAmount) */
  private BigDecimal transactionAmount;

  /** Payment type name */
  private String paymentTypeName;

  /** Transaction note */
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
