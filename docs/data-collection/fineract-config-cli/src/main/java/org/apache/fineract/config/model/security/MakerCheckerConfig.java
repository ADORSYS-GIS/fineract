package org.apache.fineract.config.model.security;

import lombok.Data;

/**
 * Maker-Checker configuration.
 *
 * <p>Defines which operations require dual authorization (maker creates, checker approves).
 *
 * <p>NOTE: Fineract's maker-checker is boolean-only (enabled/disabled). When enabled, ALL
 * operations of that type will require approval. Amount-based thresholds are NOT supported by the
 * native Fineract API.
 *
 * <p>Example:
 *
 * <pre>
 * taskName: Loan Approval
 * entity: Loan
 * action: APPROVE
 * enabled: true
 * makerRole: Loan Officer
 * checkerRole: Branch Manager
 * description: All loan approvals require manager authorization
 * </pre>
 */
@Data
public class MakerCheckerConfig {
  /** Task name for documentation (e.g., "Loan Approval", "Client Activation"). */
  private String taskName;

  /**
   * Entity type (e.g., "Loan", "SAVINGSACCOUNT", "Client").
   *
   * <p>Valid values:
   *
   * <ul>
   *   <li>Loan
   *   <li>SAVINGSACCOUNT
   *   <li>Client
   *   <li>JOURNALENTRY
   *   <li>User
   *   <li>RESCHEDULELOAN
   *   <li>OFFICETRANSACTION
   * </ul>
   */
  private String entity;

  /**
   * Action type (e.g., "APPROVE", "DISBURSE", "CREATE", "ACTIVATE").
   *
   * <p>Valid values:
   *
   * <ul>
   *   <li>APPROVE
   *   <li>DISBURSE
   *   <li>WRITEOFF
   *   <li>CREATE
   *   <li>ACTIVATE
   *   <li>UPDATE
   *   <li>CLOSE
   *   <li>PROPOSETRANSFER
   * </ul>
   */
  private String action;

  /**
   * Whether this maker-checker permission is enabled (default: false).
   *
   * <p>When true, the permission will be activated in Fineract and the corresponding maker/checker
   * roles will receive the appropriate permissions.
   */
  private Boolean enabled;

  /**
   * Role that initiates the operation (maker).
   *
   * <p>This role will receive the base permission (e.g., APPROVE_LOAN).
   */
  private String makerRole;

  /**
   * Role that approves the operation (checker).
   *
   * <p>This role will receive the checker permission (e.g., CHECKER_APPROVE_LOAN).
   */
  private String checkerRole;

  /** Description of what this maker-checker configuration controls. */
  private String description;
}
