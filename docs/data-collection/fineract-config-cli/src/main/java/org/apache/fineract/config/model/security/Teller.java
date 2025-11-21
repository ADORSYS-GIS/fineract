package org.apache.fineract.config.model.security;

import java.time.LocalDate;

import lombok.Data;

/** Teller configuration. */
@Data
public class Teller {
  private String name;
  private String officeName;
  private String description;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;
}
