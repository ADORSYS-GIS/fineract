package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.account.LoanGuarantor;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for loan guarantors.
 *
 * <p>Creates guarantor records for loan accounts.
 *
 * <p>API Endpoint: POST /api/v1/loans/{loanId}/guarantors
 */
@Slf4j
@Component
public class LoanGuarantorLoader {

  private final FineractApiClient apiClient;

  public LoanGuarantorLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads loan guarantors.
   *
   * @param guarantors list of loan guarantors
   * @param context import context
   * @param result import result
   */
  public void load(List<LoanGuarantor> guarantors, ImportContext context, ImportResult result) {
    log.debug("Loading {} loan guarantors", guarantors.size());

    for (LoanGuarantor guarantor : guarantors) {
      try {
        loadSingleGuarantor(guarantor, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load loan guarantor for loan '{}': {}",
            guarantor.getLoanExternalId(),
            ex.getMessage());
        result.recordEntity("loanGuarantor", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single loan guarantor.
   *
   * @param guarantor guarantor to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleGuarantor(
      LoanGuarantor guarantor, ImportContext context, ImportResult result) {
    log.debug("Loading loan guarantor for loan: {}", guarantor.getLoanExternalId());

    // Resolve loan account ID
    Long loanId = context.resolveEntityId("loanAccount", guarantor.getLoanExternalId());
    if (loanId == null) {
      throw new IllegalStateException(
          "Loan account '" + guarantor.getLoanExternalId() + "' not found");
    }

    // Check if guarantor already exists
    if (guarantorAlreadyExists(loanId, guarantor, context)) {
      log.info(
          "Loan guarantor already exists for loan '{}': {}, skipping",
          guarantor.getLoanExternalId(),
          guarantor.getGuarantorType());
      result.recordEntity("loanGuarantor", ImportResult.EntityAction.UNCHANGED);
      return;
    }

    // Build request
    Map<String, Object> request = buildRequest(guarantor, context);

    // Create guarantor
    String endpoint = "/api/v1/loans/" + loanId + "/guarantors";
    Map<String, Object> response = apiClient.post(endpoint, request, Map.class);
    Long guarantorId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Loan guarantor created for loan '{}': {} (ID: {})",
        guarantor.getLoanExternalId(),
        guarantor.getGuarantorType(),
        guarantorId);
    result.recordEntity("loanGuarantor", ImportResult.EntityAction.CREATED);
  }

  /**
   * Checks if a guarantor already exists for the loan.
   *
   * @param loanId loan ID
   * @param guarantor guarantor to check
   * @param context import context
   * @return true if guarantor already exists
   */
  @SuppressWarnings("unchecked")
  private boolean guarantorAlreadyExists(
      Long loanId, LoanGuarantor guarantor, ImportContext context) {
    try {
      // Get existing guarantors for this loan
      String endpoint = "/api/v1/loans/" + loanId + "/guarantors";
      List<Map<String, Object>> existingGuarantors = apiClient.get(endpoint, List.class);

      if (existingGuarantors == null || existingGuarantors.isEmpty()) {
        return false;
      }

      Integer guarantorTypeId = mapGuarantorType(guarantor.getGuarantorType());

      for (Map<String, Object> existing : existingGuarantors) {
        // Check guarantor type match
        Map<String, Object> typeObj = (Map<String, Object>) existing.get("guarantorType");
        if (typeObj == null) {
          continue;
        }
        Integer existingTypeId = ((Number) typeObj.get("id")).intValue();

        if (!existingTypeId.equals(guarantorTypeId)) {
          continue;
        }

        // For CLIENT guarantor, check if same client
        // Note: Fineract API returns the client ID as 'entityId' not 'clientId'
        if (guarantorTypeId == 1 && guarantor.getClientExternalId() != null) {
          Long clientId = context.resolveEntityId("client", guarantor.getClientExternalId());
          Long existingEntityId =
              existing.get("entityId") != null
                  ? ((Number) existing.get("entityId")).longValue()
                  : null;
          if (clientId != null && clientId.equals(existingEntityId)) {
            return true;
          }
        }

        // For STAFF guarantor, check if same staff
        // Note: Fineract API returns the staff ID as 'entityId' not 'staffId'
        if (guarantorTypeId == 2 && guarantor.getStaffName() != null) {
          Long staffId = context.resolveEntityId("staff", guarantor.getStaffName());
          Long existingEntityId =
              existing.get("entityId") != null
                  ? ((Number) existing.get("entityId")).longValue()
                  : null;
          if (staffId != null && staffId.equals(existingEntityId)) {
            return true;
          }
        }

        // For EXTERNAL guarantor, check by name
        if (guarantorTypeId == 3 && guarantor.getFirstname() != null) {
          String existingFirstname = (String) existing.get("firstname");
          String existingLastname = (String) existing.get("lastname");
          if (guarantor.getFirstname().equals(existingFirstname)
              && (guarantor.getLastname() == null
                  || guarantor.getLastname().equals(existingLastname))) {
            return true;
          }
        }
      }
      return false;
    } catch (Exception ex) {
      log.debug("Error checking existing guarantors: {}", ex.getMessage());
      return false;
    }
  }

  /**
   * Builds API request for loan guarantor.
   *
   * @param guarantor guarantor
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(LoanGuarantor guarantor, ImportContext context) {
    Map<String, Object> request = new HashMap<>();
    request.put("locale", "en");

    // Map guarantor type to Fineract enum ID
    Integer guarantorTypeId = mapGuarantorType(guarantor.getGuarantorType());
    request.put("guarantorTypeId", guarantorTypeId);

    // Handle based on guarantor type
    switch (guarantorTypeId) {
      case 1 -> {
        // CLIENT guarantor
        if (guarantor.getClientExternalId() != null) {
          Long clientId = context.resolveEntityId("client", guarantor.getClientExternalId());
          if (clientId != null) {
            request.put("entityId", clientId);
          } else {
            throw new IllegalStateException(
                "Client '" + guarantor.getClientExternalId() + "' not found for guarantor");
          }
        }
      }
      case 2 -> {
        // STAFF guarantor
        if (guarantor.getStaffName() != null) {
          Long staffId = context.resolveEntityId("staff", guarantor.getStaffName());
          if (staffId != null) {
            request.put("entityId", staffId);
          } else {
            throw new IllegalStateException(
                "Staff '" + guarantor.getStaffName() + "' not found for guarantor");
          }
        }
      }
      case 3 -> {
        // EXTERNAL guarantor
        if (guarantor.getFirstname() != null) {
          request.put("firstname", guarantor.getFirstname());
        }
        if (guarantor.getLastname() != null) {
          request.put("lastname", guarantor.getLastname());
        }
        if (guarantor.getAddressLine1() != null) {
          request.put("addressLine1", guarantor.getAddressLine1());
        }
        if (guarantor.getCity() != null) {
          request.put("city", guarantor.getCity());
        }
      }
    }

    // Guaranteed amount
    if (guarantor.getAmount() != null) {
      request.put("amount", guarantor.getAmount());
    }

    // Savings account as collateral
    if (guarantor.getSavingsExternalId() != null) {
      Long savingsId = context.resolveEntityId("savingsAccount", guarantor.getSavingsExternalId());
      if (savingsId != null) {
        request.put("savingsId", savingsId);
      } else {
        log.warn("Savings account '{}' not found for guarantor", guarantor.getSavingsExternalId());
      }
    }

    return request;
  }

  /**
   * Maps guarantor type string to Fineract enum ID.
   *
   * @param guarantorType type string (CLIENT, STAFF, EXTERNAL)
   * @return Fineract enum ID
   */
  private Integer mapGuarantorType(String guarantorType) {
    if (guarantorType == null) {
      return 3; // Default to EXTERNAL
    }

    return switch (guarantorType.toUpperCase().trim()) {
      case "CLIENT" -> 1;
      case "STAFF" -> 2;
      case "EXTERNAL" -> 3;
      default -> 3;
    };
  }
}
