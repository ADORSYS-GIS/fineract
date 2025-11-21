package org.apache.fineract.config.model.security;

import java.time.LocalDate;

import lombok.Data;

/**
 * Office (branch/location).
 *
 * <p>Represents an organizational unit such as head office, branch, or regional office.
 */
@Data
public class Office {
  private String name;
  private String externalId;
  private LocalDate openingDate;
  private String parentName; // Reference to parent office
  private String hierarchy; // Optional: explicit hierarchy path
}
