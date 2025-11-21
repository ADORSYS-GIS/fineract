package org.apache.fineract.config.model.account;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * Savings Account definition.
 *
 * <p>Represents a savings account for a client or group.
 */
@Data
public class SavingsAccount {
  private String externalId;
  private String clientExternalId; // For individual accounts
  private String groupName; // For group accounts
  private String productName;
  private String fieldOfficerName;
  private LocalDate submittedOnDate;
  private BigDecimal nominalAnnualInterestRate; // Optional override
  private Boolean active;
  private LocalDate activationDate;
}
