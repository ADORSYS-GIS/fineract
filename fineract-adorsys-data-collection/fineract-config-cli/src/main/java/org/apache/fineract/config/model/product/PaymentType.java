package org.apache.fineract.config.model.product;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment Type.
 *
 * <p>Defines payment methods available in the system (Cash, Check, Mobile Money, etc.).
 */
@Slf4j
@Data
public class PaymentType {
  private String name;
  private String description;
  private Boolean isCashPayment;
  private Integer position;

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
