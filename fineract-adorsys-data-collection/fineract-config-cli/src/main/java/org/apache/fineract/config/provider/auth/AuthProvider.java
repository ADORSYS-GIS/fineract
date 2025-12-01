package org.apache.fineract.config.provider.auth;

import org.springframework.http.HttpHeaders;

/**
 * Interface for Fineract authentication providers.
 *
 * <p>Implementations provide authentication headers for different authentication methods (Basic
 * Auth, OAuth2, etc.)
 */
public interface AuthProvider {

  /**
   * Configures authentication headers for HTTP requests.
   *
   * @param headers HTTP headers to configure
   */
  void configureHeaders(HttpHeaders headers);

  /**
   * Gets authentication token (for OAuth2).
   *
   * @return authentication token, or null for Basic Auth
   */
  default String getToken() {
    return null;
  }

  /**
   * Checks if the provider is ready to authenticate.
   *
   * @return true if ready, false otherwise
   */
  default boolean isReady() {
    return true;
  }
}
