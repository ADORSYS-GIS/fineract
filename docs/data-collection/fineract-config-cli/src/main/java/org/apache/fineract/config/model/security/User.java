package org.apache.fineract.config.model.security;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * User account.
 *
 * <p>Defines a system user with authentication credentials and role assignments.
 */
@Data
public class User {
  private String username;
  private String firstname;
  private String lastname;
  private String email;
  private String password; // Will be hashed by Fineract
  private Boolean passwordNeverExpires;
  private String officeName; // Reference to office
  private List<String> roles = new ArrayList<>(); // Role names
  private Boolean isSelfServiceUser;
  private List<String> staffName; // Reference to staff (optional)
}
