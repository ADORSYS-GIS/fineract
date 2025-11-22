package org.apache.fineract.config.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.config.model.account.LoanAccount;
import org.apache.fineract.config.model.account.SavingsAccount;
import org.apache.fineract.config.model.accounting.FinancialActivityMapping;
import org.apache.fineract.config.model.accounting.GLAccount;
import org.apache.fineract.config.model.client.Center;
import org.apache.fineract.config.model.client.Client;
import org.apache.fineract.config.model.client.Group;
import org.apache.fineract.config.model.product.Charge;
import org.apache.fineract.config.model.product.CollateralType;
import org.apache.fineract.config.model.product.DelinquencyBucket;
import org.apache.fineract.config.model.product.FloatingRate;
import org.apache.fineract.config.model.product.FundSource;
import org.apache.fineract.config.model.product.GuarantorType;
import org.apache.fineract.config.model.product.LoanProduct;
import org.apache.fineract.config.model.product.LoanProvisioning;
import org.apache.fineract.config.model.product.PaymentType;
import org.apache.fineract.config.model.product.SavingsProduct;
import org.apache.fineract.config.model.product.TaxGroup;
import org.apache.fineract.config.model.security.Office;
import org.apache.fineract.config.model.security.Role;
import org.apache.fineract.config.model.security.Staff;
import org.apache.fineract.config.model.security.Teller;
import org.apache.fineract.config.model.security.TellerCashierMapping;
import org.apache.fineract.config.model.security.User;
import org.apache.fineract.config.model.systemconfig.Holiday;
import org.apache.fineract.config.model.systemconfig.SchedulerJob;
import org.apache.fineract.config.model.systemconfig.SystemConfig;
import org.apache.fineract.config.model.transaction.LoanTransaction;
import org.apache.fineract.config.model.transaction.SavingsTransaction;

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
  private List<Office> offices = new ArrayList<>();

  /** Phase 2: Roles & Permissions */
  private List<Role> roles = new ArrayList<>();

  /** Phase 2: Users */
  private List<User> users = new ArrayList<>();

  /** Phase 2: Staff */
  private List<Staff> staff = new ArrayList<>();

  /** Phase 2: Tellers */
  private List<Teller> tellers = new ArrayList<>();

  /** Phase 2: Teller Cashier Mappings */
  private List<TellerCashierMapping> tellerCashierMappings = new ArrayList<>();

  /** Phase 3: Chart of Accounts */
  private List<GLAccount> chartOfAccounts = new ArrayList<>();

  /** Phase 3: Financial Activity Mappings */
  private List<FinancialActivityMapping> financialActivityMappings = new ArrayList<>();

  /** Phase 3: Teller Accounting Rules */
  private List<Object> tellerAccountingRules =
      new ArrayList<>(); // TODO: Create TellerAccountingRule model

  /** Phase 3: Maker-Checker Configuration */
  private List<Object> makerCheckerConfig =
      new ArrayList<>(); // TODO: Create MakerCheckerConfig model

  /** Phase 3: Scheduler Jobs */
  private List<SchedulerJob> schedulerJobs = new ArrayList<>();

  /** Phase 3: Loan Provisioning Criteria */
  private List<LoanProvisioning> loanProvisioning = new ArrayList<>();

  /** Phase 4: Floating Rates */
  private List<FloatingRate> floatingRates = new ArrayList<>();

  /** Phase 4: Tax Groups */
  private List<TaxGroup> taxGroups = new ArrayList<>();

  /** Phase 4: Charges */
  private List<Charge> charges = new ArrayList<>();

  /** Phase 4: Fund Sources */
  private List<FundSource> fundSources = new ArrayList<>();

  /** Phase 4: Payment Types */
  private List<PaymentType> paymentTypes = new ArrayList<>();

  /** Phase 4: Holiday Calendar */
  private List<Holiday> holidayCalendar = new ArrayList<>();

  /** Phase 4: Loan Products */
  private List<LoanProduct> loanProducts = new ArrayList<>();

  /** Phase 4: Delinquency Buckets */
  private List<DelinquencyBucket> delinquencyBuckets = new ArrayList<>();

  /** Phase 4: Savings Products */
  private List<SavingsProduct> savingsProducts = new ArrayList<>();

  /** Phase 4: Collateral Types */
  private List<CollateralType> collateralTypes = new ArrayList<>();

  /** Phase 4: Guarantor Types */
  private List<GuarantorType> guarantorTypes = new ArrayList<>();

  /** Phase 5: Centers */
  private List<Center> centers = new ArrayList<>();

  /** Phase 5: Clients */
  private List<Client> clients = new ArrayList<>();

  /** Phase 5: Groups */
  private List<Group> groups = new ArrayList<>();

  /** Phase 5: Savings Accounts */
  private List<SavingsAccount> savingsAccounts = new ArrayList<>();

  /** Phase 5: Loan Accounts */
  private List<LoanAccount> loanAccounts = new ArrayList<>();

  /** Phase 5: Loan Collateral */
  private List<Object> loanCollateral = new ArrayList<>(); // TODO: Create LoanCollateral model

  /** Phase 5: Loan Guarantors */
  private List<Object> loanGuarantors = new ArrayList<>(); // TODO: Create LoanGuarantor model

  /** Phase 6: Savings Transactions (deposits, withdrawals) */
  private List<SavingsTransaction> savingsTransactions = new ArrayList<>();

  /** Phase 6: Loan Transactions (repayments, waivers, writeoffs) */
  private List<LoanTransaction> loanTransactions = new ArrayList<>();

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @com.fasterxml.jackson.annotation.JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    org.slf4j.LoggerFactory.getLogger(this.getClass())
        .warn(
            "Unknown field '{}' with value '{}' in {} (will be ignored). "
                + "This may indicate a missing field in the model class.",
            key,
            value,
            this.getClass().getSimpleName());
  }
}
