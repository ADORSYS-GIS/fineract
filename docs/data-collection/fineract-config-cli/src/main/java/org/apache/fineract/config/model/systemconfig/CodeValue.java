package org.apache.fineract.config.model.systemconfig;

import lombok.Data;

/** Code value (option for a code). */
@Data
public class CodeValue {
  private String name;
  private Integer position;
  private String description;
  private Boolean isActive;
}
