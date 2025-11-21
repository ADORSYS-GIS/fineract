package org.apache.fineract.config.model.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * Loan Transaction definition.
 *
 * <p>Represents a transaction on a loan account (repayment, waiver, etc.).
 */
@Data
public class LoanTransaction {
  private String loanAccountExternalId;
  private String transactionType; // REPAYMENT, WAIVER, WRITEOFF
  private LocalDate transactionDate;
  private BigDecimal amount;
  private String paymentTypeName;
  private String note;
}
