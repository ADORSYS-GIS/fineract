package org.apache.fineract.config.model.product;

import lombok.Data;

/**
 * Payment Type.
 *
 * <p>Defines payment methods available in the system (Cash, Check, Mobile Money, etc.).
 */
@Data
public class PaymentType {
  private String name;
  private String description;
  private Boolean isCashPayment;
  private Integer position;
}
