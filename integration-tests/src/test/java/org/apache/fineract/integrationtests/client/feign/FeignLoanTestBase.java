/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.integrationtests.client.feign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.fineract.client.feign.FineractFeignClient;
import org.apache.fineract.client.models.DeleteLoansLoanIdChargesChargeIdResponse;
import org.apache.fineract.client.models.GetLoanProductsProductIdResponse;
import org.apache.fineract.client.models.GetLoansLoanIdChargesChargeIdResponse;
import org.apache.fineract.client.models.GetLoansLoanIdResponse;
import org.apache.fineract.client.models.GetLoansLoanIdStatus;
import org.apache.fineract.client.models.GetLoansLoanIdTransactionsTemplateResponse;
import org.apache.fineract.client.models.PostCreateRescheduleLoansRequest;
import org.apache.fineract.client.models.PostLoanProductsRequest;
import org.apache.fineract.client.models.PostLoansLoanIdChargesChargeIdRequest;
import org.apache.fineract.client.models.PostLoansLoanIdChargesChargeIdResponse;
import org.apache.fineract.client.models.PostLoansLoanIdChargesRequest;
import org.apache.fineract.client.models.PostLoansLoanIdChargesResponse;
import org.apache.fineract.client.models.PostLoansLoanIdRequest;
import org.apache.fineract.client.models.PostLoansLoanIdResponse;
import org.apache.fineract.client.models.PostLoansLoanIdTransactionsRequest;
import org.apache.fineract.client.models.PostLoansLoanIdTransactionsResponse;
import org.apache.fineract.client.models.PostLoansRequest;
import org.apache.fineract.client.models.PostUpdateRescheduleLoansRequest;
import org.apache.fineract.client.models.PutLoanProductsProductIdRequest;
import org.apache.fineract.client.models.PutLoanProductsProductIdResponse;
import org.apache.fineract.integrationtests.client.FeignIntegrationTest;
import org.apache.fineract.integrationtests.client.feign.helpers.FeignAccountHelper;
import org.apache.fineract.integrationtests.client.feign.helpers.FeignBusinessDateHelper;
import org.apache.fineract.integrationtests.client.feign.helpers.FeignChargesHelper;
import org.apache.fineract.integrationtests.client.feign.helpers.FeignClientHelper;
import org.apache.fineract.integrationtests.client.feign.helpers.FeignJournalEntryHelper;
import org.apache.fineract.integrationtests.client.feign.helpers.FeignLoanHelper;
import org.apache.fineract.integrationtests.client.feign.helpers.FeignTransactionHelper;
import org.apache.fineract.integrationtests.client.feign.modules.LoanProductTemplates;
import org.apache.fineract.integrationtests.client.feign.modules.LoanRequestBuilders;
import org.apache.fineract.integrationtests.client.feign.modules.LoanTestAccounts;
import org.apache.fineract.integrationtests.client.feign.modules.LoanTestData;
import org.apache.fineract.integrationtests.client.feign.modules.LoanTestValidators;
import org.apache.fineract.integrationtests.common.FineractFeignClientHelper;
import org.apache.fineract.integrationtests.common.loans.LoanTestLifecycleExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LoanTestLifecycleExtension.class)
public abstract class FeignLoanTestBase extends FeignIntegrationTest implements LoanProductTemplates {

    protected static FeignAccountHelper accountHelper;
    protected static FeignLoanHelper loanHelper;
    protected static FeignTransactionHelper transactionHelper;
    protected static FeignJournalEntryHelper journalHelper;
    protected static FeignBusinessDateHelper businessDateHelper;
    protected static FeignClientHelper clientHelper;
    protected static FeignChargesHelper chargesHelper;
    protected static LoanTestAccounts accounts;

    @BeforeAll
    public static void setupHelpers() {
        FineractFeignClient client = FineractFeignClientHelper.getFineractFeignClient();
        accountHelper = new FeignAccountHelper(client);
        loanHelper = new FeignLoanHelper(client);
        transactionHelper = new FeignTransactionHelper(client);
        journalHelper = new FeignJournalEntryHelper(client);
        businessDateHelper = new FeignBusinessDateHelper(client);
        clientHelper = new FeignClientHelper(client);
        chargesHelper = new FeignChargesHelper(client);
    }

