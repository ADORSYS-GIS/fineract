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
   * <p>Fineract Template API requires:
   *
   * <ul>
   *   <li>entity - integer (enum index: 0=CLIENT, 1=LOAN, etc.)
   *   <li>type - integer (0=DOCUMENT, 2=SMS)
   *   <li>mappers - array (optional)
   * </ul>
   *
   * @param template template
   * @return request map
   */
  private Map<String, Object> buildRequest(NotificationTemplate template) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", template.getName());

    // Map type string to integer (0=DOCUMENT, 2=SMS)
    String typeStr = template.getType() != null ? template.getType() : template.getChannel();
    request.put("type", mapTemplateType(typeStr));

    // Map entity string to integer
    String entityStr = template.getEntity();
    request.put("entity", mapTemplateEntity(entityStr));

    if (template.getSubject() != null) {
      request.put("subject", template.getSubject());
    }

    // Use text or messageBody
    String text = template.getText() != null ? template.getText() : template.getMessageBody();
    if (text != null) {
      request.put("text", text);
    }

    // Mappers array is required by Fineract Template API (can be empty)
    request.put("mappers", List.of());

    return request;
  }

  /**
   * Maps template type string to Fineract integer.
   *
   * @param type type string (SMS, EMAIL, DOCUMENT)
   * @return integer code (0=DOCUMENT, 2=SMS)
   */
  private Integer mapTemplateType(String type) {
    if (type == null) {
      return 0; // Default to DOCUMENT
    }
    return switch (type.toUpperCase().trim()) {
      case "SMS" -> 2;
      case "EMAIL", "DOCUMENT" -> 0;
      default -> 0;
    };
  }

  /**
   * Maps entity string to Fineract entity enum index.
   *
   * <p>TemplateEntity enum values: CLIENT=0, LOAN=1, etc.
   *
   * @param entity entity name
   * @return entity index
   */
  private Integer mapTemplateEntity(String entity) {
    if (entity == null) {
      return 0; // Default to CLIENT
    }
    return switch (entity.toUpperCase().trim()) {
      case "CLIENT" -> 0;
      case "LOAN" -> 1;
      case "SAVINGS", "SAVING" -> 2;
      case "GROUP" -> 3;
      case "CENTER" -> 4;
      case "OFFICE" -> 5;
      case "STAFF" -> 6;
      default -> 0; // Default to CLIENT
    };
  }
}
