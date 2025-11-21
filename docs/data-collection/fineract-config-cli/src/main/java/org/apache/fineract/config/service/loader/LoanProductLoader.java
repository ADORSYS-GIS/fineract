package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.LoanProduct;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
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
   * Builds API request for loan product using centralized utilities.
   *
   * @param loanProduct loan product
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(LoanProduct loanProduct, ImportContext context) {
    // Build request using RequestBuilder (loan products need locale for numeric fields)
    RequestBuilder builder = RequestBuilder.forProduct();

    // Basic information
    builder
        .put("name", loanProduct.getName())
        .put("shortName", loanProduct.getShortName())
        .put("currencyCode", loanProduct.getCurrencyCode())
        .put("digitsAfterDecimal", loanProduct.getDigitsAfterDecimal())
        .put("inMultiplesOf", 0)
        .putIfNotNull("description", loanProduct.getDescription());

    // Resolve fund source reference
    if (loanProduct.getFundSourceName() != null) {
      Long fundId = context.resolveEntityId("fundSource", loanProduct.getFundSourceName());
      builder.putIfNotNull("fundId", fundId);
    }

    // Principal limits
    builder
        .put("principal", loanProduct.getPrincipal())
        .putIfNotNull("minPrincipal", loanProduct.getMinPrincipal())
        .putIfNotNull("maxPrincipal", loanProduct.getMaxPrincipal());

    // Loan term
    builder
        .put("numberOfRepayments", loanProduct.getNumberOfRepayments())
        .putIfNotNull("minNumberOfRepayments", loanProduct.getMinNumberOfRepayments())
        .putIfNotNull("maxNumberOfRepayments", loanProduct.getMaxNumberOfRepayments())
        .put("repaymentEvery", loanProduct.getRepaymentEvery())
        .put(
            "repaymentFrequencyType",
            FineractEnumMapper.mapRepaymentFrequencyType(loanProduct.getRepaymentFrequencyType()));

    // Interest configuration using FineractEnumMapper
    builder
        .put("interestRatePerPeriod", loanProduct.getInterestRatePerPeriod())
        .putIfNotNull("minInterestRatePerPeriod", loanProduct.getMinInterestRatePerPeriod())
        .putIfNotNull("maxInterestRatePerPeriod", loanProduct.getMaxInterestRatePerPeriod())
        .put(
            "interestRateFrequencyType",
            FineractEnumMapper.mapInterestRateFrequencyType(
                loanProduct.getInterestRateFrequencyType()))
        .put("interestType", FineractEnumMapper.mapInterestType(loanProduct.getInterestType()))
        .put(
            "interestCalculationPeriodType",
            FineractEnumMapper.mapInterestCalculationPeriodType(
                loanProduct.getInterestCalculationPeriodType()))
        .put(
            "amortizationType",
            FineractEnumMapper.mapAmortizationType(loanProduct.getAmortizationType()));

    // Transaction processing strategy (mifos-standard-strategy is most common)
    builder.put("transactionProcessingStrategyId", 1);

    // Resolve charge references
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
      builder.put("charges", chargeIds);
    }

    // Accounting using FineractEnumMapper
    builder.put(
        "accountingRule", FineractEnumMapper.mapAccountingRule(loanProduct.getAccountingRule()));

    if (!"NONE".equals(loanProduct.getAccountingRule())) {
      addAccountingMappings(builder, loanProduct, context);
    }

    // Optional settings
    builder
        .putIfNotNull("includeInBorrowerCycle", loanProduct.getIncludeInBorrowerCycle())
        .putIfNotNull("useBorrowerCycle", loanProduct.getUseBorrowerCycle())
        .putIfNotNull("multiDisburseLoan", loanProduct.getMultiDisburseLoan())
        .putIfNotNull("maxTrancheCount", loanProduct.getMaxTrancheCount())
        .putIfNotNull("outstandingLoanBalance", loanProduct.getOutstandingLoanBalance());

    return builder.build();
  }

  /**
   * Adds accounting mappings to request builder.
   *
   * @param builder request builder
   * @param loanProduct loan product
   * @param context import context
   */
  private void addAccountingMappings(
      RequestBuilder builder, LoanProduct loanProduct, ImportContext context) {

    // Resolve all GL account references
    if (loanProduct.getFundSourceAccountCode() != null) {
      Long accountId = context.resolveEntityId("glAccount", loanProduct.getFundSourceAccountCode());
      builder.putIfNotNull("fundSourceAccountId", accountId);
    }

    if (loanProduct.getLoanPortfolioAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", loanProduct.getLoanPortfolioAccountCode());
      builder.putIfNotNull("loanPortfolioAccountId", accountId);
    }

    if (loanProduct.getInterestOnLoansAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", loanProduct.getInterestOnLoansAccountCode());
      builder.putIfNotNull("interestOnLoanAccountId", accountId);
    }

    if (loanProduct.getIncomeFromFeesAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", loanProduct.getIncomeFromFeesAccountCode());
      builder.putIfNotNull("incomeFromFeeAccountId", accountId);
    }

    if (loanProduct.getIncomeFromPenaltiesAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", loanProduct.getIncomeFromPenaltiesAccountCode());
      builder.putIfNotNull("incomeFromPenaltyAccountId", accountId);
    }

    if (loanProduct.getWriteOffAccountCode() != null) {
      Long accountId = context.resolveEntityId("glAccount", loanProduct.getWriteOffAccountCode());
      builder.putIfNotNull("writeOffAccountId", accountId);
    }
  }
}
