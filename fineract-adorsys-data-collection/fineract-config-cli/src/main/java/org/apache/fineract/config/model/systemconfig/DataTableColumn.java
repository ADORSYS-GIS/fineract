package org.apache.fineract.config.model.systemconfig;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Data table column definition. */
@Slf4j
@Data
public class DataTableColumn {
  private String name;
  private String type; // String, Number, Decimal, Date, Dropdown, etc.
  private Integer length;
  private Boolean mandatory;
  private String code; // For dropdown columns, reference to a code
  private List<String> values; // For dropdown columns, list of values

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    log.warn(
        "Unknown field '{}' with value '{}' in {} (will be ignored). "
            + "This may indicate a missing field in the model class.",
        key,
        value,
        this.getClass().getSimpleName());
  }
}
