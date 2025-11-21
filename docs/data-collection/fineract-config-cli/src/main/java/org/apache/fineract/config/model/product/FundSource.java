package org.apache.fineract.config.model.product;

import lombok.Data;

/**
 * Fund Source.
 *
 * <p>Represents a source of funds for loan products.
 */
@Data
public class FundSource {
  private String name;
  private String externalId;
}
