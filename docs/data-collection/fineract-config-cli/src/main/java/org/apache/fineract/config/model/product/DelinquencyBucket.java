package org.apache.fineract.config.model.product;

import lombok.Data;

/** Delinquency bucket configuration. */
@Data
public class DelinquencyBucket {
  private String name;
  private String classification;
}
