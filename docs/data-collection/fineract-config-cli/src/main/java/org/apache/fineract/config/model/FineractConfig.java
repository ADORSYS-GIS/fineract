package org.apache.fineract.config.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.config.model.systemconfig.SystemConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Root configuration model representing the complete Fineract configuration YAML file.
 *
 * <p>This is the top-level object that gets parsed from YAML and contains all entity configurations
 * organized by phase.
 *
 * <p>Example YAML structure:
 *
 * <pre>
 * tenant: default
 * systemConfig:
 *   currency: {...}
 *   workingDays: [...]
 * offices: [...]
 * staff: [...]
 * </pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FineractConfig {

  /** Tenant identifier (default: 'default') */
  private String tenant = "default";

  /** Phase 1: System Configuration */
  private SystemConfig systemConfig;

  /** Phase 2: Offices */
  private List<Object> offices = new ArrayList<>();

  /** Phase 2: Roles & Permissions */
  private List<Object> roles = new ArrayList<>();

  /** Phase 2: Users */
  private List<Object> users = new ArrayList<>();

  /** Phase 2: Staff */
  private List<Object> staff = new ArrayList<>();

  /** Phase 2: Tellers */
  private List<Object> tellers = new ArrayList<>();

  /** Phase 3: Chart of Accounts */
  private List<Object> chartOfAccounts = new ArrayList<>();

  /** Phase 3: Financial Activity Mappings */
  private List<Object> financialActivityMappings = new ArrayList<>();

  /** Phase 3: Teller Accounting Rules */
  private List<Object> tellerAccountingRules = new ArrayList<>();

  /** Phase 3: Maker-Checker Configuration */
  private List<Object> makerCheckerConfig = new ArrayList<>();

  /** Phase 3: Scheduler Jobs */
  private List<Object> schedulerJobs = new ArrayList<>();

  /** Phase 3: Loan Provisioning Criteria */
  private List<Object> loanProvisioningCriteria = new ArrayList<>();

  /** Phase 4: Floating Rates */
  private List<Object> floatingRates = new ArrayList<>();

  /** Phase 4: Tax Groups */
  private List<Object> taxGroups = new ArrayList<>();

  /** Phase 4: Charges */
  private List<Object> charges = new ArrayList<>();

  /** Phase 4: Fund Sources */
  private List<Object> fundSources = new ArrayList<>();

  /** Phase 4: Payment Types */
  private List<Object> paymentTypes = new ArrayList<>();

  /** Phase 4: Holiday Calendar */
  private List<Object> holidayCalendar = new ArrayList<>();

  /** Phase 4: Loan Products */
  private List<Object> loanProducts = new ArrayList<>();

  /** Phase 4: Delinquency Buckets */
  private List<Object> delinquencyBuckets = new ArrayList<>();

  /** Phase 4: Savings Products */
  private List<Object> savingsProducts = new ArrayList<>();

  /** Phase 4: Collateral Types */
  private List<Object> collateralTypes = new ArrayList<>();

  /** Phase 5: Centers */
  private List<Object> centers = new ArrayList<>();

  /** Phase 5: Clients */
  private List<Object> clients = new ArrayList<>();

  /** Phase 5: Groups */
  private List<Object> groups = new ArrayList<>();

  /** Phase 5: Savings Accounts */
  private List<Object> savingsAccounts = new ArrayList<>();

  /** Phase 5: Loan Accounts */
  private List<Object> loanAccounts = new ArrayList<>();

  /** Phase 5: Loan Collateral */
  private List<Object> loanCollateral = new ArrayList<>();

  /** Phase 5: Loan Guarantors */
  private List<Object> loanGuarantors = new ArrayList<>();

  /** Phase 6: Savings Transactions (deposits, withdrawals) */
  private List<Object> savingsTransactions = new ArrayList<>();

  /** Phase 6: Loan Transactions (repayments, waivers, writeoffs) */
  private List<Object> loanTransactions = new ArrayList<>();
}
