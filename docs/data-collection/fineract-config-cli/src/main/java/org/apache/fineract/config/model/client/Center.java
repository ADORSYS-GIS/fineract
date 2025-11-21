package org.apache.fineract.config.model.client;

import java.time.LocalDate;

import lombok.Data;

/**
 * Center definition.
 *
 * <p>Represents a center (collection of groups) in the system.
 */
@Data
public class Center {
  private String name;
  private String externalId;
  private String officeName;
  private String staffName;
  private Boolean active;
  private LocalDate activationDate;
  private LocalDate submittedOnDate;
}
