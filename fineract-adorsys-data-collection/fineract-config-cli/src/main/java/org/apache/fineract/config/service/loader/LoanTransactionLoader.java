package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.transaction.LoanTransaction;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for loan transactions.
 *
 * <p>Creates transactions on loan accounts (repayments, waivers, writeoffs).
 *
 * <p>API Endpoint: POST /api/v1/loans/{loanId}/transactions
 */
@Slf4j
@Component
public class LoanTransactionLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");

  private final FineractApiClient apiClient;

  public LoanTransactionLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads loan transactions.
   *
   * @param transactions list of loan transactions
   * @param context import context
   * @param result import result
   */
  public void load(List<LoanTransaction> transactions, ImportContext context, ImportResult result) {
    log.debug("Loading {} loan transactions", transactions.size());

    for (LoanTransaction transaction : transactions) {
      try {
        loadSingleTransaction(transaction, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load loan transaction for account '{}': {}",
            transaction.getLoanExternalId(),
            ex.getMessage());
        result.recordEntity("loanTransaction", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single loan transaction.
   *
   * @param transaction transaction to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleTransaction(
      LoanTransaction transaction, ImportContext context, ImportResult result) {
    log.debug("Loading loan transaction for account: {}", transaction.getLoanExternalId());

    // Resolve loan account
    Long loanId = context.resolveEntityId("loanAccount", transaction.getLoanExternalId());
    if (loanId == null) {
      throw new IllegalStateException(
          "Loan account '" + transaction.getLoanExternalId() + "' not found");
    }

    // Determine transaction command
    String command =
        FineractEnumMapper.mapLoanTransactionTypeToCommand(transaction.getTransactionType());

    // Check if similar transaction already exists (idempotency)
    if (transactionAlreadyExists(loanId, transaction, command)) {
      log.info(
          "Loan transaction already exists: {} {} on account {}, skipping",
          transaction.getTransactionType(),
          transaction.getTransactionAmount(),
          transaction.getLoanExternalId());
      result.recordEntity("loanTransaction", ImportResult.EntityAction.UNCHANGED);
      return;
    }

    // Build request
    Map<String, Object> request = buildRequest(transaction, context);

    // Post transaction
    String endpoint = "/api/v1/loans/" + loanId + "/transactions?command=" + command;
    Map<String, Object> response = apiClient.post(endpoint, request, Map.class);
    Long transactionId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Loan transaction created: {} on account {} (ID: {})",
        transaction.getTransactionType(),
        transaction.getLoanExternalId(),
        transactionId);
    result.recordEntity("loanTransaction", ImportResult.EntityAction.CREATED);
  }

  /**
   * Checks if a similar transaction already exists for the loan.
   *
   * @param loanId loan ID
   * @param transaction transaction to check
   * @param transactionCommand transaction command (repayment/waiveinterest/etc)
   * @return true if similar transaction already exists
   */
  @SuppressWarnings("unchecked")
  private boolean transactionAlreadyExists(
      Long loanId, LoanTransaction transaction, String transactionCommand) {
    try {
      // Get loan with transactions
      String endpoint = "/api/v1/loans/" + loanId + "?associations=transactions";
      Map<String, Object> loan = apiClient.get(endpoint, Map.class);

      List<Map<String, Object>> transactions = (List<Map<String, Object>>) loan.get("transactions");
      if (transactions == null || transactions.isEmpty()) {
        return false;
      }

      java.math.BigDecimal expectedAmount = transaction.getTransactionAmount();

      // Map command to transaction type
      String expectedType = mapCommandToTransactionType(transactionCommand);

      for (Map<String, Object> existing : transactions) {
        // Check transaction type
        Map<String, Object> typeObj = (Map<String, Object>) existing.get("type");
        if (typeObj == null) {
          continue;
        }
        String existingType = (String) typeObj.get("value");
        if (!expectedType.equalsIgnoreCase(existingType)) {
          continue;
        }

        // Check amount (for repayments)
        if (expectedAmount != null) {
          Object amountObj = existing.get("amount");
          if (amountObj == null) {
            continue;
          }
          java.math.BigDecimal existingAmount = new java.math.BigDecimal(amountObj.toString());
          if (existingAmount.compareTo(expectedAmount) != 0) {
            continue;
          }
        }

        // Check date - Fineract returns date as array [year, month, day]
        List<Integer> dateArray = (List<Integer>) existing.get("date");
        if (dateArray != null && dateArray.size() >= 3) {
          java.time.LocalDate existingDate =
              java.time.LocalDate.of(dateArray.get(0), dateArray.get(1), dateArray.get(2));
          if (existingDate.equals(transaction.getTransactionDate())) {
            return true;
          }
        }
      }
      return false;
    } catch (Exception ex) {
      log.debug("Error checking existing loan transactions: {}", ex.getMessage());
      return false;
    }
  }

  /**
   * Maps command to transaction type value for comparison.
   *
   * @param command transaction command
   * @return transaction type value
   */
  private String mapCommandToTransactionType(String command) {
    return switch (command.toLowerCase()) {
      case "repayment" -> "Repayment";
      case "waiveinterest" -> "Waive Interest";
      case "writeoff" -> "Write-Off";
      default -> command;
    };
  }

  /**
   * Builds API request for loan transaction.
   *
   * @param transaction transaction
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(LoanTransaction transaction, ImportContext context) {
    RequestBuilder builder = RequestBuilder.forTransaction();

    builder.put("transactionDate", transaction.getTransactionDate().format(DATE_FORMATTER));

    // Amount is only needed for repayments
    if ("REPAYMENT".equalsIgnoreCase(transaction.getTransactionType())
        && transaction.getTransactionAmount() != null) {
      builder.put("transactionAmount", transaction.getTransactionAmount());
    }

    // Resolve payment type
    if (transaction.getPaymentTypeName() != null) {
      Long paymentTypeId = context.resolveEntityId("paymentType", transaction.getPaymentTypeName());
      builder.putIfNotNull("paymentTypeId", paymentTypeId);
    }

    // Add note
    builder.putIfNotNull("note", transaction.getNote());

    return builder.build();
  }
}