    protected LoanTestAccounts getAccounts() {
        if (accounts == null) {
            accounts = new LoanTestAccounts(accountHelper);
        }
        return accounts;
    }

    @Override
    public Long getAssetAccountId(String accountName) {
        return getAccounts().getAssetAccountId(accountName);
    }

    @Override
    public Long getLiabilityAccountId(String accountName) {
        return getAccounts().getLiabilityAccountId(accountName);
    }

    @Override
    public Long getIncomeAccountId(String accountName) {
        return getAccounts().getIncomeAccountId(accountName);
    }

    @Override
    public Long getExpenseAccountId(String accountName) {
        return getAccounts().getExpenseAccountId(accountName);
    }

    protected Long createClient() {
        return clientHelper.createClient();
    }

    protected Long createClient(String activationDate) {
        return clientHelper.createClient(activationDate);
    }

    protected Long createLoanProduct(PostLoanProductsRequest request) {
        return loanHelper.createLoanProduct(request);
    }

    protected GetLoanProductsProductIdResponse retrieveLoanProduct(Long productId) {
        return loanHelper.retrieveLoanProduct(productId);
    }

    protected PutLoanProductsProductIdResponse updateLoanProduct(Long productId, PutLoanProductsProductIdRequest request) {
        return loanHelper.updateLoanProduct(productId, request);
    }

    protected Long applyForLoan(PostLoansRequest request) {
        return loanHelper.applyForLoan(request);
    }

    protected PostLoansLoanIdResponse approveLoan(Long loanId, PostLoansLoanIdRequest request) {
        return loanHelper.approveLoan(loanId, request);
    }

    protected PostLoansLoanIdRequest approveLoanRequest(Double amount, String approvalDate) {
        return LoanRequestBuilders.approveLoan(amount, approvalDate);
    }

    protected PostLoansLoanIdRequest approveLoanRequest(Double amount, String approvalDate, String expectedDisbursementDate) {
        return LoanRequestBuilders.approveLoan(amount, approvalDate, expectedDisbursementDate);
    }

    protected PostLoansLoanIdResponse disburseLoan(Long loanId, PostLoansLoanIdRequest request) {
        return loanHelper.disburseLoan(loanId, request);
    }

    protected GetLoansLoanIdResponse getLoanDetails(Long loanId) {
        return loanHelper.getLoanDetails(loanId);
    }

    protected void undoApproval(Long loanId) {
        loanHelper.undoApproval(loanId);
    }

    protected void undoDisbursement(Long loanId) {
        loanHelper.undoDisbursement(loanId);
    }

    protected PostLoansLoanIdResponse disburseToSavings(Long loanId, PostLoansLoanIdRequest request) {
        return loanHelper.disburseToSavings(loanId, request);
    }

    protected PostLoansLoanIdResponse rejectLoan(Long loanId, PostLoansLoanIdRequest request) {
        return loanHelper.rejectLoan(loanId, request);
    }

    protected PostLoansLoanIdResponse withdrawLoan(Long loanId, PostLoansLoanIdRequest request) {
        return loanHelper.withdrawLoan(loanId, request);
    }

    protected PostLoansLoanIdTransactionsResponse closeLoan(Long loanId, PostLoansLoanIdTransactionsRequest request) {
        return loanHelper.closeLoan(loanId, request);
    }

    protected PostLoansLoanIdTransactionsResponse forecloseLoan(Long loanId, PostLoansLoanIdTransactionsRequest request) {
        return loanHelper.forecloseLoan(loanId, request);
    }

    protected PostLoansLoanIdChargesResponse addLoanCharge(Long loanId, PostLoansLoanIdChargesRequest request) {
        return loanHelper.addLoanCharge(loanId, request);
    }

    protected List<GetLoansLoanIdChargesChargeIdResponse> getLoanCharges(Long loanId) {
        return loanHelper.getLoanCharges(loanId);
    }

    protected GetLoansLoanIdChargesChargeIdResponse getLoanCharge(Long loanId, Long loanChargeId) {
        return loanHelper.getLoanCharge(loanId, loanChargeId);
    }

    protected DeleteLoansLoanIdChargesChargeIdResponse deleteLoanCharge(Long loanId, Long loanChargeId) {
        return loanHelper.deleteLoanCharge(loanId, loanChargeId);
    }

