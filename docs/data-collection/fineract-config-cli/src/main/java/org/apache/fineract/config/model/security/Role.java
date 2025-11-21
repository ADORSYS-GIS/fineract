package org.apache.fineract.config.model.security;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Role with permissions.
 *
 * <p>Defines a user role with associated permissions.
 */
@Data
public class Role {
  private String name;
  private String description;
  private List<String> permissions = new ArrayList<>(); // Permission names
  private Boolean disabled;
}
