package org.apache.fineract.config.model.product;

import lombok.Data;

/** Collateral type configuration. */
@Data
public class CollateralType {
  private String name;
  private String description;
  private Boolean requiresValuation;
}
