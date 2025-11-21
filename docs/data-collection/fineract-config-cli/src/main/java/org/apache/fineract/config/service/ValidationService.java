package org.apache.fineract.config.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.fineract.config.exception.ValidationException;
import org.apache.fineract.config.model.FineractConfig;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for validating configuration.
 *
 * <p>Performs validation checks before import including: - YAML syntax validation (done by parser)
 * - Schema validation (JSR-303 annotations) - Business rule validation
 */
@Slf4j
@Service
public class ValidationService {

  private final Validator validator;

  public ValidationService(Validator validator) {
    this.validator = validator;
  }

  /**
   * Validates configuration.
   *
   * @param config configuration to validate
   * @throws ValidationException if validation fails
   */
  public void validate(FineractConfig config) {
    log.debug("Validating configuration for tenant: {}", config.getTenant());

    // Validate using JSR-303 annotations
    Set<ConstraintViolation<FineractConfig>> violations = validator.validate(config);

    if (!violations.isEmpty()) {
      String errorMessage =
          violations.stream()
              .map(v -> v.getPropertyPath() + ": " + v.getMessage())
              .collect(Collectors.joining(", "));

      log.error("Configuration validation failed: {}", errorMessage);
      throw new ValidationException("Configuration validation failed: " + errorMessage);
    }

    // Additional business rule validation
    validateBusinessRules(config);

    log.info("Configuration validation successful");
  }

  /**
   * Validates business rules.
   *
   * @param config configuration to validate
   */
  private void validateBusinessRules(FineractConfig config) {
    // Example: Check if currency is configured when system config is present
    if (config.getSystemConfig() != null && config.getSystemConfig().getCurrency() == null) {
      log.warn("System config present but currency not configured");
    }

    // Add more business rule validations as needed
  }
}
