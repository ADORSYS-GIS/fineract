package org.apache.fineract.config.model.security;

import java.time.LocalDate;

import lombok.Data;

/**
 * Staff member model.
 *
 * <p>Represents staff members (loan officers, managers, etc.) in the Fineract system.
 *
 * <p>Staff can be assigned to:
 *
 * <ul>
 *   <li>Offices - staff work at specific offices
 *   <li>Clients - clients can have assigned loan officers
 *   <li>Groups - groups can have assigned staff
 *   <li>Centers - centers can have assigned staff
 *   <li>Users - users can be linked to staff records
 * </ul>
 *
 * <p>Example YAML:
 *
 * <pre>
 * staff:
 *   - externalId: "STAFF-001"
 *     officeName: "Head Office"
 *     firstName: "Jane"
 *     lastName: "Advisor"
 *     isLoanOfficer: true
 *     isActive: true
 *     joiningDate: [2023, 1, 15]
 *     mobileNo: "+1234567890"
 *     emailAddress: "jane.advisor@example.com"
 * </pre>
 */
@Data
public class Staff {

  /**
   * External ID for the staff member (unique identifier).
   *
   * <p>Used for lookups and references from other entities.
   */
  private String externalId;

  /**
   * Office name where the staff member is assigned.
   *
   * <p>Will be resolved to officeId during import.
   */
  private String officeName;

  /** First name of the staff member. */
  private String firstName;

  /** Last name of the staff member. */
  private String lastName;

  /**
   * Whether the staff member is a loan officer.
   *
   * <p>Loan officers can be assigned to clients, groups, and loan accounts.
   */
  private Boolean isLoanOfficer;

  /**
   * Whether the staff member is currently active.
   *
   * <p>Inactive staff cannot be assigned to new clients/groups.
   */
  private Boolean isActive;

  /**
   * Date when the staff member joined.
   *
   * <p>Format in YAML: [year, month, day] (e.g., [2023, 1, 15])
   */
  private LocalDate joiningDate;

  /** Mobile phone number (optional). */
  private String mobileNo;

  /** Email address (optional). */
  private String emailAddress;
}
