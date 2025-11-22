package org.apache.fineract.config.provider;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.fineract.config.properties.FineractProperties;
import org.apache.fineract.config.provider.auth.AuthProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 authentication provider.
 *
 * <p>Obtains OAuth2 access tokens from Keycloak or other OAuth2 providers.
 *
 * <p>Enabled when: fineract.auth.type=oauth2
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "fineract.auth.type", havingValue = "oauth2")
public class OAuth2Provider implements AuthProvider {

  private final FineractProperties fineractProperties;
  private final WebClient webClient;

  private String accessToken;
  private Instant tokenExpiry;

  public OAuth2Provider(FineractProperties fineractProperties) {
    this.fineractProperties = fineractProperties;
    this.webClient = createWebClient();
  }

  @Override
  public void configureHeaders(HttpHeaders headers) {
    String token = getToken();
    if (token != null) {
      headers.setBearerAuth(token);
    }
  }

  @Override
  public String getToken() {
    // Check if token is expired or not yet obtained
    if (accessToken == null || isTokenExpired()) {
      refreshToken();
    }
    return accessToken;
  }

  @Override
  public boolean isReady() {
    try {
      return getToken() != null;
    } catch (Exception ex) {
      log.error("OAuth2 authentication not ready: {}", ex.getMessage());
      return false;
    }
  }

  /** Refreshes the OAuth2 access token. */
  private void refreshToken() {
    log.debug("Obtaining OAuth2 access token");

    try {
      FineractProperties.OAuth2Properties oauth2 = fineractProperties.getAuth().getOauth2();

      if (oauth2 == null) {
        throw new IllegalStateException("OAuth2 configuration not found");
      }

      // Log OAuth2 config for debugging
      log.info("OAuth2 Config - grant_type: {}, client_id: {}, token_url: {}",
          oauth2.getGrantType(), oauth2.getClientId(), oauth2.getTokenUrl());

      // Build token request
      Map<String, Object> request = new HashMap<>();
      request.put("grant_type", oauth2.getGrantType());
      request.put("client_id", oauth2.getClientId());

      if (oauth2.getClientSecret() != null) {
        request.put("client_secret", oauth2.getClientSecret());
      }

      if ("password".equals(oauth2.getGrantType())) {
        request.put("username", fineractProperties.getUser());
        request.put("password", fineractProperties.getPassword());
      }

      if (oauth2.getScope() != null) {
        request.put("scope", oauth2.getScope());
      }

      // Request token
      Map<String, Object> response =
          webClient
              .post()
              .uri(oauth2.getTokenUrl())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .bodyValue(buildFormData(request))
              .retrieve()
              .bodyToMono(Map.class)
              .block();

      if (response == null) {
        throw new IllegalStateException("OAuth2 token response is null");
      }

      // Extract token and expiry
      accessToken = (String) response.get("access_token");
      Integer expiresIn = (Integer) response.get("expires_in");

      if (expiresIn != null) {
        // Set expiry 30 seconds before actual expiry for safety
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 30);
      } else {
        // Default to 5 minutes if not specified
        tokenExpiry = Instant.now().plusSeconds(300);
      }

      log.info("OAuth2 access token obtained, expires at {}", tokenExpiry);

    } catch (Exception ex) {
      log.error("Failed to obtain OAuth2 access token: {}", ex.getMessage(), ex);
      throw new IllegalStateException("OAuth2 authentication failed", ex);
    }
  }

  /**
   * Checks if token is expired.
   *
   * @return true if expired
   */
  private boolean isTokenExpired() {
    return tokenExpiry == null || Instant.now().isAfter(tokenExpiry);
  }

  /**
   * Builds form data for token request.
   *
   * @param params parameters
   * @return form data string
   */
  private String buildFormData(Map<String, Object> params) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      if (sb.length() > 0) {
        sb.append("&");
      }
      sb.append(entry.getKey()).append("=").append(entry.getValue());
    }
    return sb.toString();
  }

  /**
   * Creates WebClient for OAuth2 token requests.
   *
   * @return WebClient
   */
  private WebClient createWebClient() {
    return WebClient.builder()
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .build();
  }
}
