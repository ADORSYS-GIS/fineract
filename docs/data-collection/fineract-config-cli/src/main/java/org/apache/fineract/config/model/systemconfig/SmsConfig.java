package org.apache.fineract.config.model.systemconfig;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class SmsConfig {
  private String provider;
  private Map<String, Object> config = new HashMap<>();
}
