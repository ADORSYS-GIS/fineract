package org.apache.fineract.config.model.security;

import lombok.Data;

/**
 * Permission definition.
 *
 * <p>Represents a system permission that can be assigned to roles.
 *
 * <p>Note: Permissions in Fineract are mostly pre-defined. This model is for documentation and
 * validation purposes.
 */
@Data
public class Permission {
  private String code; // Permission code (e.g., "READ_CLIENT", "CREATE_LOAN")
  private String grouping; // Permission group (e.g., "portfolio", "organisation")
  private String entityName; // Entity type (e.g., "CLIENT", "LOAN")
  private String actionName; // Action (e.g., "READ", "CREATE", "UPDATE", "DELETE")
  private String description;
}
