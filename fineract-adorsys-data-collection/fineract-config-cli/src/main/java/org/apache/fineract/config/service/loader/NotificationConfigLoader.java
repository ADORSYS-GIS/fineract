package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.NotificationConfig;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for notification (SMS/Email) configuration.
 *
 * <p>Configures SMS and email gateway settings.
 *
 * <p>API Endpoints: - PUT /api/v1/smsgateway - PUT /api/v1/emailconfiguration
 */
@Slf4j
@Component
public class NotificationConfigLoader {

  private final FineractApiClient apiClient;

  public NotificationConfigLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads notification configuration.
   *
   * @param notificationConfig notification configuration
   * @param context import context
   * @param result import result
   */
  public void load(
      NotificationConfig notificationConfig, ImportContext context, ImportResult result) {
    log.debug("Loading notification configuration");

    try {
      // Load SMS configuration if present
      if (notificationConfig.getSms() != null) {
        loadSmsConfig(notificationConfig.getSms(), result);
      }

      // Load email configuration if present
      if (notificationConfig.getEmail() != null) {
        loadEmailConfig(notificationConfig.getEmail(), result);
      }

      log.info("Notification configuration loaded successfully");

    } catch (Exception ex) {
      log.error("Failed to load notification configuration: {}", ex.getMessage(), ex);
      result.recordEntity("notificationConfig", ImportResult.EntityAction.FAILED);
      throw ex;
    }
  }

  /**
   * Loads SMS gateway configuration.
   *
   * @param smsConfig SMS configuration
   * @param result import result
   */
  private void loadSmsConfig(NotificationConfig.SmsConfig smsConfig, ImportResult result) {
    log.debug("Loading SMS gateway configuration");

    Map<String, Object> request = new HashMap<>();
    request.put("providerId", smsConfig.getProviderId());
    request.put("hostName", smsConfig.getHostName());
    request.put("port", smsConfig.getPort());
    request.put("accountSId", smsConfig.getAccountSId());
    request.put("authToken", smsConfig.getAuthToken());

    apiClient.put("/api/v1/smscampaigns/1", request, Map.class);

    log.info("SMS gateway configured: {} ({})", smsConfig.getProviderId(), smsConfig.getHostName());
    result.recordEntity("smsConfig", ImportResult.EntityAction.UPDATED);
  }

  /**
   * Loads email configuration.
   *
   * @param emailConfig email configuration
   * @param result import result
   */
  private void loadEmailConfig(NotificationConfig.EmailConfig emailConfig, ImportResult result) {
    log.debug("Loading email configuration");

    Map<String, Object> request = new HashMap<>();
    request.put("smtpServer", emailConfig.getSmtpServer());
    request.put("smtpPort", emailConfig.getSmtpPort());
    request.put("smtpUsername", emailConfig.getSmtpUsername());
    request.put("smtpPassword", emailConfig.getSmtpPassword());
    request.put("fromEmail", emailConfig.getFromEmail());
    request.put("fromName", emailConfig.getFromName());

    if (emailConfig.getUseTLS() != null) {
      request.put("useTLS", emailConfig.getUseTLS());
    }

    apiClient.put("/api/v1/emailconfiguration", request, Map.class);

    log.info("Email configured: {} ({})", emailConfig.getSmtpServer(), emailConfig.getFromEmail());
    result.recordEntity("emailConfig", ImportResult.EntityAction.UPDATED);
  }
}
