package org.apache.fineract.config.model.client;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Group definition.
 *
 * <p>Represents a group of clients (e.g., self-help group, solidarity group).
 */
@Data
public class Group {
  private String name;
  private String externalId;
  private String officeName;
  private String staffName;
  private Boolean active;
  private LocalDate activationDate;
  private LocalDate submittedOnDate;

  // Group members (client external IDs)
  private List<String> clientExternalIds = new ArrayList<>();

  // Link to center (optional)
  private String centerName;
}
