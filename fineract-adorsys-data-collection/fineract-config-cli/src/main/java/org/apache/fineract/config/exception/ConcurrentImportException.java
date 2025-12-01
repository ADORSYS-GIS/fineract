package org.apache.fineract.config.exception;

/**
 * Exception thrown when a concurrent import is detected.
 *
 * <p>This occurs when the state version has changed between reading and writing, indicating another
 * import process has modified the state.
 */
public class ConcurrentImportException extends RuntimeException {

  public ConcurrentImportException(String message) {
    super(message);
  }

  public ConcurrentImportException(String message, Throwable cause) {
    super(message, cause);
  }
}
