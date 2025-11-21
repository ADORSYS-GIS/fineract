package org.apache.fineract.config.model.product;

import lombok.Data;

/** Floating interest rate configuration. */
@Data
public class FloatingRate {
  private String name;
  private Boolean isActive;
}
