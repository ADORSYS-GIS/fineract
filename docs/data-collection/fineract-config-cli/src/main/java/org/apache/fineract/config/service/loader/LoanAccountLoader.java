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
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
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
        Map<String, Object> response =
            apiClient.get("/api/v1/loans?externalId=" + loanAccount.getExternalId(), Map.class);

        // Response is paginated: {"totalFilteredRecords":1,"pageItems":[...]}
        if (response != null && response.containsKey("pageItems")) {
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> pageItems =
              (List<Map<String, Object>>) response.get("pageItems");
          if (pageItems != null && !pageItems.isEmpty()) {
            Map<String, Object> existingLoan = pageItems.get(0);
            Long loanId = ((Number) existingLoan.get("id")).longValue();
            log.debug(
                "Loan account already exists: {} (ID: {})", loanAccount.getExternalId(), loanId);
            result.recordEntity("loanAccount", ImportResult.EntityAction.UNCHANGED);

            // Store for reference
            context.registerEntity("loanAccount", loanAccount.getExternalId(), loanId);

            // Check if loan needs approval/disbursement based on workflow state
            String workflowState = loanAccount.getWorkflowState();
            if (workflowState == null || workflowState.isEmpty()) {
              workflowState = "active"; // Default to active (normal flow)
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> status = (Map<String, Object>) existingLoan.get("status");
            if (status != null && "active".equalsIgnoreCase(workflowState)) {
              String statusCode = (String) status.get("code");

              // For workflowState=active, auto-derive dates if not explicitly set
              java.time.LocalDate effectiveApprovalDate = loanAccount.getApprovalDate();
              java.time.LocalDate effectiveDisbursementDate = loanAccount.getDisbursementDate();

              if (effectiveApprovalDate == null && loanAccount.getSubmittedOnDate() != null) {
                effectiveApprovalDate = loanAccount.getSubmittedOnDate();
              }
              if (effectiveDisbursementDate == null && loanAccount.getSubmittedOnDate() != null) {
                // Default disbursement to submitted date + 1 day (avoiding weekends)
                effectiveDisbursementDate = loanAccount.getSubmittedOnDate().plusDays(1);
              }

              // Approve if pending approval
              if ("loanStatusType.submitted.and.pending.approval".equals(statusCode)) {
                if (effectiveApprovalDate != null) {
                  log.debug(
                      "Existing loan {} needs approval, current status: {}",
                      loanAccount.getExternalId(),
                      statusCode);
                  approveLoanWithDate(
                      loanId, loanAccount, effectiveApprovalDate, effectiveDisbursementDate);
                  // Refresh status for disbursement check
                  statusCode = "loanStatusType.approved";
                }
              }
              // Disburse if approved but not disbursed
              if ("loanStatusType.approved".equals(statusCode)) {
                if (effectiveDisbursementDate != null) {
                  log.debug("Existing loan {} needs disbursement", loanAccount.getExternalId());
                  disburseLoanWithDate(loanId, loanAccount, effectiveDisbursementDate);
                }
              }
            }
            return;
          }
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
  @SuppressWarnings("unchecked")
  private Map<String, Object> buildRequest(LoanAccount loanAccount, ImportContext context) {
    RequestBuilder builder = RequestBuilder.forAccount();

    builder.putIfNotNull("externalId", loanAccount.getExternalId());

    // Resolve product (try productName first, then productShortName)
    Long productId = null;
    String productRef = null;
    if (loanAccount.getProductName() != null) {
      productRef = loanAccount.getProductName();
      productId = context.resolveEntityId("loanProduct", productRef);
    }
    if (productId == null && loanAccount.getProductShortName() != null) {
      productRef = loanAccount.getProductShortName();
      productId = resolveLoanProductByShortName(loanAccount.getProductShortName(), context);
    }
    if (productId != null) {
      builder.put("productId", productId);
    } else {
      throw new IllegalStateException("Loan product '" + productRef + "' not found");
    }

    // Fetch product details for default values
    Map<String, Object> productDetails = fetchLoanProductDetails(productId);

    // Resolve client or group
    if (loanAccount.getClientExternalId() != null) {
      Long clientId = context.resolveEntityId("client", loanAccount.getClientExternalId());
      if (clientId != null) {
        builder.put("clientId", clientId);
      } else {
        throw new IllegalStateException(
            "Client '" + loanAccount.getClientExternalId() + "' not found");
      }
    } else if (loanAccount.getGroupName() != null) {
      Long groupId = context.resolveEntityId("group", loanAccount.getGroupName());
      if (groupId != null) {
        builder.put("groupId", groupId);
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
        builder.put("loanOfficerId", loanOfficerId);
      }
    }

    // Resolve fund source
    if (loanAccount.getFundSourceName() != null) {
      Long fundId = context.resolveEntityId("fundSource", loanAccount.getFundSourceName());
      if (fundId != null) {
        builder.put("fundId", fundId);
      }
    }

    // Resolve loan purpose
    if (loanAccount.getLoanPurposeCode() != null) {
      Long loanPurposeId = context.resolveEntityId("loanPurpose", loanAccount.getLoanPurposeCode());
      if (loanPurposeId != null) {
        builder.put("loanPurposeId", loanPurposeId);
      }
    }

    // Submitted date
    if (loanAccount.getSubmittedOnDate() != null) {
      builder.put("submittedOnDate", loanAccount.getSubmittedOnDate().format(DATE_FORMATTER));
    }

    // Expected disbursement date - required, default to submitted date + 1 day
    if (loanAccount.getExpectedDisbursementDate() != null) {
      builder.put(
          "expectedDisbursementDate",
          loanAccount.getExpectedDisbursementDate().format(DATE_FORMATTER));
    } else if (loanAccount.getSubmittedOnDate() != null) {
      builder.put(
          "expectedDisbursementDate",
          loanAccount.getSubmittedOnDate().plusDays(1).format(DATE_FORMATTER));
    }

    // loanType - required (individual=1, group=2, jlg=3)
    if (loanAccount.getClientExternalId() != null) {
      builder.put("loanType", "individual");
    } else {
      builder.put("loanType", "group");
    }

    // Loan terms (overrides or defaults)
    builder.putIfNotNull("principal", loanAccount.getPrincipal());

    // numberOfRepayments - required
    if (loanAccount.getNumberOfRepayments() != null) {
      builder.put("numberOfRepayments", loanAccount.getNumberOfRepayments());
    } else {
      builder.put("numberOfRepayments", 12); // Default to 12 months
    }

    // loanTermFrequency - required (same as numberOfRepayments if monthly)
    builder.put(
        "loanTermFrequency",
        loanAccount.getNumberOfRepayments() != null ? loanAccount.getNumberOfRepayments() : 12);
    builder.put("loanTermFrequencyType", 2); // 2 = MONTHS

    // repaymentEvery - required
    if (loanAccount.getRepaymentEvery() != null) {
      builder.put("repaymentEvery", loanAccount.getRepaymentEvery());
    } else {
      builder.put("repaymentEvery", 1); // Default to every 1 period
    }

    // repaymentFrequencyType - required
    if (loanAccount.getRepaymentFrequencyType() != null) {
      builder.put(
          "repaymentFrequencyType",
          FineractEnumMapper.mapRepaymentFrequencyType(loanAccount.getRepaymentFrequencyType()));
    } else {
      builder.put("repaymentFrequencyType", 2); // 2 = MONTHS
    }

    // interestRatePerPeriod - required, use product's default if not specified
    if (loanAccount.getInterestRatePerPeriod() != null) {
      builder.put("interestRatePerPeriod", loanAccount.getInterestRatePerPeriod());
    } else if (productDetails != null && productDetails.get("interestRatePerPeriod") != null) {
      builder.put("interestRatePerPeriod", productDetails.get("interestRatePerPeriod"));
    } else {
      // Fallback: this will likely fail validation
      log.warn("No interestRatePerPeriod specified and could not get default from product");
    }

    // interestType - required (0=DECLINING_BALANCE, 1=FLAT)
    if (loanAccount.getInterestType() != null) {
      builder.put(
          "interestType", FineractEnumMapper.mapInterestType(loanAccount.getInterestType()));
    } else {
      builder.put("interestType", 0); // Default to DECLINING_BALANCE
    }

    // interestCalculationPeriodType - required (0=DAILY, 1=SAME_AS_REPAYMENT_PERIOD)
    if (loanAccount.getInterestCalculationPeriodType() != null) {
      builder.put(
          "interestCalculationPeriodType",
          FineractEnumMapper.mapInterestCalculationPeriodType(
              loanAccount.getInterestCalculationPeriodType()));
    } else {
      builder.put("interestCalculationPeriodType", 1); // Default to SAME_AS_REPAYMENT_PERIOD
    }

    // amortizationType - required (0=EQUAL_PRINCIPAL, 1=EQUAL_INSTALLMENTS)
    if (loanAccount.getAmortizationType() != null) {
      builder.put(
          "amortizationType",
          FineractEnumMapper.mapAmortizationType(loanAccount.getAmortizationType()));
    } else {
      builder.put("amortizationType", 1); // Default to EQUAL_INSTALLMENTS
    }

    // transactionProcessingStrategyCode - required for modern Fineract
    builder.put("transactionProcessingStrategyCode", "mifos-standard-strategy");

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
        builder.put("charges", charges);
      }
    }

    // Transaction processing strategy (use code instead of ID for Fineract 1.9+)
    builder.put("transactionProcessingStrategyCode", "mifos-standard-strategy");

    return builder.build();
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

  /**
   * Approves a loan with explicit date parameters.
   *
   * @param loanId loan ID
   * @param loanAccount loan account
   * @param approvalDate the approval date
   * @param expectedDisbursementDate the expected disbursement date
   */
  private void approveLoanWithDate(
      Long loanId,
      LoanAccount loanAccount,
      java.time.LocalDate approvalDate,
      java.time.LocalDate expectedDisbursementDate) {
    try {
      Map<String, Object> approvalRequest = new HashMap<>();
      approvalRequest.put("locale", LOCALE);
      approvalRequest.put("dateFormat", DATE_FORMAT);
      approvalRequest.put("approvedOnDate", approvalDate.format(DATE_FORMATTER));

      if (expectedDisbursementDate != null) {
        approvalRequest.put(
            "expectedDisbursementDate", expectedDisbursementDate.format(DATE_FORMATTER));
      }

      apiClient.post("/api/v1/loans/" + loanId + "?command=approve", approvalRequest, Map.class);

      log.info("Loan approved: {} (ID: {})", loanAccount.getExternalId(), loanId);
    } catch (Exception ex) {
      log.warn("Failed to approve loan {}: {}", loanId, ex.getMessage());
    }
  }

  /**
   * Disburses a loan with explicit date parameter.
   *
   * @param loanId loan ID
   * @param loanAccount loan account
   * @param disbursementDate the actual disbursement date
   */
  private void disburseLoanWithDate(
      Long loanId, LoanAccount loanAccount, java.time.LocalDate disbursementDate) {
    try {
      Map<String, Object> disbursementRequest = new HashMap<>();
      disbursementRequest.put("locale", LOCALE);
      disbursementRequest.put("dateFormat", DATE_FORMAT);
      disbursementRequest.put("actualDisbursementDate", disbursementDate.format(DATE_FORMATTER));

      apiClient.post(
          "/api/v1/loans/" + loanId + "?command=disburse", disbursementRequest, Map.class);

      log.info("Loan disbursed: {} (ID: {})", loanAccount.getExternalId(), loanId);
    } catch (Exception ex) {
      log.warn("Failed to disburse loan {}: {}", loanId, ex.getMessage());
    }
  }

  /**
   * Resolves a loan product ID by short name.
   *
   * @param shortName product short name
   * @param context import context
   * @return product ID or null if not found
   */
  @SuppressWarnings("unchecked")
  private Long resolveLoanProductByShortName(String shortName, ImportContext context) {
    // Try from context first
    Long id = context.getEntityId("loanProduct", shortName);
    if (id != null) {
      return id;
    }

    // Fetch from API
    try {
      List<Map<String, Object>> products = apiClient.get("/api/v1/loanproducts", List.class);
      for (Map<String, Object> product : products) {
        if (shortName.equals(product.get("shortName"))) {
          Long productId = ((Number) product.get("id")).longValue();
          context.registerEntity("loanProduct", shortName, productId);
          return productId;
        }
      }
    } catch (Exception ex) {
      log.debug("Could not look up loan product by shortName '{}': {}", shortName, ex.getMessage());
    }

    return null;
  }

  /**
   * Fetches loan product details by ID.
   *
   * @param productId product ID
   * @return product details map or null
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> fetchLoanProductDetails(Long productId) {
    try {
      return apiClient.get("/api/v1/loanproducts/" + productId, Map.class);
    } catch (Exception ex) {
      log.debug("Could not fetch loan product details for ID {}: {}", productId, ex.getMessage());
      return null;
    }
  }
}
