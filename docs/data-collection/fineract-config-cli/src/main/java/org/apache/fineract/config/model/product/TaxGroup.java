package org.apache.fineract.config.model.product;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/** Tax group configuration. */
@Data
public class TaxGroup {
  private String name;
  private List<TaxComponent> taxComponents = new ArrayList<>();
}
