package org.apache.fineract.config.model.client;

import java.time.LocalDate;

import lombok.Data;

/**
 * Client definition.
 *
 * <p>Represents an individual client (borrower/saver) in the system.
 */
@Data
public class Client {
  private String externalId;
  private String officeName;
  private String firstName;
  private String middleName;
  private String lastName;
  private String fullname; // For entity clients (legalFormId=2)
  private LocalDate dateOfBirth;
  private String gender; // MALE, FEMALE, OTHER
  private String mobileNo;
  private String emailAddress;
  private Boolean active;
  private LocalDate activationDate;
  private String staffName;

  // Legal form - required by Fineract API
  // 1 = Person (Individual), 2 = Entity (Corporate/Business)
  private Integer legalFormId;

  // Client classification
  private Long clientTypeId;
  private Long clientClassificationId;
  private String clientType; // For YAML convenience (will need resolution to ID)
  private String clientClassification; // For YAML convenience (will need resolution to ID)

  // Address
  private String addressLine1;
  private String addressLine2;
  private String city;
  private String stateProvince;
  private String countryCode;
  private String postalCode;

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @com.fasterxml.jackson.annotation.JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    org.slf4j.LoggerFactory.getLogger(this.getClass())
        .warn(
            "Unknown field '{}' with value '{}' in {} (will be ignored). "
                + "This may indicate a missing field in the model class.",
            key,
            value,
            this.getClass().getSimpleName());
  }
}
