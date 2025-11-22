package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.account.LoanCollateral;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for loan collaterals.
 *
 * <p>Creates collateral records for loan accounts.
 *
 * <p>API Endpoint: POST /api/v1/loans/{loanId}/collaterals
 */
@Slf4j
@Component
public class LoanCollateralLoader {

  private final FineractApiClient apiClient;

  public LoanCollateralLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads loan collaterals.
   *
   * @param collaterals list of loan collaterals
   * @param context import context
   * @param result import result
   */
  public void load(List<LoanCollateral> collaterals, ImportContext context, ImportResult result) {
    log.debug("Loading {} loan collaterals", collaterals.size());

    for (LoanCollateral collateral : collaterals) {
      try {
        loadSingleCollateral(collateral, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load loan collateral for loan '{}': {}",
            collateral.getLoanExternalId(),
            ex.getMessage());
        result.recordEntity("loanCollateral", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single loan collateral.
   *
   * @param collateral collateral to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleCollateral(
      LoanCollateral collateral, ImportContext context, ImportResult result) {
    log.debug("Loading loan collateral for loan: {}", collateral.getLoanExternalId());

    // Resolve loan account ID
    Long loanId = context.resolveEntityId("loanAccount", collateral.getLoanExternalId());
    if (loanId == null) {
      throw new IllegalStateException(
          "Loan account '" + collateral.getLoanExternalId() + "' not found");
    }

    // Resolve collateral type
    Long collateralTypeId =
        context.resolveEntityId("collateralType", collateral.getCollateralTypeName());
    if (collateralTypeId == null) {
      throw new IllegalStateException(
          "Collateral type '" + collateral.getCollateralTypeName() + "' not found");
    }

    // Check if collateral already exists (idempotency)
    if (collateralAlreadyExists(loanId, collateralTypeId, collateral)) {
      log.info(
          "Loan collateral already exists for loan '{}': {}, skipping",
          collateral.getLoanExternalId(),
          collateral.getCollateralTypeName());
      result.recordEntity("loanCollateral", ImportResult.EntityAction.UNCHANGED);
      return;
    }

    // Build request
    Map<String, Object> request = buildRequest(collateral, collateralTypeId);

    // Create collateral
    String endpoint = "/api/v1/loans/" + loanId + "/collaterals";
    Map<String, Object> response = apiClient.post(endpoint, request, Map.class);
    Long collateralId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Loan collateral created for loan '{}': {} (ID: {})",
        collateral.getLoanExternalId(),
        collateral.getCollateralTypeName(),
        collateralId);
    result.recordEntity("loanCollateral", ImportResult.EntityAction.CREATED);
  }

  /**
   * Checks if a collateral with the same type already exists for the loan.
   *
   * @param loanId loan ID
   * @param collateralTypeId collateral type ID
   * @param collateral collateral to check
   * @return true if similar collateral already exists
   */
  @SuppressWarnings("unchecked")
  private boolean collateralAlreadyExists(
      Long loanId, Long collateralTypeId, LoanCollateral collateral) {
    try {
      // Get existing collaterals for this loan
      String endpoint = "/api/v1/loans/" + loanId + "/collaterals";
      List<Map<String, Object>> existingCollaterals = apiClient.get(endpoint, List.class);

      if (existingCollaterals == null || existingCollaterals.isEmpty()) {
        return false;
      }

      for (Map<String, Object> existing : existingCollaterals) {
        // Check collateral type match
        Map<String, Object> typeObj = (Map<String, Object>) existing.get("type");
        if (typeObj == null) {
          continue;
        }
        Long existingTypeId = ((Number) typeObj.get("id")).longValue();

        if (collateralTypeId.equals(existingTypeId)) {
          // Same collateral type - check if value matches to be more precise
          Object valueObj = existing.get("value");
          if (collateral.getValue() != null && valueObj != null) {
            java.math.BigDecimal existingValue = new java.math.BigDecimal(valueObj.toString());
            if (existingValue.compareTo(collateral.getValue()) == 0) {
              return true;
            }
          } else {
            // No value to compare, consider it duplicate by type alone
            return true;
          }
        }
      }
      return false;
    } catch (Exception ex) {
      log.debug("Error checking existing collaterals: {}", ex.getMessage());
      return false;
    }
  }

  /**
   * Builds API request for loan collateral.
   *
   * @param collateral collateral
   * @param collateralTypeId resolved collateral type ID
   * @return request map
   */
  private Map<String, Object> buildRequest(LoanCollateral collateral, Long collateralTypeId) {
    Map<String, Object> request = new HashMap<>();
    request.put("locale", "en");

    request.put("collateralTypeId", collateralTypeId);

    if (collateral.getValue() != null) {
      request.put("value", collateral.getValue());
    }

    if (collateral.getDescription() != null) {
      request.put("description", collateral.getDescription());
    }

    return request;
  }
}
