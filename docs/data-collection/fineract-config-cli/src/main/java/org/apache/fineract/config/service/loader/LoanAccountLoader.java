package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.account.LoanAccount;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for loan accounts.
 *
 * <p>Creates loan account records for clients or groups.
 *
 * <p>IMPORTANT: Loan accounts have very limited update support. Most fields are immutable after
 * creation (productId, clientId, groupId, principal, submittedOnDate, approvedOnDate,
 * disbursementDate, etc.). Updates are generally NOT recommended. This loader detects existing
 * accounts and skips them to prevent errors.
 *
 * <p>API Endpoint: POST /api/v1/loans
 */
@Slf4j
@Component
public class LoanAccountLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");
  private static final String LOCALE = "en";
  private static final String DATE_FORMAT = "dd MMMM yyyy";

  private final FineractApiClient apiClient;

  public LoanAccountLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads loan accounts.
   *
   * @param loanAccounts list of loan accounts
   * @param context import context
   * @param result import result
   */
  public void load(List<LoanAccount> loanAccounts, ImportContext context, ImportResult result) {
    log.debug("Loading {} loan accounts", loanAccounts.size());

    for (LoanAccount loanAccount : loanAccounts) {
      try {
        loadSingleLoanAccount(loanAccount, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load loan account '{}': {}", loanAccount.getExternalId(), ex.getMessage());
        result.recordEntity("loanAccount", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single loan account.
   *
   * @param loanAccount loan account to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleLoanAccount(
      LoanAccount loanAccount, ImportContext context, ImportResult result) {
    log.debug("Loading loan account: {}", loanAccount.getExternalId());

    // Check if loan account already exists by external ID
    if (loanAccount.getExternalId() != null) {
      try {
        Map<String, Object> existingLoan =
            apiClient.get("/api/v1/loans?externalId=" + loanAccount.getExternalId(), Map.class);

        if (existingLoan != null && existingLoan.containsKey("id")) {
          Long loanId = ((Number) existingLoan.get("id")).longValue();
          log.debug(
              "Loan account already exists: {} (ID: {})", loanAccount.getExternalId(), loanId);
          result.recordEntity("loanAccount", ImportResult.EntityAction.UNCHANGED);

          // Store for reference
          context.registerEntity("loanAccount", loanAccount.getExternalId(), loanId);
          return;
        }
      } catch (Exception ex) {
        // Loan not found, proceed with creation
        log.debug("Loan account not found by external ID, will create new loan");
      }
    }

    // Create new loan account
    Map<String, Object> request = buildRequest(loanAccount, context);
    Map<String, Object> response = apiClient.post("/api/v1/loans", request, Map.class);
    Long loanId = ((Number) response.get("loanId")).longValue();

    log.info("Loan account created: {} (ID: {})", loanAccount.getExternalId(), loanId);
    result.recordEntity("loanAccount", ImportResult.EntityAction.CREATED);

    // Store for reference
    if (loanAccount.getExternalId() != null) {
      context.registerEntity("loanAccount", loanAccount.getExternalId(), loanId);
    }

    // Handle workflow state for maker-checker demonstration
    String workflowState = loanAccount.getWorkflowState();
    if (workflowState == null || workflowState.isEmpty()) {
      workflowState = "active"; // Default to active (normal flow)
    }

    switch (workflowState.toLowerCase()) {
      case "pending_approval":
        // Stop here - loan requires approval via maker-checker
        log.info(
            "⊙ Loan {} created - PENDING APPROVAL (maker-checker workflow)",
            loanAccount.getExternalId());
        log.info("   → Checker must approve via: POST /api/v1/loans/{}?command=approve", loanId);
        break;

      case "pending_disbursal":
        // Approve, but stop before disbursal
        if (loanAccount.getApprovalDate() != null) {
          approveLoan(loanId, loanAccount);
        }
        log.info(
            "⊙ Loan {} approved - PENDING DISBURSAL (maker-checker workflow)",
            loanAccount.getExternalId());
        log.info("   → Checker must disburse via: POST /api/v1/loans/{}?command=disburse", loanId);
        break;

      case "active":
      default:
        // Full workflow: Approve + Disburse (normal flow)
        if (Boolean.TRUE.equals(loanAccount.getApproveOnSubmit())
            && loanAccount.getApprovalDate() != null) {
          approveLoan(loanId, loanAccount);
        }

        if (Boolean.TRUE.equals(loanAccount.getDisbursementOnApproval())
            && loanAccount.getDisbursementDate() != null) {
          disburseLoan(loanId, loanAccount);
        }
        break;
    }
  }

  /**
   * Builds API request for loan account.
   *
   * @param loanAccount loan account
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(LoanAccount loanAccount, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("locale", LOCALE);
    request.put("dateFormat", DATE_FORMAT);

    if (loanAccount.getExternalId() != null) {
      request.put("externalId", loanAccount.getExternalId());
    }

    // Resolve product
    if (loanAccount.getProductName() != null) {
      Long productId = context.resolveEntityId("loanProduct", loanAccount.getProductName());
      if (productId != null) {
        request.put("productId", productId);
      } else {
        throw new IllegalStateException(
            "Loan product '" + loanAccount.getProductName() + "' not found");
      }
    }

    // Resolve client or group
    if (loanAccount.getClientExternalId() != null) {
      Long clientId = context.resolveEntityId("client", loanAccount.getClientExternalId());
      if (clientId != null) {
        request.put("clientId", clientId);
      } else {
        throw new IllegalStateException(
            "Client '" + loanAccount.getClientExternalId() + "' not found");
      }
    } else if (loanAccount.getGroupName() != null) {
      Long groupId = context.resolveEntityId("group", loanAccount.getGroupName());
      if (groupId != null) {
        request.put("groupId", groupId);
      } else {
        throw new IllegalStateException("Group '" + loanAccount.getGroupName() + "' not found");
      }
    } else {
      throw new IllegalStateException(
          "Loan account must have either clientExternalId or groupName");
    }

    // Resolve loan officer
    if (loanAccount.getLoanOfficerName() != null) {
      Long loanOfficerId = context.resolveEntityId("staff", loanAccount.getLoanOfficerName());
      if (loanOfficerId != null) {
        request.put("loanOfficerId", loanOfficerId);
      }
    }

    // Resolve fund source
    if (loanAccount.getFundSourceName() != null) {
      Long fundId = context.resolveEntityId("fundSource", loanAccount.getFundSourceName());
      if (fundId != null) {
        request.put("fundId", fundId);
      }
    }

    // Resolve loan purpose
    if (loanAccount.getLoanPurposeCode() != null) {
      Long loanPurposeId = context.resolveEntityId("loanPurpose", loanAccount.getLoanPurposeCode());
      if (loanPurposeId != null) {
        request.put("loanPurposeId", loanPurposeId);
      }
    }

    // Submitted date
    if (loanAccount.getSubmittedOnDate() != null) {
      request.put("submittedOnDate", loanAccount.getSubmittedOnDate().format(DATE_FORMATTER));
    }

    // Expected disbursement date
    if (loanAccount.getExpectedDisbursementDate() != null) {
      request.put(
          "expectedDisbursementDate",
          loanAccount.getExpectedDisbursementDate().format(DATE_FORMATTER));
    }

    // Loan terms (overrides)
    if (loanAccount.getPrincipal() != null) {
      request.put("principal", loanAccount.getPrincipal());
    }

    if (loanAccount.getNumberOfRepayments() != null) {
      request.put("numberOfRepayments", loanAccount.getNumberOfRepayments());
    }

    if (loanAccount.getRepaymentEvery() != null) {
      request.put("repaymentEvery", loanAccount.getRepaymentEvery());
    }

    if (loanAccount.getRepaymentFrequencyType() != null) {
      request.put(
          "repaymentFrequencyType",
          mapRepaymentFrequencyType(loanAccount.getRepaymentFrequencyType()));
    }

    if (loanAccount.getInterestRatePerPeriod() != null) {
      request.put("interestRatePerPeriod", loanAccount.getInterestRatePerPeriod());
    }

    if (loanAccount.getInterestType() != null) {
      request.put("interestType", mapInterestType(loanAccount.getInterestType()));
    }

    if (loanAccount.getInterestCalculationPeriodType() != null) {
      request.put(
          "interestCalculationPeriodType",
          mapInterestCalculationPeriodType(loanAccount.getInterestCalculationPeriodType()));
    }

    if (loanAccount.getAmortizationType() != null) {
      request.put("amortizationType", mapAmortizationType(loanAccount.getAmortizationType()));
    }

    // Charges
    if (loanAccount.getChargeNames() != null && !loanAccount.getChargeNames().isEmpty()) {
      List<Map<String, Object>> charges = new ArrayList<>();
      for (String chargeName : loanAccount.getChargeNames()) {
        Long chargeId = context.resolveEntityId("charge", chargeName);
        if (chargeId != null) {
          Map<String, Object> chargeEntry = new HashMap<>();
          chargeEntry.put("chargeId", chargeId);
          charges.add(chargeEntry);
        } else {
          log.warn(
              "Charge '{}' not found for loan account '{}', skipping",
              chargeName,
              loanAccount.getExternalId());
        }
      }
      if (!charges.isEmpty()) {
        request.put("charges", charges);
      }
    }

    // Transaction processing strategy (use mifos-standard-strategy)
    request.put("transactionProcessingStrategyId", 1);

    return request;
  }

  /**
   * Approves a loan.
   *
   * @param loanId loan ID
   * @param loanAccount loan account
   */
  private void approveLoan(Long loanId, LoanAccount loanAccount) {
    try {
      Map<String, Object> approvalRequest = new HashMap<>();
      approvalRequest.put("locale", LOCALE);
      approvalRequest.put("dateFormat", DATE_FORMAT);
      approvalRequest.put("approvedOnDate", loanAccount.getApprovalDate().format(DATE_FORMATTER));

      if (loanAccount.getExpectedDisbursementDate() != null) {
        approvalRequest.put(
            "expectedDisbursementDate",
            loanAccount.getExpectedDisbursementDate().format(DATE_FORMATTER));
      }

      apiClient.post("/api/v1/loans/" + loanId + "?command=approve", approvalRequest, Map.class);

      log.info("Loan approved: {} (ID: {})", loanAccount.getExternalId(), loanId);
    } catch (Exception ex) {
      log.warn("Failed to approve loan {}: {}", loanId, ex.getMessage());
    }
  }

  /**
   * Disburses a loan.
   *
   * @param loanId loan ID
   * @param loanAccount loan account
   */
  private void disburseLoan(Long loanId, LoanAccount loanAccount) {
    try {
      Map<String, Object> disbursementRequest = new HashMap<>();
      disbursementRequest.put("locale", LOCALE);
      disbursementRequest.put("dateFormat", DATE_FORMAT);
      disbursementRequest.put(
          "actualDisbursementDate", loanAccount.getDisbursementDate().format(DATE_FORMATTER));

      apiClient.post(
          "/api/v1/loans/" + loanId + "?command=disburse", disbursementRequest, Map.class);

      log.info("Loan disbursed: {} (ID: {})", loanAccount.getExternalId(), loanId);
    } catch (Exception ex) {
      log.warn("Failed to disburse loan {}: {}", loanId, ex.getMessage());
    }
  }

  private Integer mapRepaymentFrequencyType(String frequencyType) {
    return switch (frequencyType.toUpperCase()) {
      case "DAYS" -> 0;
      case "WEEKS" -> 1;
      case "MONTHS" -> 2;
      case "YEARS" -> 3;
      default -> throw new IllegalArgumentException(
          "Invalid repayment frequency type: " + frequencyType);
    };
  }

  private Integer mapInterestType(String interestType) {
    return switch (interestType.toUpperCase()) {
      case "DECLINING_BALANCE" -> 0;
      case "FLAT" -> 1;
      default -> throw new IllegalArgumentException("Invalid interest type: " + interestType);
    };
  }

  private Integer mapInterestCalculationPeriodType(String calculationType) {
    return switch (calculationType.toUpperCase()) {
      case "DAILY" -> 0;
      case "SAME_AS_REPAYMENT_PERIOD" -> 1;
      default -> throw new IllegalArgumentException(
          "Invalid interest calculation period type: " + calculationType);
    };
  }

  private Integer mapAmortizationType(String amortizationType) {
    return switch (amortizationType.toUpperCase()) {
      case "EQUAL_INSTALLMENTS" -> 1;
      case "EQUAL_PRINCIPAL" -> 0;
      default -> throw new IllegalArgumentException(
          "Invalid amortization type: " + amortizationType);
    };
  }
}
