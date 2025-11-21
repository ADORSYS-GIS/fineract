package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.Charge;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for charges (fees and penalties).
 *
 * <p>Creates charge definitions for loans and savings products.
 *
 * <p>API Endpoint: POST /api/v1/charges
 */
@Slf4j
@Component
public class ChargeLoader {

  private final FineractApiClient apiClient;
  private final UpsertService upsertService;

  public ChargeLoader(FineractApiClient apiClient, UpsertService upsertService) {
    this.apiClient = apiClient;
    this.upsertService = upsertService;
  }

  /**
   * Loads charges.
   *
   * @param charges list of charges
   * @param context import context
   * @param result import result
   */
  public void load(List<Charge> charges, ImportContext context, ImportResult result) {
    log.debug("Loading {} charges", charges.size());

    for (Charge charge : charges) {
      try {
        loadSingleCharge(charge, context, result);
      } catch (Exception ex) {
        log.error("Failed to load charge '{}': {}", charge.getName(), ex.getMessage());
        result.recordEntity("charge", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single charge.
   *
   * <p>Uses upsert logic: updates existing charge if found (by name), creates new if not.
   *
   * @param charge charge to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleCharge(Charge charge, ImportContext context, ImportResult result) {
    log.debug("Loading charge: {}", charge.getName());

    // Build request data
    Map<String, Object> request = buildRequest(charge, context);

    // Use upsert service (search by name, update if exists, create if not)
    UpsertService.UpsertResult upsertResult =
        upsertService.upsert(
            "/api/v1/charges", request, "name", charge.getName(), context, result, "charge");

    // Store for reference (important for dependency resolution)
    context.registerEntity("charge", charge.getName(), upsertResult.getEntityId());

    if (upsertResult.wasCreated()) {
      log.info("✓ Created charge: {} (ID: {})", charge.getName(), upsertResult.getEntityId());
    } else if (upsertResult.wasUpdated()) {
      log.info("✓ Updated charge: {} (ID: {})", charge.getName(), upsertResult.getEntityId());
    } else {
      log.debug("✓ Charge unchanged: {} (ID: {})", charge.getName(), upsertResult.getEntityId());
    }
  }

  /**
   * Builds API request for charge.
   *
   * @param charge charge
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(Charge charge, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", charge.getName());
    request.put("currencyCode", charge.getCurrencyCode());
    request.put("amount", charge.getAmount());
    request.put("chargeAppliesTo", mapChargeAppliesTo(charge.getChargeAppliesTo()));
    request.put("chargeTimeType", mapChargeTimeType(charge.getChargeTimeType()));
    request.put(
        "chargeCalculationType", mapChargeCalculationType(charge.getChargeCalculationType()));

    if (charge.getChargePaymentMode() != null) {
      request.put("chargePaymentMode", mapChargePaymentMode(charge.getChargePaymentMode()));
    }

    if (charge.getActive() != null) {
      request.put("active", charge.getActive());
    }

    if (charge.getPenalty() != null) {
      request.put("penalty", charge.getPenalty());
    }

    // Resolve income account
    if (charge.getIncomeAccountCode() != null) {
      Long incomeAccountId = context.resolveEntityId("glAccount", charge.getIncomeAccountCode());
      if (incomeAccountId != null) {
        request.put("incomeAccountId", incomeAccountId);
      }
    }

    // Fee frequency for recurring charges
    if (charge.getFeeFrequency() != null && charge.getFeeInterval() != null) {
      request.put("feeFrequency", charge.getFeeFrequency());
      request.put("feeInterval", mapFeeInterval(charge.getFeeInterval()));
    }

    return request;
  }

  private Integer mapChargeAppliesTo(String appliesTo) {
    return switch (appliesTo.toUpperCase()) {
      case "LOAN" -> 1;
      case "SAVINGS" -> 2;
      case "CLIENT" -> 3;
      case "SHARES" -> 4;
      default -> throw new IllegalArgumentException("Invalid charge applies to: " + appliesTo);
    };
  }

  private Integer mapChargeTimeType(String timeType) {
    return switch (timeType.toUpperCase()) {
      case "DISBURSEMENT" -> 1;
      case "SPECIFIED_DUE_DATE" -> 2;
      case "INSTALMENT_FEE" -> 3;
      case "OVERDUE_INSTALLMENT" -> 4;
      case "SAVINGS_ACTIVATION" -> 5;
      case "WITHDRAWAL_FEE" -> 6;
      case "ANNUAL_FEE" -> 7;
      case "MONTHLY_FEE" -> 8;
      case "WEEKLY_FEE" -> 9;
      default -> throw new IllegalArgumentException("Invalid charge time type: " + timeType);
    };
  }

  private Integer mapChargeCalculationType(String calculationType) {
    return switch (calculationType.toUpperCase()) {
      case "FLAT" -> 1;
      case "PERCENT_OF_AMOUNT" -> 2;
      case "PERCENT_OF_AMOUNT_AND_INTEREST" -> 3;
      case "PERCENT_OF_INTEREST" -> 4;
      case "PERCENT_OF_DISBURSEMENT_AMOUNT" -> 5;
      default -> throw new IllegalArgumentException(
          "Invalid charge calculation type: " + calculationType);
    };
  }

  private Integer mapChargePaymentMode(String paymentMode) {
    return switch (paymentMode.toUpperCase()) {
      case "REGULAR" -> 0;
      case "ACCOUNT_TRANSFER" -> 1;
      default -> throw new IllegalArgumentException("Invalid charge payment mode: " + paymentMode);
    };
  }

  private Integer mapFeeInterval(String interval) {
    return switch (interval.toUpperCase()) {
      case "DAYS" -> 0;
      case "WEEKS" -> 1;
      case "MONTHS" -> 2;
      case "YEARS" -> 3;
      default -> throw new IllegalArgumentException("Invalid fee interval: " + interval);
    };
  }
}
