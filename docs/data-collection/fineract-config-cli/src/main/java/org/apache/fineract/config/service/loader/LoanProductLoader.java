package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.LoanProduct;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for loan products.
 *
 * <p>Creates loan product definitions with terms, interest rates, charges, and accounting rules.
 *
 * <p>API Endpoint: POST /api/v1/loanproducts
 */
@Slf4j
@Component
public class LoanProductLoader {

  private final FineractApiClient apiClient;
  private final UpsertService upsertService;

  public LoanProductLoader(FineractApiClient apiClient, UpsertService upsertService) {
    this.apiClient = apiClient;
    this.upsertService = upsertService;
  }

  /**
   * Loads loan products.
   *
   * @param loanProducts list of loan products
   * @param context import context
   * @param result import result
   */
  public void load(List<LoanProduct> loanProducts, ImportContext context, ImportResult result) {
    log.debug("Loading {} loan products", loanProducts.size());

    for (LoanProduct loanProduct : loanProducts) {
      try {
        loadSingleLoanProduct(loanProduct, context, result);
      } catch (Exception ex) {
        log.error("Failed to load loan product '{}': {}", loanProduct.getName(), ex.getMessage());
        result.recordEntity("loanProduct", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single loan product.
   *
   * <p>Uses upsert logic: updates existing product if found (by shortName), creates new if not.
   *
   * @param loanProduct loan product to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleLoanProduct(
      LoanProduct loanProduct, ImportContext context, ImportResult result) {
    log.debug("Loading loan product: {}", loanProduct.getName());

    // Build request data
    Map<String, Object> request = buildRequest(loanProduct, context);

    // Use upsert service (search by shortName, update if exists, create if not)
    UpsertService.UpsertResult upsertResult =
        upsertService.upsert(
            "/api/v1/loanproducts",
            request,
            "shortName",
            loanProduct.getShortName(),
            context,
            result,
            "loanProduct");

    // Store for reference (important for dependency resolution)
    context.registerEntity("loanProduct", loanProduct.getName(), upsertResult.getEntityId());

    if (upsertResult.wasCreated()) {
      log.info(
          "✓ Created loan product: {} (shortName: {}, ID: {})",
          loanProduct.getName(),
          loanProduct.getShortName(),
          upsertResult.getEntityId());
    } else if (upsertResult.wasUpdated()) {
      log.info(
          "✓ Updated loan product: {} (shortName: {}, ID: {})",
          loanProduct.getName(),
          loanProduct.getShortName(),
          upsertResult.getEntityId());
    } else {
      log.debug(
          "✓ Loan product unchanged: {} (shortName: {}, ID: {})",
          loanProduct.getName(),
          loanProduct.getShortName(),
          upsertResult.getEntityId());
    }
  }

  /**
   * Builds API request for loan product.
   *
   * @param loanProduct loan product
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(LoanProduct loanProduct, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    // Basic information
    request.put("name", loanProduct.getName());
    request.put("shortName", loanProduct.getShortName());
    request.put("currencyCode", loanProduct.getCurrencyCode());
    request.put("digitsAfterDecimal", loanProduct.getDigitsAfterDecimal());
    request.put("inMultiplesOf", 0);

    if (loanProduct.getDescription() != null) {
      request.put("description", loanProduct.getDescription());
    }

    // Resolve fund source
    if (loanProduct.getFundSourceName() != null) {
      Long fundId = context.resolveEntityId("fundSource", loanProduct.getFundSourceName());
      if (fundId != null) {
        request.put("fundId", fundId);
      }
    }

    // Principal limits
    request.put("principal", loanProduct.getPrincipal());
    if (loanProduct.getMinPrincipal() != null) {
      request.put("minPrincipal", loanProduct.getMinPrincipal());
    }
    if (loanProduct.getMaxPrincipal() != null) {
      request.put("maxPrincipal", loanProduct.getMaxPrincipal());
    }

    // Loan term
    request.put("numberOfRepayments", loanProduct.getNumberOfRepayments());
    if (loanProduct.getMinNumberOfRepayments() != null) {
      request.put("minNumberOfRepayments", loanProduct.getMinNumberOfRepayments());
    }
    if (loanProduct.getMaxNumberOfRepayments() != null) {
      request.put("maxNumberOfRepayments", loanProduct.getMaxNumberOfRepayments());
    }

    request.put("repaymentEvery", loanProduct.getRepaymentEvery());
    request.put(
        "repaymentFrequencyType",
        mapRepaymentFrequencyType(loanProduct.getRepaymentFrequencyType()));

    // Interest configuration
    request.put("interestRatePerPeriod", loanProduct.getInterestRatePerPeriod());
    if (loanProduct.getMinInterestRatePerPeriod() != null) {
      request.put("minInterestRatePerPeriod", loanProduct.getMinInterestRatePerPeriod());
    }
    if (loanProduct.getMaxInterestRatePerPeriod() != null) {
      request.put("maxInterestRatePerPeriod", loanProduct.getMaxInterestRatePerPeriod());
    }

    request.put(
        "interestRateFrequencyType",
        mapInterestRateFrequencyType(loanProduct.getInterestRateFrequencyType()));
    request.put("interestType", mapInterestType(loanProduct.getInterestType()));
    request.put(
        "interestCalculationPeriodType",
        mapInterestCalculationPeriodType(loanProduct.getInterestCalculationPeriodType()));
    request.put("amortizationType", mapAmortizationType(loanProduct.getAmortizationType()));

    // Transaction processing strategy (mifos-standard-strategy is most common)
    request.put("transactionProcessingStrategyId", 1);

    // Charges
    if (loanProduct.getChargeNames() != null && !loanProduct.getChargeNames().isEmpty()) {
      List<Long> chargeIds = new ArrayList<>();
      for (String chargeName : loanProduct.getChargeNames()) {
        Long chargeId = context.resolveEntityId("charge", chargeName);
        if (chargeId != null) {
          chargeIds.add(chargeId);
        } else {
          log.warn(
              "Charge '{}' not found for loan product '{}'", chargeName, loanProduct.getName());
        }
      }
      request.put("charges", chargeIds);
    }

    // Accounting
    request.put("accountingRule", mapAccountingRule(loanProduct.getAccountingRule()));

    if (!"NONE".equals(loanProduct.getAccountingRule())) {
      addAccountingMappings(request, loanProduct, context);
    }

    // Settings
    if (loanProduct.getIncludeInBorrowerCycle() != null) {
      request.put("includeInBorrowerCycle", loanProduct.getIncludeInBorrowerCycle());
    }
    if (loanProduct.getUseBorrowerCycle() != null) {
      request.put("useBorrowerCycle", loanProduct.getUseBorrowerCycle());
    }
    if (loanProduct.getMultiDisburseLoan() != null) {
      request.put("multiDisburseLoan", loanProduct.getMultiDisburseLoan());
    }
    if (loanProduct.getMaxTrancheCount() != null) {
      request.put("maxTrancheCount", loanProduct.getMaxTrancheCount());
    }
    if (loanProduct.getOutstandingLoanBalance() != null) {
      request.put("outstandingLoanBalance", loanProduct.getOutstandingLoanBalance());
    }

    return request;
  }

  /**
   * Adds accounting mappings to request.
   *
   * @param request request map
   * @param loanProduct loan product
   * @param context import context
   */
  private void addAccountingMappings(
      Map<String, Object> request, LoanProduct loanProduct, ImportContext context) {

    if (loanProduct.getFundSourceAccountCode() != null) {
      Long accountId = context.resolveEntityId("glAccount", loanProduct.getFundSourceAccountCode());
      if (accountId != null) {
        request.put("fundSourceAccountId", accountId);
      }
    }

    if (loanProduct.getLoanPortfolioAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", loanProduct.getLoanPortfolioAccountCode());
      if (accountId != null) {
        request.put("loanPortfolioAccountId", accountId);
      }
    }

    if (loanProduct.getInterestOnLoansAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", loanProduct.getInterestOnLoansAccountCode());
      if (accountId != null) {
        request.put("interestOnLoanAccountId", accountId);
      }
    }

    if (loanProduct.getIncomeFromFeesAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", loanProduct.getIncomeFromFeesAccountCode());
      if (accountId != null) {
        request.put("incomeFromFeeAccountId", accountId);
      }
    }

    if (loanProduct.getIncomeFromPenaltiesAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", loanProduct.getIncomeFromPenaltiesAccountCode());
      if (accountId != null) {
        request.put("incomeFromPenaltyAccountId", accountId);
      }
    }

    if (loanProduct.getWriteOffAccountCode() != null) {
      Long accountId = context.resolveEntityId("glAccount", loanProduct.getWriteOffAccountCode());
      if (accountId != null) {
        request.put("writeOffAccountId", accountId);
      }
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

  private Integer mapInterestRateFrequencyType(String frequencyType) {
    return switch (frequencyType.toUpperCase()) {
      case "MONTH" -> 2;
      case "YEAR" -> 3;
      default -> throw new IllegalArgumentException(
          "Invalid interest rate frequency type: " + frequencyType);
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

  private Integer mapAccountingRule(String accountingRule) {
    return switch (accountingRule.toUpperCase()) {
      case "NONE" -> 1;
      case "CASH_BASED" -> 2;
      case "ACCRUAL_PERIODIC" -> 3;
      case "ACCRUAL_UPFRONT" -> 4;
      default -> throw new IllegalArgumentException("Invalid accounting rule: " + accountingRule);
    };
  }
}
