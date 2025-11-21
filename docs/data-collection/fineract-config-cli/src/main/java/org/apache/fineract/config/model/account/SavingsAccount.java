package org.apache.fineract.config.model.account;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.fineract.config.util.DateArrayDeserializer;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Savings Account definition.
 *
 * <p>Represents a savings account for a client or group.
 *
 * <p>Lifecycle:
 *
 * <ul>
 *   <li>Created with status PENDING_APPROVAL on submittedOnDate
 *   <li>Approved and activated on activationDate
 * </ul>
 *
 * <p>YAML Example:
 *
 * <pre>
 * savingsAccounts:
 *   - externalId: SAV-001
 *     productName: Regular Savings
 *     clientExternalId: CLI-001
 *     submittedOnDate: [2024, 1, 10]
 *     activatedOnDate: [2024, 1, 15]
 *     active: true
 * </pre>
 */
@Slf4j
@Data
public class SavingsAccount {
  /** External identifier for the savings account. */
  private String externalId;

  /** For individual accounts: external ID of the client. */
  private String clientExternalId;

  /** For group accounts: name of the group. */
  private String groupName;

  /** Name of the savings product. */
  private String productName;

  /** Name of the field officer assigned to this account. */
  private String fieldOfficerName;

  /**
   * Date when the savings account was submitted for approval.
   *
   * <p>YAML Format: [year, month, day] e.g. [2024, 1, 10]
   *
   * <p>This date is immutable after account creation.
   */
  @JsonProperty("submittedOnDate")
  @JsonDeserialize(using = DateArrayDeserializer.class)
  private LocalDate submittedOnDate;

  /** Optional: Override the product's nominal annual interest rate for this account. */
  private BigDecimal nominalAnnualInterestRate;

  /** Whether the account is active (approved and activated). */
  private Boolean active;

  /**
   * Date when the savings account was activated.
   *
   * <p>YAML Format: [year, month, day] e.g. [2024, 1, 15]
   *
   * <p>The account will be approved and activated on this date. This date is immutable after
   * account creation.
   */
  @JsonProperty("activatedOnDate")
  @JsonDeserialize(using = DateArrayDeserializer.class)
  private LocalDate activationDate;

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    log.warn(
        "Unknown field '{}' with value '{}' in {} (will be ignored). "
            + "This may indicate a missing field in the model class.",
        key,
        value,
        this.getClass().getSimpleName());
  }
}
