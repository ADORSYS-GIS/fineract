package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.SavingsProduct;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for savings products.
 *
 * <p>Creates savings product definitions with interest rates, balance requirements, and accounting
 * rules.
 *
 * <p>API Endpoint: POST /api/v1/savingsproducts
 */
@Slf4j
@Component
public class SavingsProductLoader {

  private final FineractApiClient apiClient;
  private final UpsertService upsertService;

  public SavingsProductLoader(FineractApiClient apiClient, UpsertService upsertService) {
    this.apiClient = apiClient;
    this.upsertService = upsertService;
  }

  /**
   * Loads savings products.
   *
   * @param savingsProducts list of savings products
   * @param context import context
   * @param result import result
   */
  public void load(
      List<SavingsProduct> savingsProducts, ImportContext context, ImportResult result) {
    log.debug("Loading {} savings products", savingsProducts.size());

    for (SavingsProduct savingsProduct : savingsProducts) {
      try {
        loadSingleSavingsProduct(savingsProduct, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load savings product '{}': {}", savingsProduct.getName(), ex.getMessage());
        result.recordEntity("savingsProduct", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single savings product.
   *
   * <p>Uses upsert logic: updates existing product if found (by shortName), creates new if not.
   *
   * @param savingsProduct savings product to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleSavingsProduct(
      SavingsProduct savingsProduct, ImportContext context, ImportResult result) {
    log.debug("Loading savings product: {}", savingsProduct.getName());

    // Build request data
    Map<String, Object> request = buildRequest(savingsProduct, context);

    // Use upsert service (search by shortName, update if exists, create if not)
    UpsertService.UpsertResult upsertResult =
        upsertService.upsert(
            "/api/v1/savingsproducts",
            request,
            "shortName",
            savingsProduct.getShortName(),
            context,
            result,
            "savingsProduct");

    // Store for reference (important for dependency resolution)
    context.registerEntity("savingsProduct", savingsProduct.getName(), upsertResult.getEntityId());

    if (upsertResult.wasCreated()) {
      log.info(
          "✓ Created savings product: {} (shortName: {}, ID: {})",
          savingsProduct.getName(),
          savingsProduct.getShortName(),
          upsertResult.getEntityId());
    } else if (upsertResult.wasUpdated()) {
      log.info(
          "✓ Updated savings product: {} (shortName: {}, ID: {})",
          savingsProduct.getName(),
          savingsProduct.getShortName(),
          upsertResult.getEntityId());
    } else {
      log.debug(
          "✓ Savings product unchanged: {} (shortName: {}, ID: {})",
          savingsProduct.getName(),
          savingsProduct.getShortName(),
          upsertResult.getEntityId());
    }
  }

  /**
   * Builds API request for savings product using centralized utilities.
   *
   * @param savingsProduct savings product
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(SavingsProduct savingsProduct, ImportContext context) {
    // Build request using RequestBuilder
    RequestBuilder builder = RequestBuilder.forProduct();

    // Basic information
    builder
        .put("name", savingsProduct.getName())
        .put("shortName", savingsProduct.getShortName())
        .put("currencyCode", savingsProduct.getCurrencyCode())
        .put("digitsAfterDecimal", savingsProduct.getDigitsAfterDecimal())
        .put("inMultiplesOf", 0)
        .putIfNotNull("description", savingsProduct.getDescription());

    // Interest configuration using FineractEnumMapper
    builder
        .put("nominalAnnualInterestRate", savingsProduct.getNominalAnnualInterestRate())
        .put(
            "interestCompoundingPeriodType",
            FineractEnumMapper.mapInterestCompoundingPeriodType(
                savingsProduct.getInterestCompoundingPeriodType()))
        .put(
            "interestPostingPeriodType",
            FineractEnumMapper.mapInterestPostingPeriodType(
                savingsProduct.getInterestPostingPeriodType()))
        .put(
            "interestCalculationType",
            FineractEnumMapper.mapInterestCalculationType(
                savingsProduct.getInterestCalculationType()))
        .put(
            "interestCalculationDaysInYearType",
            FineractEnumMapper.mapInterestCalculationDaysInYearType(
                savingsProduct.getInterestCalculationDaysInYearType()));

    // Balance requirements
    builder
        .putIfNotNull("minRequiredOpeningBalance", savingsProduct.getMinRequiredOpeningBalance())
        .putIfNotNull(
            "minBalanceForInterestCalculation",
            savingsProduct.getMinBalanceForInterestCalculation())
        .putIfNotNull("enforceMinRequiredBalance", savingsProduct.getEnforceMinRequiredBalance())
        .putIfNotNull("minRequiredBalance", savingsProduct.getMinRequiredBalance());

    // Withdrawal settings
    builder
        .putIfNotNull("withdrawalFeeForTransfers", savingsProduct.getWithdrawalFeeForTransfers())
        .putIfNotNull("allowOverdraft", savingsProduct.getAllowOverdraft())
        .putIfNotNull("overdraftLimit", savingsProduct.getOverdraftLimit());

    // Charges
    if (savingsProduct.getChargeNames() != null && !savingsProduct.getChargeNames().isEmpty()) {
      List<Long> chargeIds = new ArrayList<>();
      for (String chargeName : savingsProduct.getChargeNames()) {
        Long chargeId = context.resolveEntityId("charge", chargeName);
        if (chargeId != null) {
          chargeIds.add(chargeId);
        } else {
          log.warn(
              "Charge '{}' not found for savings product '{}'",
              chargeName,
              savingsProduct.getName());
        }
      }
      builder.put("charges", chargeIds);
    }

    // Accounting using FineractEnumMapper
    builder.put(
        "accountingRule", FineractEnumMapper.mapAccountingRule(savingsProduct.getAccountingRule()));

    if (!"NONE".equals(savingsProduct.getAccountingRule())) {
      addAccountingMappings(builder, savingsProduct, context);
    }

    return builder.build();
  }

  /**
   * Adds accounting mappings to request builder.
   *
   * @param builder request builder
   * @param savingsProduct savings product
   * @param context import context
   */
  private void addAccountingMappings(
      RequestBuilder builder, SavingsProduct savingsProduct, ImportContext context) {

    // Resolve all GL account references
    if (savingsProduct.getSavingsReferenceAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getSavingsReferenceAccountCode());
      builder.putIfNotNull("savingsReferenceAccountId", accountId);
    }

    if (savingsProduct.getSavingsControlAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getSavingsControlAccountCode());
      builder.putIfNotNull("savingsControlAccountId", accountId);
    }

    if (savingsProduct.getTransfersInSuspenseAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getTransfersInSuspenseAccountCode());
      builder.putIfNotNull("transfersInSuspenseAccountId", accountId);
    }

    if (savingsProduct.getInterestOnSavingsAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getInterestOnSavingsAccountCode());
      builder.putIfNotNull("interestOnSavingsAccountId", accountId);
    }

    if (savingsProduct.getIncomeFromFeesAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getIncomeFromFeesAccountCode());
      builder.putIfNotNull("incomeFromFeeAccountId", accountId);
    }

    if (savingsProduct.getIncomeFromPenaltiesAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getIncomeFromPenaltiesAccountCode());
      builder.putIfNotNull("incomeFromPenaltyAccountId", accountId);
    }

    if (savingsProduct.getOverdraftPortfolioControlAccountCode() != null) {
      Long accountId =
          context.resolveEntityId(
              "glAccount", savingsProduct.getOverdraftPortfolioControlAccountCode());
      builder.putIfNotNull("overdraftPortfolioControlId", accountId);
    }

    if (savingsProduct.getIncomeFromInterestAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getIncomeFromInterestAccountCode());
      builder.putIfNotNull("incomeFromInterestId", accountId);
    }
  }
}
