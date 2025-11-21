package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.SavingsProduct;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
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
   * Builds API request for savings product.
   *
   * @param savingsProduct savings product
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(SavingsProduct savingsProduct, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    // Basic information
    request.put("name", savingsProduct.getName());
    request.put("shortName", savingsProduct.getShortName());
    request.put("currencyCode", savingsProduct.getCurrencyCode());
    request.put("digitsAfterDecimal", savingsProduct.getDigitsAfterDecimal());
    request.put("inMultiplesOf", 0);

    if (savingsProduct.getDescription() != null) {
      request.put("description", savingsProduct.getDescription());
    }

    // Interest configuration
    request.put("nominalAnnualInterestRate", savingsProduct.getNominalAnnualInterestRate());
    request.put(
        "interestCompoundingPeriodType",
        mapInterestCompoundingPeriodType(savingsProduct.getInterestCompoundingPeriodType()));
    request.put(
        "interestPostingPeriodType",
        mapInterestPostingPeriodType(savingsProduct.getInterestPostingPeriodType()));
    request.put(
        "interestCalculationType",
        mapInterestCalculationType(savingsProduct.getInterestCalculationType()));
    request.put(
        "interestCalculationDaysInYearType",
        mapInterestCalculationDaysInYearType(
            savingsProduct.getInterestCalculationDaysInYearType()));

    // Balance requirements
    if (savingsProduct.getMinRequiredOpeningBalance() != null) {
      request.put("minRequiredOpeningBalance", savingsProduct.getMinRequiredOpeningBalance());
    }
    if (savingsProduct.getMinBalanceForInterestCalculation() != null) {
      request.put(
          "minBalanceForInterestCalculation", savingsProduct.getMinBalanceForInterestCalculation());
    }
    if (savingsProduct.getEnforceMinRequiredBalance() != null) {
      request.put("enforceMinRequiredBalance", savingsProduct.getEnforceMinRequiredBalance());
    }
    if (savingsProduct.getMinRequiredBalance() != null) {
      request.put("minRequiredBalance", savingsProduct.getMinRequiredBalance());
    }

    // Withdrawal settings
    if (savingsProduct.getWithdrawalFeeForTransfers() != null) {
      request.put("withdrawalFeeForTransfers", savingsProduct.getWithdrawalFeeForTransfers());
    }
    if (savingsProduct.getAllowOverdraft() != null) {
      request.put("allowOverdraft", savingsProduct.getAllowOverdraft());
    }
    if (savingsProduct.getOverdraftLimit() != null) {
      request.put("overdraftLimit", savingsProduct.getOverdraftLimit());
    }

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
      request.put("charges", chargeIds);
    }

    // Accounting
    request.put("accountingRule", mapAccountingRule(savingsProduct.getAccountingRule()));

    if (!"NONE".equals(savingsProduct.getAccountingRule())) {
      addAccountingMappings(request, savingsProduct, context);
    }

    return request;
  }

  /**
   * Adds accounting mappings to request.
   *
   * @param request request map
   * @param savingsProduct savings product
   * @param context import context
   */
  private void addAccountingMappings(
      Map<String, Object> request, SavingsProduct savingsProduct, ImportContext context) {

    if (savingsProduct.getSavingsReferenceAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getSavingsReferenceAccountCode());
      if (accountId != null) {
        request.put("savingsReferenceAccountId", accountId);
      }
    }

    if (savingsProduct.getSavingsControlAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getSavingsControlAccountCode());
      if (accountId != null) {
        request.put("savingsControlAccountId", accountId);
      }
    }

    if (savingsProduct.getTransfersInSuspenseAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getTransfersInSuspenseAccountCode());
      if (accountId != null) {
        request.put("transfersInSuspenseAccountId", accountId);
      }
    }

    if (savingsProduct.getInterestOnSavingsAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getInterestOnSavingsAccountCode());
      if (accountId != null) {
        request.put("interestOnSavingsAccountId", accountId);
      }
    }

    if (savingsProduct.getIncomeFromFeesAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getIncomeFromFeesAccountCode());
      if (accountId != null) {
        request.put("incomeFromFeeAccountId", accountId);
      }
    }

    if (savingsProduct.getIncomeFromPenaltiesAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getIncomeFromPenaltiesAccountCode());
      if (accountId != null) {
        request.put("incomeFromPenaltyAccountId", accountId);
      }
    }

    if (savingsProduct.getOverdraftPortfolioControlAccountCode() != null) {
      Long accountId =
          context.resolveEntityId(
              "glAccount", savingsProduct.getOverdraftPortfolioControlAccountCode());
      if (accountId != null) {
        request.put("overdraftPortfolioControlId", accountId);
      }
    }

    if (savingsProduct.getIncomeFromInterestAccountCode() != null) {
      Long accountId =
          context.resolveEntityId("glAccount", savingsProduct.getIncomeFromInterestAccountCode());
      if (accountId != null) {
        request.put("incomeFromInterestId", accountId);
      }
    }
  }

  private Integer mapInterestCompoundingPeriodType(String periodType) {
    return switch (periodType.toUpperCase()) {
      case "DAILY" -> 1;
      case "MONTHLY" -> 4;
      case "QUARTERLY" -> 5;
      case "SEMI_ANNUAL" -> 6;
      case "ANNUAL" -> 7;
      default -> throw new IllegalArgumentException(
          "Invalid interest compounding period type: " + periodType);
    };
  }

  private Integer mapInterestPostingPeriodType(String periodType) {
    return switch (periodType.toUpperCase()) {
      case "MONTHLY" -> 4;
      case "QUARTERLY" -> 5;
      case "BIANNUAL" -> 6;
      case "ANNUAL" -> 7;
      default -> throw new IllegalArgumentException(
          "Invalid interest posting period type: " + periodType);
    };
  }

  private Integer mapInterestCalculationType(String calculationType) {
    return switch (calculationType.toUpperCase()) {
      case "DAILY_BALANCE" -> 1;
      case "AVERAGE_DAILY_BALANCE" -> 2;
      default -> throw new IllegalArgumentException(
          "Invalid interest calculation type: " + calculationType);
    };
  }

  private Integer mapInterestCalculationDaysInYearType(String daysInYearType) {
    return switch (daysInYearType.toUpperCase()) {
      case "DAYS_360" -> 360;
      case "DAYS_365" -> 365;
      default -> throw new IllegalArgumentException(
          "Invalid interest calculation days in year type: " + daysInYearType);
    };
  }

  private Integer mapAccountingRule(String accountingRule) {
    return switch (accountingRule.toUpperCase()) {
      case "NONE" -> 1;
      case "CASH_BASED" -> 2;
      default -> throw new IllegalArgumentException("Invalid accounting rule: " + accountingRule);
    };
  }
}
