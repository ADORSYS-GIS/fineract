package org.apache.fineract.config.model.account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Loan Account definition.
 *
 * <p>Represents a loan account for a client or group.
 */
@Data
public class LoanAccount {
  private String externalId;
  private String clientExternalId; // For individual loans
  private String groupName; // For group loans
  private String productName;
  private String loanOfficerName;
  private String fundSourceName;
  private LocalDate submittedOnDate;
  private LocalDate expectedDisbursementDate;

  // Loan terms (can override product defaults)
  private BigDecimal principal;
  private Integer numberOfRepayments;
  private Integer repaymentEvery;
  private String repaymentFrequencyType; // DAYS, WEEKS, MONTHS, YEARS
  private BigDecimal interestRatePerPeriod;
  private String interestType; // FLAT, DECLINING_BALANCE
  private String interestCalculationPeriodType; // DAILY, SAME_AS_REPAYMENT_PERIOD
  private String amortizationType; // EQUAL_INSTALLMENTS, EQUAL_PRINCIPAL

  // Loan purpose
  private String loanPurposeCode;

  // Charges (charge names)
  private List<String> chargeNames = new ArrayList<>();

  // Disbursement
  private Boolean disbursementOnApproval;
  private LocalDate disbursementDate;

  // Approval
  private Boolean approveOnSubmit;
  private LocalDate approvalDate;

  /**
   * Workflow state for demonstrating maker-checker workflows.
   *
   * <p>Valid values:
   *
   * <ul>
   *   <li><b>active</b>: Auto-approve and auto-disburse (normal flow, default)
   *   <li><b>pending_approval</b>: Create loan, stop at approval (requires checker to approve)
   *   <li><b>pending_disbursal</b>: Create + approve, stop at disbursal (requires checker to
   *       disburse)
   * </ul>
   *
   * <p>When null or empty, defaults to "active".
   */
  private String workflowState;
}
