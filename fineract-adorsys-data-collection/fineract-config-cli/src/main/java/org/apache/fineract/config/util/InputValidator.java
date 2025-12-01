package org.apache.fineract.config.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility for validating input data.
 *
 * <p>Provides comprehensive validation for entity identifiers, strings, and other input data to
 * prevent injection attacks and data corruption.
 *
 * <p>Validation rules:
 *
 * <ul>
 *   <li>Entity identifiers: alphanumeric, spaces, hyphens, underscores, dots (max 255 chars)
 *   <li>Email addresses: RFC 5322 compliant format
 *   <li>Phone numbers: digits, spaces, hyphens, plus, parentheses (max 50 chars)
 *   <li>External IDs: alphanumeric, hyphens, underscores (max 100 chars)
 *   <li>Names: letters, spaces, hyphens, apostrophes, dots (max 255 chars)
 * </ul>
 */
@Slf4j
@Component
public class InputValidator {

  // Patterns for validation
  private static final Pattern ENTITY_IDENTIFIER_PATTERN =
      Pattern.compile("^[a-zA-Z0-9\\s._-]{1,255}$");

  private static final Pattern EXTERNAL_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,100}$");

  private static final Pattern NAME_PATTERN =
      Pattern.compile("^[a-zA-Z0-9\\s.'\\-]{1,255}$", Pattern.UNICODE_CHARACTER_CLASS);

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile(
          "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

  private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9\\s()+-]{1,50}$");

  private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^.{0,1000}$", Pattern.DOTALL);

  /**
   * Validates an entity identifier (office name, role name, etc.).
   *
   * @param identifier identifier to validate
   * @param fieldName field name for error messages
   * @return validation result
   */
  public ValidationResult validateEntityIdentifier(String identifier, String fieldName) {
    if (identifier == null || identifier.trim().isEmpty()) {
      return ValidationResult.invalid(fieldName + " cannot be null or empty");
    }

    if (!ENTITY_IDENTIFIER_PATTERN.matcher(identifier).matches()) {
      return ValidationResult.invalid(
          fieldName
              + " contains invalid characters. Allowed: letters, digits, spaces, dots, hyphens,"
              + " underscores (max 255 chars)");
    }

    return ValidationResult.valid();
  }

  /**
   * Validates an external ID.
   *
   * @param externalId external ID to validate
   * @param fieldName field name for error messages
   * @return validation result
   */
  public ValidationResult validateExternalId(String externalId, String fieldName) {
    if (externalId == null || externalId.trim().isEmpty()) {
      return ValidationResult.valid(); // External ID is optional
    }

    if (!EXTERNAL_ID_PATTERN.matcher(externalId).matches()) {
      return ValidationResult.invalid(
          fieldName
              + " contains invalid characters. Allowed: letters, digits, hyphens, underscores (max"
              + " 100 chars)");
    }

    return ValidationResult.valid();
  }

  /**
   * Validates a person name (first name, last name).
   *
   * @param name name to validate
   * @param fieldName field name for error messages
   * @return validation result
   */
  public ValidationResult validateName(String name, String fieldName) {
    if (name == null || name.trim().isEmpty()) {
      return ValidationResult.invalid(fieldName + " cannot be null or empty");
    }

    if (!NAME_PATTERN.matcher(name).matches()) {
      return ValidationResult.invalid(
          fieldName
              + " contains invalid characters. Allowed: letters, digits, spaces, dots, hyphens,"
              + " apostrophes (max 255 chars)");
    }

    return ValidationResult.valid();
  }

  /**
   * Validates an email address.
   *
   * @param email email to validate
   * @param fieldName field name for error messages
   * @return validation result
   */
  public ValidationResult validateEmail(String email, String fieldName) {
    if (email == null || email.trim().isEmpty()) {
      return ValidationResult.valid(); // Email is optional
    }

    if (!EMAIL_PATTERN.matcher(email).matches()) {
      return ValidationResult.invalid(fieldName + " is not a valid email address");
    }

    return ValidationResult.valid();
  }

  /**
   * Validates a phone number.
   *
   * @param phone phone number to validate
   * @param fieldName field name for error messages
   * @return validation result
   */
  public ValidationResult validatePhone(String phone, String fieldName) {
    if (phone == null || phone.trim().isEmpty()) {
      return ValidationResult.valid(); // Phone is optional
    }

    if (!PHONE_PATTERN.matcher(phone).matches()) {
      return ValidationResult.invalid(
          fieldName
              + " contains invalid characters. Allowed: digits, spaces, hyphens, plus,"
              + " parentheses (max 50 chars)");
    }

    return ValidationResult.valid();
  }

  /**
   * Validates a description field.
   *
   * @param description description to validate
   * @param fieldName field name for error messages
   * @return validation result
   */
  public ValidationResult validateDescription(String description, String fieldName) {
    if (description == null || description.trim().isEmpty()) {
      return ValidationResult.valid(); // Description is optional
    }

    if (!DESCRIPTION_PATTERN.matcher(description).matches()) {
      return ValidationResult.invalid(fieldName + " exceeds maximum length of 1000 characters");
    }

    return ValidationResult.valid();
  }

  /**
   * Validates a string does not contain SQL injection patterns.
   *
   * @param value value to validate
   * @param fieldName field name for error messages
   * @return validation result
   */
  public ValidationResult validateNoSqlInjection(String value, String fieldName) {
    if (value == null) {
      return ValidationResult.valid();
    }

    // Check for common SQL injection patterns
    String lowerValue = value.toLowerCase();
    String[] sqlKeywords = {
      "--",
      "/*",
      "*/",
      ";",
      "union",
      "select",
      "insert",
      "update",
      "delete",
      "drop",
      "alter",
      "create",
      "exec",
      "execute",
      "script",
      "javascript",
      "onerror",
      "onload"
    };

    for (String keyword : sqlKeywords) {
      if (lowerValue.contains(keyword)) {
        return ValidationResult.invalid(
            fieldName + " contains potentially unsafe content: '" + keyword + "'");
      }
    }

    return ValidationResult.valid();
  }

  /**
   * Validates multiple fields and aggregates errors.
   *
   * @param results list of validation results
   * @return aggregated validation result
   */
  public ValidationResult validateAll(List<ValidationResult> results) {
    List<String> errors = new ArrayList<>();

    for (ValidationResult result : results) {
      if (!result.isValid()) {
        errors.addAll(result.getErrors());
      }
    }

    if (errors.isEmpty()) {
      return ValidationResult.valid();
    }

    return ValidationResult.invalid(errors);
  }

  /**
   * Validation result.
   *
   * <p>Contains validation status and error messages.
   */
  public static class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
      this.valid = valid;
      this.errors = errors != null ? errors : new ArrayList<>();
    }

    public static ValidationResult valid() {
      return new ValidationResult(true, new ArrayList<>());
    }

    public static ValidationResult invalid(String error) {
      List<String> errors = new ArrayList<>();
      errors.add(error);
      return new ValidationResult(false, errors);
    }

    public static ValidationResult invalid(List<String> errors) {
      return new ValidationResult(false, errors);
    }

    public boolean isValid() {
      return valid;
    }

    public List<String> getErrors() {
      return errors;
    }

    public String getErrorMessage() {
      return String.join("; ", errors);
    }

    /**
     * Throws IllegalArgumentException if validation failed.
     *
     * @throws IllegalArgumentException if validation failed
     */
    public void throwIfInvalid() {
      if (!valid) {
        throw new IllegalArgumentException("Validation failed: " + getErrorMessage());
      }
    }
  }
}
