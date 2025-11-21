package org.apache.fineract.config.model.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * Savings Transaction definition.
 *
 * <p>Represents a transaction on a savings account (deposit, withdrawal, etc.).
 */
@Data
public class SavingsTransaction {
  private String savingsAccountExternalId;
  private String transactionType; // DEPOSIT, WITHDRAWAL
  private LocalDate transactionDate;
  private BigDecimal amount;
  private String paymentTypeName;
  private String note;
}
