package org.apache.fineract.config.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Exception thrown when Fineract API calls fail.
 *
 * <p>Captures HTTP status code, error message, and response headers for detailed error reporting.
 */
@Getter
public class FineractApiException extends RuntimeException {

  private final HttpStatus status;
  private final String responseBody;
  private final HttpHeaders headers;

  public FineractApiException(HttpStatus status, String responseBody, HttpHeaders headers) {
    super(String.format("Fineract API error: %s - %s", status, responseBody));
    this.status = status;
    this.responseBody = responseBody;
    this.headers = headers;
  }

  public FineractApiException(HttpStatus status, String message) {
    this(status, message, new HttpHeaders());
  }

  /**
   * Checks if this error is retryable (5xx errors).
   *
   * @return true if retryable, false otherwise
   */
  public boolean isRetryable() {
    return status.is5xxServerError();
  }
}
