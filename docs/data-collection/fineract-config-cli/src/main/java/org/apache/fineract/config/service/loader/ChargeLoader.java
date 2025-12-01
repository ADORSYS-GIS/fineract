package org.apache.fineract.config.service.loader;

import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.Charge;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
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
   * Builds API request for charge using centralized utilities.
   *
   * @param charge charge
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(Charge charge, ImportContext context) {
    // Build base request using RequestBuilder - locale is REQUIRED by Fineract API for charges
    RequestBuilder builder = RequestBuilder.create().put("locale", "en");

    // Basic fields
    builder
        .put("name", charge.getName())
        .put("currencyCode", charge.getCurrencyCode())
        .put("amount", charge.getAmount());

    // Enum fields using FineractEnumMapper
    builder
        .put("chargeAppliesTo", FineractEnumMapper.mapChargeAppliesTo(charge.getChargeAppliesTo()))
        .put("chargeTimeType", FineractEnumMapper.mapChargeTimeType(charge.getChargeTimeType()))
        .put(
            "chargeCalculationType",
            FineractEnumMapper.mapChargeCalculationType(charge.getChargeCalculationType()));

    // chargePaymentMode is mandatory for loan charges (0=REGULAR, 1=ACCOUNT_TRANSFER)
    // Default to 0 (REGULAR) if not specified
    if (charge.getChargePaymentMode() != null) {
      builder.put(
          "chargePaymentMode",
          FineractEnumMapper.mapChargePaymentMode(charge.getChargePaymentMode()));
    } else {
      // Default to REGULAR payment mode (0) for loan charges
      builder.put("chargePaymentMode", 0);
    }

    // Optional boolean fields
    builder.putIfNotNull("active", charge.getActive()).putIfNotNull("penalty", charge.getPenalty());

    // Resolve income account reference
    if (charge.getIncomeAccountCode() != null) {
      Long incomeAccountId = context.resolveEntityId("glAccount", charge.getIncomeAccountCode());
      builder.putIfNotNull("incomeAccountId", incomeAccountId);
    }

    // Fee frequency for recurring charges
    if (charge.getFeeFrequency() != null && charge.getFeeInterval() != null) {
      builder
          .put("feeFrequency", charge.getFeeFrequency())
          .put("feeInterval", FineractEnumMapper.mapFeeInterval(charge.getFeeInterval()));
    }

    return builder.build();
  }
}
