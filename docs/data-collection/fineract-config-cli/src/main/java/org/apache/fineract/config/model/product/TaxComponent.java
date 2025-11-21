package org.apache.fineract.config.model.product;

import java.time.LocalDate;

import lombok.Data;

/** Tax component configuration. */
@Data
public class TaxComponent {
  private String name;
  private LocalDate startDate;
  private String creditAccountType;
}
