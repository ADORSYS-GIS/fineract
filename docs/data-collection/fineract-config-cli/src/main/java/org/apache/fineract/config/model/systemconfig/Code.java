package org.apache.fineract.config.model.systemconfig;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/** Code (category) with code values. */
@Data
public class Code {
  private String name;
  private List<CodeValue> values = new ArrayList<>();
}
