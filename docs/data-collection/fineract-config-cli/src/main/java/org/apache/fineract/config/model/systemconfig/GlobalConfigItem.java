package org.apache.fineract.config.model.systemconfig;

import java.time.LocalDate;

import lombok.Data;

/** Global configuration item. */
@Data
public class GlobalConfigItem {
  private String name;
  private boolean enabled;
  private Long value;
  private String stringValue;
  private LocalDate dateValue;
}