    protected PostLoansLoanIdChargesChargeIdResponse waiveLoanCharge(Long loanId, Long loanChargeId,
            PostLoansLoanIdChargesChargeIdRequest request) {
        return loanHelper.waiveLoanCharge(loanId, loanChargeId, request);
    }

    protected PostLoansLoanIdChargesChargeIdResponse payLoanCharge(Long loanId, Long loanChargeId,
            PostLoansLoanIdChargesChargeIdRequest request) {
        return loanHelper.payLoanCharge(loanId, loanChargeId, request);
    }

    protected Long createLoanSpecifiedDueDateCharge(double amount) {
        return chargesHelper.createLoanSpecifiedDueDateCharge(amount);
    }

    protected Long createLoanDisbursementCharge(double amount) {
        return chargesHelper.createLoanDisbursementCharge(amount);
    }

    protected Long addRepayment(Long loanId, PostLoansLoanIdTransactionsRequest request) {
        return transactionHelper.addRepayment(loanId, request);
    }

    protected Long addInterestWaiver(Long loanId, PostLoansLoanIdTransactionsRequest request) {
        return transactionHelper.addInterestWaiver(loanId, request);
    }

    protected Long chargeOff(Long loanId, PostLoansLoanIdTransactionsRequest request) {
        return transactionHelper.chargeOff(loanId, request);
    }

    protected Long addChargeback(Long loanId, Long transactionId, PostLoansLoanIdTransactionsRequest request) {
        return transactionHelper.addChargeback(loanId, transactionId, request);
    }

    protected void undoRepayment(Long loanId, Long transactionId, String transactionDate) {
        transactionHelper.undoRepayment(loanId, transactionId, transactionDate);
    }

    protected void verifyJournalEntries(Long loanId, LoanTestData.Journal... expectedEntries) {
        journalHelper.verifyJournalEntries(loanId, expectedEntries);
    }

    protected void verifyJournalEntriesSequentially(Long loanId, LoanTestData.Journal... expectedEntries) {
        journalHelper.verifyJournalEntriesSequentially(loanId, expectedEntries);
    }

    protected void runAt(String date, Runnable action) {
        businessDateHelper.runAt(date, detectDateFormat(date), action);
    }

    protected void updateBusinessDate(String type, String date) {
        businessDateHelper.updateBusinessDate(type, date, detectDateFormat(date));
    }

