package org.apache.fineract.config.provider;

import java.time.Duration;

import org.apache.fineract.config.exception.FineractApiException;
import org.apache.fineract.config.properties.FineractProperties;
import org.apache.fineract.config.provider.auth.AuthProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * HTTP client for Fineract REST API.
 *
 * <p>Provides a simplified interface for making HTTP requests to Fineract with automatic
 * authentication, error handling, and SSL configuration.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Automatic authentication (Basic Auth or OAuth2)
 *   <li>SSL verification toggle
 *   <li>Configurable timeouts
 *   <li>Comprehensive error handling
 *   <li>Request/response logging
 * </ul>
 */
@Slf4j
@Component
public class FineractApiClient {

  private final WebClient webClient;
  private final FineractProperties fineractProperties;
  private final AuthProvider authProvider;

  public FineractApiClient(FineractProperties fineractProperties, AuthProvider authProvider) {
    this.fineractProperties = fineractProperties;
    this.authProvider = authProvider;
    this.webClient = createWebClient();

    log.info(
        "FineractApiClient initialized: url={}, tenant={}, sslVerify={}",
        fineractProperties.getUrl(),
        fineractProperties.getTenant(),
        fineractProperties.isSslVerify());
  }

  /**
   * Performs HTTP GET request.
   *
   * @param path API path (relative to base URL)
   * @param responseType response class type
   * @param <T> response type
   * @return response object
   * @throws FineractApiException if request fails
   */
  public <T> T get(String path, Class<T> responseType) {
    log.debug("GET {}", path);

    // Calculate block timeout (readTimeout + 5 seconds buffer)
    Duration blockTimeout = Duration.ofSeconds(fineractProperties.getReadTimeout() + 5);

    return webClient
        .get()
        .uri(path)
        .headers(authProvider::configureHeaders)
        .retrieve()
        .onStatus(status -> status.isError(), this::handleErrorResponse)
        .bodyToMono(responseType)
        .doOnSuccess(response -> log.debug("GET {} -> SUCCESS", path))
        .doOnError(error -> log.error("GET {} -> ERROR: {}", path, error.getMessage()))
        .block(blockTimeout);
  }

  /**
   * Performs HTTP POST request.
   *
   * @param path API path (relative to base URL)
   * @param body request body
   * @param responseType response class type
   * @param <T> request body type
   * @param <R> response type
   * @return response object
   * @throws FineractApiException if request fails
   */
  public <T, R> R post(String path, T body, Class<R> responseType) {
    log.debug("POST {} with body: {}", path, body);

    // Calculate block timeout (readTimeout + 5 seconds buffer)
    Duration blockTimeout = Duration.ofSeconds(fineractProperties.getReadTimeout() + 5);

    return webClient
        .post()
        .uri(path)
        .headers(authProvider::configureHeaders)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .onStatus(status -> status.isError(), this::handleErrorResponse)
        .bodyToMono(responseType)
        .doOnSuccess(response -> log.debug("POST {} -> SUCCESS: {}", path, response))
        .doOnError(error -> log.error("POST {} -> ERROR: {}", path, error.getMessage()))
        .block(blockTimeout);
  }

  /**
   * Performs HTTP PUT request.
   *
   * @param path API path (relative to base URL)
   * @param body request body
   * @param responseType response class type
   * @param <T> request body type
   * @param <R> response type
   * @return response object
   * @throws FineractApiException if request fails
   */
  public <T, R> R put(String path, T body, Class<R> responseType) {
    log.debug("PUT {} with body: {}", path, body);

    // Calculate block timeout (readTimeout + 5 seconds buffer)
    Duration blockTimeout = Duration.ofSeconds(fineractProperties.getReadTimeout() + 5);

    return webClient
        .put()
        .uri(path)
        .headers(authProvider::configureHeaders)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .onStatus(status -> status.isError(), this::handleErrorResponse)
        .bodyToMono(responseType)
        .doOnSuccess(response -> log.debug("PUT {} -> SUCCESS: {}", path, response))
        .doOnError(error -> log.error("PUT {} -> ERROR: {}", path, error.getMessage()))
        .block(blockTimeout);
  }

  /**
   * Performs HTTP DELETE request.
   *
   * @param path API path (relative to base URL)
   * @param responseType response class type
   * @param <T> response type
   * @return response object
   * @throws FineractApiException if request fails
   */
  public <T> T delete(String path, Class<T> responseType) {
    log.debug("DELETE {}", path);

    // Calculate block timeout (readTimeout + 5 seconds buffer)
    Duration blockTimeout = Duration.ofSeconds(fineractProperties.getReadTimeout() + 5);

    return webClient
        .delete()
        .uri(path)
        .headers(authProvider::configureHeaders)
        .retrieve()
        .onStatus(status -> status.isError(), this::handleErrorResponse)
        .bodyToMono(responseType)
        .doOnSuccess(response -> log.debug("DELETE {} -> SUCCESS", path))
        .doOnError(error -> log.error("DELETE {} -> ERROR: {}", path, error.getMessage()))
        .block(blockTimeout);
  }

  /**
   * Checks if Fineract is reachable and authentication is working.
   *
   * @return true if connection successful, false otherwise
   */
  public boolean testConnection() {
    try {
      log.info("Testing connection to Fineract...");
      // Try to fetch currencies (lightweight endpoint)
      get("/api/v1/currencies", Object.class);
      log.info("Connection test successful");
      return true;
    } catch (Exception ex) {
      log.error("Connection test failed: {}", ex.getMessage());
      return false;
    }
  }

  /**
   * Creates WebClient with SSL and timeout configuration.
   *
   * @return configured WebClient
   */
  private WebClient createWebClient() {
    try {
      // Configure SSL
      SslContext sslContext =
          fineractProperties.isSslVerify()
              ? SslContextBuilder.forClient().build()
              : SslContextBuilder.forClient()
                  .trustManager(InsecureTrustManagerFactory.INSTANCE)
                  .build();

      // Configure HTTP client
      HttpClient httpClient =
          HttpClient.create()
              .secure(spec -> spec.sslContext(sslContext))
              .responseTimeout(Duration.ofSeconds(fineractProperties.getReadTimeout()));

      // Create WebClient
      return WebClient.builder()
          .baseUrl(fineractProperties.getUrl())
          .clientConnector(new ReactorClientHttpConnector(httpClient))
          .build();

    } catch (Exception ex) {
      throw new IllegalStateException("Failed to create WebClient", ex);
    }
  }

  /**
   * Handles error responses from Fineract API.
   *
   * @param response error response
   * @return Mono error
   */
  private Mono<Throwable> handleErrorResponse(ClientResponse response) {
    return response
        .bodyToMono(String.class)
        .flatMap(
            errorBody -> {
              log.error("Fineract API error: status={}, body={}", response.statusCode(), errorBody);

              return Mono.error(
                  new FineractApiException(
                      HttpStatus.valueOf(response.statusCode().value()),
                      errorBody,
                      response.headers().asHttpHeaders()));
            });
  }
}
