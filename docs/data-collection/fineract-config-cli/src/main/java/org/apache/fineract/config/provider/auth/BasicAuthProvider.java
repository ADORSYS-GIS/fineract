package org.apache.fineract.config.provider.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.fineract.config.properties.FineractProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Basic Authentication provider for Fineract API.
 *
 * <p>Provides HTTP Basic Authentication using username and password.
 *
 * <p>Enabled when: fineract.auth.type=basic (default)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "fineract.auth.type", havingValue = "basic", matchIfMissing = true)
public class BasicAuthProvider implements AuthProvider {

  private final FineractProperties fineractProperties;
  private final String basicAuthHeader;

  public BasicAuthProvider(FineractProperties fineractProperties) {
    this.fineractProperties = fineractProperties;

    // Pre-calculate Basic Auth header
    String credentials = fineractProperties.getUser() + ":" + fineractProperties.getPassword();
    String encodedCredentials =
        Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    this.basicAuthHeader = "Basic " + encodedCredentials;

    log.debug("BasicAuthProvider initialized for user: {}", fineractProperties.getUser());
  }

  @Override
  public void configureHeaders(HttpHeaders headers) {
    headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader);
    headers.set("Fineract-Platform-TenantId", fineractProperties.getTenant());
    headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
    headers.set(HttpHeaders.ACCEPT, "application/json");

    log.trace("Configured Basic Auth headers for tenant: {}", fineractProperties.getTenant());
  }

  @Override
  public boolean isReady() {
    boolean ready =
        fineractProperties.getUser() != null && fineractProperties.getPassword() != null;

    if (!ready) {
      log.warn("BasicAuthProvider not ready: username or password is null");
    }

    return ready;
  }
}
