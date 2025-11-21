package org.apache.fineract.config.exception;

/**
 * Exception thrown when validation fails.
 *
 * <p>Used for YAML syntax errors, schema validation failures, or dependency validation issues.
 */
public class ValidationException extends RuntimeException {

  public ValidationException(String message) {
    super(message);
  }

  public ValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