    private static String detectDateFormat(String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}") ? LoanTestData.ISO_DATE_PATTERN : LoanTestData.DATETIME_PATTERN;
    }

    protected void validateRepaymentPeriod(GetLoansLoanIdResponse loanDetails, Integer index, LocalDate dueDate, double principalDue,
            double principalPaid, double principalOutstanding) {
        LoanTestValidators.validateRepaymentPeriod(loanDetails, index, dueDate, principalDue, principalPaid, principalOutstanding, 0.0,
                0.0);
    }

    protected void validateRepaymentPeriod(GetLoansLoanIdResponse loanDetails, Integer index, LocalDate dueDate, double principalDue,
            double principalPaid, double principalOutstanding, double paidInAdvance, double paidLate) {
        LoanTestValidators.validateRepaymentPeriod(loanDetails, index, dueDate, principalDue, principalPaid, principalOutstanding,
                paidInAdvance, paidLate);
    }

    protected void verifyLoanStatus(GetLoansLoanIdResponse loanDetails, Function<GetLoansLoanIdStatus, Boolean> extractor) {
        LoanTestValidators.verifyLoanStatus(loanDetails, extractor);
    }

    protected Long createApproveAndDisburseLoan(Long clientId, Long productId, String date, Double principal, Integer numberOfRepayments) {
        PostLoansRequest applyRequest = LoanRequestBuilders.applyLoan(clientId, productId, date, principal, numberOfRepayments);
        Long loanId = applyForLoan(applyRequest);

        PostLoansLoanIdRequest approveRequest = LoanRequestBuilders.approveLoan(principal, date);
        approveLoan(loanId, approveRequest);

        PostLoansLoanIdRequest disburseRequest = LoanRequestBuilders.disburseLoan(principal, date);
        disburseLoan(loanId, disburseRequest);

        return loanId;
    }

    protected Long createApproveAndDisburseProgressiveLoan(Long clientId, Long productId, String date, Double principal,
            Integer numberOfRepayments) {
        PostLoansRequest applyRequest = LoanRequestBuilders.applyProgressiveLoan(clientId, productId, date, principal, numberOfRepayments);
        Long loanId = applyForLoan(applyRequest);

        PostLoansLoanIdRequest approveRequest = LoanRequestBuilders.approveLoan(principal, date);
        approveLoan(loanId, approveRequest);

        PostLoansLoanIdRequest disburseRequest = LoanRequestBuilders.disburseLoan(principal, date);
        disburseLoan(loanId, disburseRequest);

        return loanId;
    }

    protected Long createApprovedLoan(Long clientId, Long productId, String date, Double principal, Integer numberOfRepayments) {
        PostLoansRequest applyRequest = LoanRequestBuilders.applyLoan(clientId, productId, date, principal, numberOfRepayments);
        Long loanId = applyForLoan(applyRequest);

        PostLoansLoanIdRequest approveRequest = LoanRequestBuilders.approveLoan(principal, date);
        approveLoan(loanId, approveRequest);

        return loanId;
    }

    protected LoanTestData.Journal debit(Long glAccountId, double amount) {
        return LoanTestData.Journal.debit(glAccountId, amount);
    }

    protected LoanTestData.Journal credit(Long glAccountId, double amount) {
        return LoanTestData.Journal.credit(glAccountId, amount);
    }

    protected PostLoansLoanIdTransactionsRequest repayment(double amount, String date) {
        return LoanRequestBuilders.repayLoan(amount, date);
    }

    protected PostLoansLoanIdTransactionsRequest waiveInterest(double amount, String date) {
        return LoanRequestBuilders.waiveInterest(amount, date);
    }

    protected PostLoansLoanIdTransactionsRequest chargeOff(String date) {
        return LoanRequestBuilders.chargeOff(date);
    }

    protected void executeInlineCOB(Long loanId) {
        transactionHelper.executeInlineCOB(loanId);
    }

    protected GetLoansLoanIdTransactionsTemplateResponse getPrepaymentAmount(Long loanId, String transactionDate, String dateFormat) {
        return transactionHelper.getPrepaymentAmount(loanId, transactionDate, dateFormat);
    }

    protected Long createRescheduleRequest(PostCreateRescheduleLoansRequest request) {
        return loanHelper.createRescheduleRequest(request);
    }

    protected Long approveRescheduleRequest(Long scheduleId, PostUpdateRescheduleLoansRequest request) {
        return loanHelper.approveRescheduleRequest(scheduleId, request);
    }

    protected void createAndApproveReschedule(Long loanId, String submittedOnDate, String rescheduleFromDate, String adjustedDueDate) {
        loanHelper.createAndApproveRescheduleRequest(
                LoanRequestBuilders.rescheduleRequest(loanId, submittedOnDate, rescheduleFromDate, adjustedDueDate),
                LoanRequestBuilders.approveReschedule(submittedOnDate));
    }

    protected Long reAge(Long loanId, PostLoansLoanIdTransactionsRequest request) {
        return transactionHelper.reAge(loanId, request);
    }

    protected PostLoansLoanIdTransactionsRequest reAge(String startDate, String frequencyType, Integer frequencyNumber,
            Integer numberOfInstallments) {
        return LoanRequestBuilders.reAge(startDate, frequencyType, frequencyNumber, numberOfInstallments);
    }

    protected LoanTestData.TransactionExt transaction(double amount, String type, String date, double outstandingPrincipal,
            double principalPortion, double interestPortion, double feePortion, double penaltyPortion, double unrecognizedIncomePortion,
            double overpaymentPortion) {
        return new LoanTestData.TransactionExt(amount, type, date, outstandingPrincipal, principalPortion, interestPortion, feePortion,
                penaltyPortion, unrecognizedIncomePortion, overpaymentPortion, false);
    }

    protected LoanTestData.TransactionExt transaction(double amount, String type, String date, double outstandingPrincipal,
            double principalPortion, double interestPortion, double feePortion, double penaltyPortion, double unrecognizedIncomePortion,
            double overpaymentPortion, boolean reversed) {
        return new LoanTestData.TransactionExt(amount, type, date, outstandingPrincipal, principalPortion, interestPortion, feePortion,
                penaltyPortion, unrecognizedIncomePortion, overpaymentPortion, reversed);
    }

    protected LoanTestData.Installment installment(double principalAmount, Boolean completed, String dueDate) {
        return new LoanTestData.Installment(principalAmount, null, null, null, null, completed, dueDate, null, null);
    }

    protected LoanTestData.Installment installment(double principalAmount, double interestAmount, double totalOutstandingAmount,
            Boolean completed, String dueDate) {
        return new LoanTestData.Installment(principalAmount, interestAmount, null, null, totalOutstandingAmount, completed, dueDate, null,
                null);
    }

    protected LoanTestData.Installment installment(double principalAmount, double interestAmount, double feeAmount,
            double totalOutstandingAmount, Boolean completed, String dueDate) {
        return new LoanTestData.Installment(principalAmount, interestAmount, feeAmount, null, totalOutstandingAmount, completed, dueDate,
                null, null);
    }

    protected LoanTestData.Installment installment(double principalAmount, double interestAmount, double feeAmount, double penaltyAmount,
            double totalOutstandingAmount, Boolean completed, String dueDate) {
        return new LoanTestData.Installment(principalAmount, interestAmount, feeAmount, penaltyAmount, totalOutstandingAmount, completed,
                dueDate, null, null);
    }

    protected LoanTestData.Installment installment(double principalAmount, double interestAmount, double feeAmount, double penaltyAmount,
            LoanTestData.OutstandingAmounts outstandingAmounts, Boolean completed, String dueDate) {
        return new LoanTestData.Installment(principalAmount, interestAmount, feeAmount, penaltyAmount, null, completed, dueDate,
                outstandingAmounts, null);
    }

    protected LoanTestData.Installment installment(double principalAmount, double interestAmount, double feeAmount, double penaltyAmount,
            double totalOutstanding, Boolean completed, String dueDate, double loanBalance) {
        return new LoanTestData.Installment(principalAmount, interestAmount, feeAmount, penaltyAmount, totalOutstanding, completed, dueDate,
                null, loanBalance);
    }

    protected LoanTestData.OutstandingAmounts outstanding(double principal, double interestOutstanding, double fee, double penalty,
            double total) {
        return new LoanTestData.OutstandingAmounts(principal, interestOutstanding, fee, penalty, total);
    }

    protected LoanTestData.TransactionExt reversedTransaction(double principalAmount, String type, String date) {
        return new LoanTestData.TransactionExt(principalAmount, type, date, null, null, null, null, null, null, null, true);
    }

    protected LoanTestData.TransactionExt transaction(double amount, String type, String date) {
        return new LoanTestData.TransactionExt(amount, type, date, null, null, null, null, null, null, null, false);
    }

    protected void verifyTransactions(Long loanId, LoanTestData.TransactionExt... transactions) {
        GetLoansLoanIdResponse loanDetails = getLoanDetails(loanId);
        LoanTestValidators.verifyTransactions(loanDetails, transactions);
    }

    protected void verifyRepaymentSchedule(Long loanId, LoanTestData.Installment... installments) {
        GetLoansLoanIdResponse loanDetails = getLoanDetails(loanId);
        LoanTestValidators.verifyRepaymentSchedule(loanDetails, installments);
    }

    protected PostLoanProductsRequest createOnePeriod30DaysLongNoInterestPeriodicAccrualProductWithAdvancedPaymentAllocation() {
        return createOnePeriod30DaysLongNoInterestPeriodicAccrualProduct()
                .transactionProcessingStrategyCode("advanced-payment-allocation-strategy").loanScheduleType("PROGRESSIVE")
                .loanScheduleProcessingType("HORIZONTAL").addPaymentAllocationItem(LoanRequestBuilders.defaultPaymentAllocation());
    }

    protected PostLoanProductsRequest createOnePeriod30DaysLongNoInterestPeriodicAccrualProduct() {
        return createOnePeriod30DaysPeriodicAccrualProduct(0);
    }

    protected PostLoanProductsRequest createOnePeriod30DaysPeriodicAccrualProduct(double interestRatePerPeriod) {
        return new PostLoanProductsRequest()
                .name(org.apache.fineract.integrationtests.common.Utils.uniqueRandomStringGenerator("LOAN_PRODUCT_", 6))//
                .shortName(org.apache.fineract.integrationtests.common.Utils.uniqueRandomStringGenerator("", 4))//
                .description("Loan Product Description")//
                .includeInBorrowerCycle(false)//
                .currencyCode("USD")//
                .digitsAfterDecimal(2)//
                .inMultiplesOf(0)//
                .installmentAmountInMultiplesOf(1)//
                .useBorrowerCycle(false)//
                .minPrincipal(100.0)//
                .principal(1000.0)//
                .maxPrincipal(100000.0)//
                .minNumberOfRepayments(1)//
                .numberOfRepayments(1)//
                .maxNumberOfRepayments(30)//
                .isLinkedToFloatingInterestRates(false)//
                .minInterestRatePerPeriod(0.0)//
                .interestRatePerPeriod(interestRatePerPeriod)//
                .maxInterestRatePerPeriod(100.0)//
                .interestRateFrequencyType(LoanTestData.InterestRateFrequencyType.MONTHS)//
                .repaymentEvery(30)//
                .repaymentFrequencyType(LoanTestData.RepaymentFrequencyType.DAYS_L)//
                .amortizationType(LoanTestData.AmortizationType.EQUAL_INSTALLMENTS)//
                .interestType(LoanTestData.InterestType.DECLINING_BALANCE)//
                .isEqualAmortization(false)//
                .interestCalculationPeriodType(LoanTestData.InterestCalculationPeriodType.SAME_AS_REPAYMENT_PERIOD)//
                .transactionProcessingStrategyCode("due-penalty-fee-interest-principal-in-advance-principal-penalty-fee-interest-strategy")//
                .loanScheduleType("CUMULATIVE")//
                .daysInYearType(LoanTestData.DaysInYearType.ACTUAL)//
                .daysInMonthType(LoanTestData.DaysInMonthType.ACTUAL)//
                .canDefineInstallmentAmount(true)//
                .graceOnArrearsAgeing(3)//
                .overdueDaysForNPA(179)//
                .accountMovesOutOfNPAOnlyOnArrearsCompletion(false)//
                .principalThresholdForLastInstallment(50)//
                .allowVariableInstallments(false)//
                .canUseForTopup(false)//
                .isInterestRecalculationEnabled(false)//
                .holdGuaranteeFunds(false)//
                .multiDisburseLoan(true)//
                .allowAttributeOverrides(new org.apache.fineract.client.models.AllowAttributeOverrides()//
                        .amortizationType(true)//
                        .interestType(true)//
                        .transactionProcessingStrategyCode(true)//
                        .interestCalculationPeriodType(true)//
                        .inArrearsTolerance(true)//
                        .repaymentEvery(true)//
                        .graceOnPrincipalAndInterestPayment(true)//
                        .graceOnArrearsAgeing(true))//
                .allowPartialPeriodInterestCalculation(true)//
                .maxTrancheCount(10)//
                .outstandingLoanBalance(10000.0)//
                .charges(java.util.Collections.emptyList())//
                .accountingRule(1)//
                .dateFormat(LoanTestData.DATETIME_PATTERN)//
                .locale("en_GB")//
                .disallowExpectedDisbursements(true)//
                .allowApprovedDisbursedAmountsOverApplied(true)//
                .overAppliedCalculationType("percentage")//
                .overAppliedNumber(50);
    }

    protected PostLoanProductsRequest create4Period1MonthLongWithoutInterestProduct(String repaymentStrategy) {
        PostLoanProductsRequest productRequest = createOnePeriod30DaysLongNoInterestPeriodicAccrualProduct().multiDisburseLoan(false)//
                .disallowExpectedDisbursements(false)//
                .allowApprovedDisbursedAmountsOverApplied(false)//
                .overAppliedCalculationType(null)//
                .overAppliedNumber(null)//
                .principal(1000.0)//
                .numberOfRepayments(4)//
                .repaymentEvery(1)//
                .repaymentFrequencyType(LoanTestData.RepaymentFrequencyType.MONTHS.longValue())//
                .transactionProcessingStrategyCode(repaymentStrategy);
        if (org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.impl.AdvancedPaymentScheduleTransactionProcessor.ADVANCED_PAYMENT_ALLOCATION_STRATEGY
                .equals(repaymentStrategy)) {
            productRequest.loanScheduleType("PROGRESSIVE").loanScheduleProcessingType("HORIZONTAL")
                    .addPaymentAllocationItem(LoanRequestBuilders.defaultPaymentAllocation());
        } else {
            productRequest.loanScheduleType("CUMULATIVE").loanScheduleProcessingType(null).paymentAllocation(null);
        }
        return productRequest;
    }

    protected PostLoansRequest applyLoanRequest(Long clientId, Long productId, String submittedOnDate, Double principal,
            Integer numberOfRepayments) {
        return LoanRequestBuilders.applyLoanRequest(clientId, productId, submittedOnDate, principal, numberOfRepayments);
    }

    protected PostLoansRequest applyLoanRequest(Long clientId, Long productId, String submittedOnDate, Double principal,
            Integer numberOfRepayments, Consumer<PostLoansRequest> customizer) {
        return LoanRequestBuilders.applyLoanRequest(clientId, productId, submittedOnDate, principal, numberOfRepayments, customizer);
    }

    protected PostLoansRequest applyLP2ProgressiveLoanRequest(Long clientId, Long loanProductId, String loanDisbursementDate, Double amount,
            Double interestRate, Integer numberOfRepayments, java.util.function.Consumer<PostLoansRequest> customizer) {
        return LoanRequestBuilders.applyLP2ProgressiveLoanRequest(clientId, loanProductId, loanDisbursementDate, amount, interestRate,
                numberOfRepayments, customizer);
    }

    protected Long applyAndApproveLoan(Long clientId, Long productId, String date, Double amount) {
        return applyAndApproveLoan(clientId, productId, date, amount, 1, null);
    }

    protected Long applyAndApproveLoan(Long clientId, Long productId, String date, Double amount, int numberOfRepayments) {
        return applyAndApproveLoan(clientId, productId, date, amount, numberOfRepayments, null);
    }

    protected Long applyAndApproveLoan(Long clientId, Long productId, String date, Double amount, int numberOfRepayments,
            Consumer<PostLoansRequest> customizer) {
        PostLoansRequest request = LoanRequestBuilders.applyLoanRequest(clientId, productId, date, amount, numberOfRepayments, customizer);
        Long loanId = applyForLoan(request);
        approveLoan(loanId, LoanRequestBuilders.approveLoan(amount, date));
        return loanId;
    }

    protected Long applyAndApproveProgressiveLoan(Long clientId, Long productId, String date, Double amount, Double interestRate,
            int numberOfRepayments, Consumer<PostLoansRequest> customizer) {
        PostLoansRequest request = LoanRequestBuilders.applyLP2ProgressiveLoanRequest(clientId, productId, date, amount, interestRate,
                numberOfRepayments, customizer);
        Long loanId = applyForLoan(request);
        approveLoan(loanId, LoanRequestBuilders.approveLoan(amount, date));
        return loanId;
    }

    protected void disburseLoan(Long loanId, BigDecimal amount, String date) {
        disburseLoan(loanId, LoanRequestBuilders.disburseLoan(amount.doubleValue(), date));
    }

    protected Long addRepaymentForLoan(Long loanId, Double amount, String date) {
        return addRepayment(loanId, LoanRequestBuilders.repayLoan(amount, date));
    }

    protected void validateLoanSummaryBalances(GetLoansLoanIdResponse loanDetails, Double totalOutstanding, Double totalRepayment,
            Double principalOutstanding, Double principalPaid, Double totalOverpaid) {
        org.junit.jupiter.api.Assertions.assertEquals(totalOutstanding,
                org.apache.fineract.integrationtests.common.Utils.getDoubleValue(loanDetails.getSummary().getTotalOutstanding()));
        org.junit.jupiter.api.Assertions.assertEquals(totalRepayment,
                org.apache.fineract.integrationtests.common.Utils.getDoubleValue(loanDetails.getSummary().getTotalRepayment()));
        org.junit.jupiter.api.Assertions.assertEquals(principalOutstanding,
                org.apache.fineract.integrationtests.common.Utils.getDoubleValue(loanDetails.getSummary().getPrincipalOutstanding()));
        org.junit.jupiter.api.Assertions.assertEquals(principalPaid,
                org.apache.fineract.integrationtests.common.Utils.getDoubleValue(loanDetails.getSummary().getPrincipalPaid()));
        org.junit.jupiter.api.Assertions.assertEquals(totalOverpaid,
                org.apache.fineract.integrationtests.common.Utils.getDoubleValue(loanDetails.getTotalOverpaid()));
    }
}
