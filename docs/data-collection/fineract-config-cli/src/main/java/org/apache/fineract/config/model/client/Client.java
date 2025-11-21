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
  private LocalDate dateOfBirth;
  private String gender; // MALE, FEMALE, OTHER
  private String mobileNo;
  private String emailAddress;
  private Boolean active;
  private LocalDate activationDate;
  private String staffName;

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
}
