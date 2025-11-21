package org.apache.fineract.config.model.systemconfig;

import java.util.List;

import lombok.Data;

/** Data table column definition. */
@Data
public class DataTableColumn {
  private String name;
  private String type; // String, Number, Decimal, Date, Dropdown, etc.
  private Integer length;
  private Boolean mandatory;
  private String code; // For dropdown columns, reference to a code
  private List<String> values; // For dropdown columns, list of values
}
