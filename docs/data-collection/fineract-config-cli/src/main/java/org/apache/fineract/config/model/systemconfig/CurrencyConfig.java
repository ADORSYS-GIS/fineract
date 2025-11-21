package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Currency configuration model.
 *
 * <p>Defines the currency used in the Fineract tenant.
 *
 * <p>Example YAML:
 *
 * <pre>
 * currency:
 *   code: USD
 *   name: US Dollar
 *   decimalPlaces: 2
 * </pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyConfig {

  /** ISO 4217 currency code (required). Example: USD, EUR, XAF */
  @NotBlank(message = "Currency code is required")
  private String code;

  /** Currency full name (required). Example: US Dollar */
  @NotBlank(message = "Currency name is required")
  private String name;

  /** Number of decimal places (required). Example: 2 for USD, 0 for XAF */
  @NotNull(message = "Decimal places is required")
  private Integer decimalPlaces;

  /** Display symbol (optional). Example: $ */
  private String displaySymbol;

  /** Name code (optional). Example: currency.USD */
  private String nameCode;
}
