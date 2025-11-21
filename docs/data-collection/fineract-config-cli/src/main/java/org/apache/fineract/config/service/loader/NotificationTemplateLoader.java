package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.NotificationTemplate;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for notification templates (SMS/Email).
 *
 * <p>Creates or updates notification templates for various events.
 *
 * <p>API Endpoints: - POST /api/v1/templates - PUT /api/v1/templates/{id}
 */
@Slf4j
@Component
public class NotificationTemplateLoader {

  private final FineractApiClient apiClient;

  public NotificationTemplateLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads notification templates.
   *
   * @param templates list of templates
   * @param context import context
   * @param result import result
   */
  public void load(
      List<NotificationTemplate> templates, ImportContext context, ImportResult result) {
    log.debug("Loading {} notification templates", templates.size());

    for (NotificationTemplate template : templates) {
      try {
        loadSingleTemplate(template, context, result);
      } catch (Exception ex) {
        log.error("Failed to load template '{}': {}", template.getName(), ex.getMessage());
        result.recordEntity("notificationTemplate", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single notification template.
   *
   * @param template template to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleTemplate(
      NotificationTemplate template, ImportContext context, ImportResult result) {
    log.debug("Loading notification template: {}", template.getName());

    // Get existing templates
    List<Map<String, Object>> existingTemplates = apiClient.get("/api/v1/templates", List.class);

    // Find template by name
    Map<String, Object> existingTemplate =
        existingTemplates.stream()
            .filter(t -> template.getName().equals(t.get("name")))
            .findFirst()
            .orElse(null);

    Map<String, Object> request = buildRequest(template);

    if (existingTemplate == null) {
      // Create new template
      Map<String, Object> response = apiClient.post("/api/v1/templates", request, Map.class);
      Long templateId = ((Number) response.get("resourceId")).longValue();

      log.info("Notification template created: {} ({})", template.getName(), template.getType());
      result.recordEntity("notificationTemplate", ImportResult.EntityAction.CREATED);

      // Store for reference
      context.registerEntity("notificationTemplate", template.getName(), templateId);
    } else {
      // Update existing template
      Long templateId = ((Number) existingTemplate.get("id")).longValue();
      apiClient.put("/api/v1/templates/" + templateId, request, Map.class);

      log.info("Notification template updated: {} ({})", template.getName(), template.getType());
      result.recordEntity("notificationTemplate", ImportResult.EntityAction.UPDATED);

      // Store for reference
      context.registerEntity("notificationTemplate", template.getName(), templateId);
    }
  }

  /**
   * Builds API request for notification template.
   *
   * @param template template
   * @return request map
   */
  private Map<String, Object> buildRequest(NotificationTemplate template) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", template.getName());
    request.put("type", template.getType()); // SMS or EMAIL
    request.put("subject", template.getSubject());
    request.put("text", template.getText());

    if (template.getEntity() != null) {
      request.put("entity", template.getEntity());
    }

    return request;
  }
}
