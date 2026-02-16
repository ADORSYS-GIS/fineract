package org.apache.fineract.config.model.client;

import lombok.Data;

@Data
public class Address {
  private String addressLine1;
  private String addressLine2;
  private String city;
  private String stateProvince;
  private String country;
  private String postalCode;
  private Long addressTypeId;
  private String addressType; // For YAML lookup (e.g., "Residential")
}
