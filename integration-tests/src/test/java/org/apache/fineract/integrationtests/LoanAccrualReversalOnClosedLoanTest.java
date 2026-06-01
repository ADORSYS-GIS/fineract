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
package org.apache.fineract.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.client.models.GetLoansLoanIdResponse;
import org.apache.fineract.client.models.GetLoansLoanIdTransactions;
import org.apache.fineract.client.models.PostClientsResponse;
import org.apache.fineract.client.models.PostLoanProductsRequest;
import org.apache.fineract.client.models.PostLoanProductsResponse;
import org.apache.fineract.client.models.PostLoansLoanIdTransactionsRequest;
import org.apache.fineract.client.models.PostLoansLoanIdTransactionsResponse;
import org.apache.fineract.client.models.PostLoansLoanIdTransactionsTransactionIdRequest;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.products.DelinquencyBucketsHelper;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.junit.jupiter.api.Test;

/**
 * Verifies that accrual transactions are never reversed (is_reversed=TRUE) on closed/overpaid loans.
 *
 * When income recognition needs correction, Fineract should create ACCRUAL_ADJUSTMENT (type 34) transactions instead of
 * reversing existing ACCRUAL transactions. Reversing accruals breaks downstream event reconciliation.
 */
@Slf4j
public class LoanAccrualReversalOnClosedLoanTest extends BaseLoanIntegrationTest {

    private static final String LOCALE = "en";

    /**
     * Reproduces the production scenario from 1. Loan disbursed, repayments made, loan closed 2. Goodwill credit
     * (backdated) makes loan overpaid 3. Merchant issued refund with interest refund calculation further overpays 4.
     * CBR to handle overpayment 5. CBR reversed and re-created 6. During reprocessing, accruals should NOT be reversed
     */
    @Test
    public void testNoAccrualReversalAfterCBRAdjustmentOnOverpaidLoan() {
        AtomicLong loanIdRef = new AtomicLong();
        AtomicReference<Long> clientIdRef = new AtomicReference<>();

        // Step 1: Create loan product with interest refund support and accrual activity posting
        runAt("01 November 2024", () -> {
            final PostClientsResponse client = clientHelper.createClient(ClientHelper.defaultClientCreationRequest());
            clientIdRef.set(client.getClientId());

            final PostLoanProductsResponse loanProduct = loanProductHelper.createLoanProduct(create4IProgressive()//
                    .currencyCode("USD")//
                    .principal(220.0).minPrincipal(100.0).maxPrincipal(1000.0)//
                    .numberOfRepayments(4).repaymentEvery(1)//
                    .interestRatePerPeriod(12.0).interestRateFrequencyType(3)//
                    .enableAccrualActivityPosting(true)//
                    .addSupportedInterestRefundTypesItem("MERCHANT_ISSUED_REFUND")//
                    .paymentAllocation(List.of(//
                            createDefaultPaymentAllocation("NEXT_INSTALLMENT"), //
                            createPaymentAllocation("MERCHANT_ISSUED_REFUND", "NEXT_INSTALLMENT"), //
                            createPaymentAllocation("GOODWILL_CREDIT", "LAST_INSTALLMENT")//
            ))//
            );

            Long loanId = applyAndApproveProgressiveLoan(clientIdRef.get(), loanProduct.getResourceId(), "01 November 2024", 220.0, 12.0, 4,
                    null);
            loanIdRef.set(loanId);
            disburseLoan(loanId, BigDecimal.valueOf(220.0), "01 November 2024");
            log.info("Loan disbursed: id={}, amount=220.0", loanId);
        });

        Long loanId = loanIdRef.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

        // Step 2: Run COB through November and make monthly repayments to close the loan
        runCobRange(loanId, fmt, LocalDate.of(2024, 11, 2), LocalDate.of(2024, 11, 30));

        payInstallment(loanId, 1, "01 December 2024");
        runCobRange(loanId, fmt, LocalDate.of(2024, 12, 2), LocalDate.of(2024, 12, 31));

        payInstallment(loanId, 2, "01 January 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 31));

        payInstallment(loanId, 3, "01 February 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 2, 2), LocalDate.of(2025, 2, 28));

        // Step 3: Final repayment closes the loan
        runAt("01 March 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            var installment = details.getRepaymentSchedule().getPeriods().stream().filter(p -> p.getPeriod() != null && p.getPeriod() == 4)
                    .findFirst().orElseThrow();
            double amount = Utils.getDoubleValue(installment.getTotalDueForPeriod());
            addRepaymentForLoan(loanId, amount, "01 March 2025");

            GetLoansLoanIdResponse loanAfterClose = loanTransactionHelper.getLoanDetails(loanId);
            assertTrue(loanAfterClose.getStatus().getClosedObligationsMet(),
                    "Loan should be CLOSED but is: " + loanAfterClose.getStatus().getCode());
            log.info("Loan closed on 01 March 2025");
        });

        runCobRange(loanId, fmt, LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 14));

        // Step 4: Goodwill credit (backdated) makes loan overpaid — mirrors production 10/29 goodwill credit
        runAt("15 March 2025", () -> {
            PostLoansLoanIdTransactionsResponse gwcResponse = loanTransactionHelper.makeGoodwillCredit(loanId,
                    new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("15 March 2025").locale(LOCALE)
                            .transactionAmount(0.5));
            assertNotNull(gwcResponse.getResourceId());

            GetLoansLoanIdResponse loanDetails = loanTransactionHelper.getLoanDetails(loanId);
            assertTrue(loanDetails.getStatus().getOverpaid(),
                    "Loan should be OVERPAID after goodwill credit but is: " + loanDetails.getStatus().getCode());
            log.info("Goodwill credit of 0.50 applied → loan OVERPAID");
        });

        // Step 5: Merchant issued refund with automatic interest refund — mirrors production merchant refunds
        runAt("16 March 2025", () -> {
            PostLoansLoanIdTransactionsResponse mirResponse = loanTransactionHelper.makeMerchantIssuedRefund(loanId,
                    new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("16 March 2025").locale(LOCALE)
                            .transactionAmount(100.0));
            assertNotNull(mirResponse.getResourceId());

            GetLoansLoanIdResponse loanDetails = loanTransactionHelper.getLoanDetails(loanId);
            assertTrue(loanDetails.getStatus().getOverpaid(), "Loan should still be OVERPAID but is: " + loanDetails.getStatus().getCode());
            log.info("Merchant issued refund of 100.0 → loan still OVERPAID");

            // Log all transactions after the refunds
            logAllTransactions(loanId, "After merchant refund");
        });

        // Step 6: Run COB, then create first CBR
        AtomicReference<Long> cbrTxnIdRef = new AtomicReference<>();
        runAt("17 March 2025", () -> {
            executeInlineCOB(loanId);

            // Get the overpayment amount
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            double overpayment = Utils.getDoubleValue(details.getTotalOverpaid());
            log.info("Total overpaid: {}", overpayment);
            assertTrue(overpayment > 0, "Should have overpayment");

            PostLoansLoanIdTransactionsResponse cbrResponse = loanTransactionHelper.makeCreditBalanceRefund(loanId,
                    new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("17 March 2025").locale(LOCALE)
                            .transactionAmount(overpayment));
            assertNotNull(cbrResponse.getResourceId());
            cbrTxnIdRef.set(cbrResponse.getResourceId());
            log.info("CBR created for {} → txn id: {}", overpayment, cbrResponse.getResourceId());

            logAllTransactions(loanId, "After first CBR");
        });

        // Step 7: Snapshot accrual state before the problematic reversal
        runAt("18 March 2025", () -> {
            executeInlineCOB(loanId);
            logAccrualState(loanId, "Before CBR reversal");
        });

        // Step 8: Reverse CBR and re-create — this is the trigger for the bug
        AtomicReference<Long> newCbrTxnIdRef = new AtomicReference<>();
        runAt("20 March 2025", () -> {
            Long cbrTxnId = cbrTxnIdRef.get();
            log.info("=== Reversing CBR transaction: {} ===", cbrTxnId);

            loanTransactionHelper.reverseLoanTransaction(loanId, cbrTxnId, new PostLoansLoanIdTransactionsTransactionIdRequest()
                    .dateFormat(DATETIME_PATTERN).transactionDate("20 March 2025").transactionAmount(0.0).locale(LOCALE));

            GetLoansLoanIdResponse afterReversal = loanTransactionHelper.getLoanDetails(loanId);
            log.info("Status after CBR reversal: {}", afterReversal.getStatus().getCode());
            logAccrualState(loanId, "After CBR reversal (before re-create)");

            // Re-create CBR with same amount
            double overpayment = Utils.getDoubleValue(afterReversal.getTotalOverpaid());
            log.info("Overpayment after CBR reversal: {}", overpayment);
            if (overpayment > 0) {
                PostLoansLoanIdTransactionsResponse newCbr = loanTransactionHelper.makeCreditBalanceRefund(loanId,
                        new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("20 March 2025")
                                .locale(LOCALE).transactionAmount(overpayment));
                assertNotNull(newCbr.getResourceId());
                newCbrTxnIdRef.set(newCbr.getResourceId());
                log.info("New CBR created: {}", newCbr.getResourceId());
            }

            logAccrualState(loanId, "After CBR reversal + re-create");
        });

        // Step 9: Second reversal cycle — mirrors the multi-cycle pattern from production
        runAt("25 March 2025", () -> {
            Long newCbrId = newCbrTxnIdRef.get();
            if (newCbrId != null) {
                log.info("=== Second CBR reversal cycle: {} ===", newCbrId);

                loanTransactionHelper.reverseLoanTransaction(loanId, newCbrId, new PostLoansLoanIdTransactionsTransactionIdRequest()
                        .dateFormat(DATETIME_PATTERN).transactionDate("25 March 2025").transactionAmount(0.0).locale(LOCALE));

                GetLoansLoanIdResponse afterReversal = loanTransactionHelper.getLoanDetails(loanId);
                double overpayment = Utils.getDoubleValue(afterReversal.getTotalOverpaid());
                if (overpayment > 0) {
                    loanTransactionHelper.makeCreditBalanceRefund(loanId, new PostLoansLoanIdTransactionsRequest()
                            .dateFormat(DATETIME_PATTERN).transactionDate("25 March 2025").locale(LOCALE).transactionAmount(overpayment));
                }
                logAccrualState(loanId, "After second CBR reversal cycle");
            }
        });

        // Step 10: Run COB and perform the critical assertion
        runAt("26 March 2025", () -> {
            executeInlineCOB(loanId);

            GetLoansLoanIdResponse loanDetails = loanTransactionHelper.getLoanDetails(loanId);
            assertNotNull(loanDetails.getTransactions());

            logAllTransactions(loanId, "Final state");

            // CRITICAL ASSERTION: No accrual transaction should be reversed
            assertNoReversedAccruals(loanDetails);

            log.info("Test completed. Loan status: {}", loanDetails.getStatus().getCode());
        });
    }

    /**
     * Simpler variant: just merchant refund + CBR reversal without goodwill credit and without interest refund support.
     * This serves as a baseline to confirm the simpler path doesn't trigger reversals.
     */
    @Test
    public void testNoAccrualReversalOnSimpleCBRReversalWithoutInterestRefund() {
        AtomicLong loanIdRef = new AtomicLong();

        runAt("01 November 2024", () -> {
            final PostClientsResponse client = clientHelper.createClient(ClientHelper.defaultClientCreationRequest());

            final PostLoanProductsResponse loanProduct = loanProductHelper.createLoanProduct(create4IProgressive()//
                    .currencyCode("USD").principal(220.0).minPrincipal(100.0).maxPrincipal(1000.0)//
                    .numberOfRepayments(4).repaymentEvery(1).interestRatePerPeriod(12.0).interestRateFrequencyType(3)//
                    .enableAccrualActivityPosting(true));

            Long loanId = applyAndApproveProgressiveLoan(client.getClientId(), loanProduct.getResourceId(), "01 November 2024", 220.0, 12.0,
                    4, null);
            loanIdRef.set(loanId);
            disburseLoan(loanId, BigDecimal.valueOf(220.0), "01 November 2024");
        });

        Long loanId = loanIdRef.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

        // Pay off the loan
        runCobRange(loanId, fmt, LocalDate.of(2024, 11, 2), LocalDate.of(2024, 11, 30));
        payInstallment(loanId, 1, "01 December 2024");
        runCobRange(loanId, fmt, LocalDate.of(2024, 12, 2), LocalDate.of(2024, 12, 31));
        payInstallment(loanId, 2, "01 January 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 31));
        payInstallment(loanId, 3, "01 February 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 2, 2), LocalDate.of(2025, 2, 28));

        runAt("01 March 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            var installment = details.getRepaymentSchedule().getPeriods().stream().filter(p -> p.getPeriod() != null && p.getPeriod() == 4)
                    .findFirst().orElseThrow();
            addRepaymentForLoan(loanId, Utils.getDoubleValue(installment.getTotalDueForPeriod()), "01 March 2025");
            assertTrue(loanTransactionHelper.getLoanDetails(loanId).getStatus().getClosedObligationsMet());
        });

        // Merchant refund → CBR → reverse CBR → new CBR
        AtomicReference<Long> cbrIdRef = new AtomicReference<>();
        runAt("15 March 2025", () -> {
            loanTransactionHelper.makeMerchantIssuedRefund(loanId, new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN)
                    .transactionDate("15 March 2025").locale(LOCALE).transactionAmount(50.0));
        });

        runAt("16 March 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            double overpayment = Utils.getDoubleValue(details.getTotalOverpaid());
            PostLoansLoanIdTransactionsResponse cbr = loanTransactionHelper.makeCreditBalanceRefund(loanId,
                    new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("16 March 2025").locale(LOCALE)
                            .transactionAmount(overpayment));
            cbrIdRef.set(cbr.getResourceId());
        });

        runAt("20 March 2025", () -> {
            loanTransactionHelper.reverseLoanTransaction(loanId, cbrIdRef.get(), new PostLoansLoanIdTransactionsTransactionIdRequest()
                    .dateFormat(DATETIME_PATTERN).transactionDate("20 March 2025").transactionAmount(0.0).locale(LOCALE));

            GetLoansLoanIdResponse afterReversal = loanTransactionHelper.getLoanDetails(loanId);
            double overpayment = Utils.getDoubleValue(afterReversal.getTotalOverpaid());
            if (overpayment > 0) {
                loanTransactionHelper.makeCreditBalanceRefund(loanId, new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN)
                        .transactionDate("20 March 2025").locale(LOCALE).transactionAmount(overpayment));
            }
        });

        runAt("21 March 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse loanDetails = loanTransactionHelper.getLoanDetails(loanId);
            assertNoReversedAccruals(loanDetails);
        });
    }

    /**
     * Progressive loan with backdated CBR re-creation — mirrors the production scenario where CBR was reversed on
     * 12/16/25 but re-created backdated to 10/31/25.
     */
    @Test
    public void testNoAccrualReversalWithBackdatedCBRRecreation() {
        AtomicLong loanIdRef = new AtomicLong();

        runAt("01 November 2024", () -> {
            final PostClientsResponse client = clientHelper.createClient(ClientHelper.defaultClientCreationRequest());

            final PostLoanProductsResponse loanProduct = loanProductHelper.createLoanProduct(create4IProgressive()//
                    .currencyCode("USD").principal(220.0).minPrincipal(100.0).maxPrincipal(1000.0)//
                    .numberOfRepayments(4).repaymentEvery(1).interestRatePerPeriod(12.0).interestRateFrequencyType(3)//
                    .enableAccrualActivityPosting(true)//
                    .addSupportedInterestRefundTypesItem("MERCHANT_ISSUED_REFUND")//
                    .paymentAllocation(List.of(//
                            createDefaultPaymentAllocation("NEXT_INSTALLMENT"), //
                            createPaymentAllocation("MERCHANT_ISSUED_REFUND", "NEXT_INSTALLMENT"), //
                            createPaymentAllocation("GOODWILL_CREDIT", "LAST_INSTALLMENT")//
            ))//
            );

            Long loanId = applyAndApproveProgressiveLoan(client.getClientId(), loanProduct.getResourceId(), "01 November 2024", 220.0, 12.0,
                    4, null);
            loanIdRef.set(loanId);
            disburseLoan(loanId, BigDecimal.valueOf(220.0), "01 November 2024");
            log.info("[Backdated CBR] Loan disbursed: id={}", loanId);
        });

        Long loanId = loanIdRef.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

        // Pay off the loan
        runCobRange(loanId, fmt, LocalDate.of(2024, 11, 2), LocalDate.of(2024, 11, 30));
        payInstallment(loanId, 1, "01 December 2024");
        runCobRange(loanId, fmt, LocalDate.of(2024, 12, 2), LocalDate.of(2024, 12, 31));
        payInstallment(loanId, 2, "01 January 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 31));
        payInstallment(loanId, 3, "01 February 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 2, 2), LocalDate.of(2025, 2, 28));

        runAt("01 March 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            var installment = details.getRepaymentSchedule().getPeriods().stream().filter(p -> p.getPeriod() != null && p.getPeriod() == 4)
                    .findFirst().orElseThrow();
            addRepaymentForLoan(loanId, Utils.getDoubleValue(installment.getTotalDueForPeriod()), "01 March 2025");
            assertTrue(loanTransactionHelper.getLoanDetails(loanId).getStatus().getClosedObligationsMet());
            log.info("[Backdated CBR] Loan closed on 01 March 2025");
        });

        runCobRange(loanId, fmt, LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 14));

        // Goodwill credit + merchant refund → overpaid
        runAt("15 March 2025", () -> {
            loanTransactionHelper.makeGoodwillCredit(loanId, new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN)
                    .transactionDate("15 March 2025").locale(LOCALE).transactionAmount(0.5));
            loanTransactionHelper.makeMerchantIssuedRefund(loanId, new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN)
                    .transactionDate("15 March 2025").locale(LOCALE).transactionAmount(100.0));
            assertTrue(loanTransactionHelper.getLoanDetails(loanId).getStatus().getOverpaid());
        });

        // CBR on March 16
        AtomicReference<Long> cbrIdRef = new AtomicReference<>();
        runAt("16 March 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            double overpayment = Utils.getDoubleValue(details.getTotalOverpaid());
            PostLoansLoanIdTransactionsResponse cbr = loanTransactionHelper.makeCreditBalanceRefund(loanId,
                    new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("16 March 2025").locale(LOCALE)
                            .transactionAmount(overpayment));
            cbrIdRef.set(cbr.getResourceId());
            logAccrualState(loanId, "After first CBR on March 16");
        });

        runCobRange(loanId, fmt, LocalDate.of(2025, 3, 17), LocalDate.of(2025, 3, 31));

        // Advance to April — reverse CBR and re-create it BACKDATED to March 16
        runAt("15 April 2025", () -> {
            log.info("[Backdated CBR] Reversing CBR {} and re-creating backdated to 16 March 2025", cbrIdRef.get());

            loanTransactionHelper.reverseLoanTransaction(loanId, cbrIdRef.get(), new PostLoansLoanIdTransactionsTransactionIdRequest()
                    .dateFormat(DATETIME_PATTERN).transactionDate("15 April 2025").transactionAmount(0.0).locale(LOCALE));

            logAccrualState(loanId, "After CBR reversal on April 15");

            GetLoansLoanIdResponse afterReversal = loanTransactionHelper.getLoanDetails(loanId);
            double overpayment = Utils.getDoubleValue(afterReversal.getTotalOverpaid());
            if (overpayment > 0) {
                // Re-create CBR backdated to original date — this is the production pattern
                loanTransactionHelper.makeCreditBalanceRefund(loanId,
                        new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("16 March 2025") // BACKDATED
                                .locale(LOCALE).transactionAmount(overpayment));
                log.info("[Backdated CBR] New CBR created backdated to 16 March 2025");
            }

            logAccrualState(loanId, "After backdated CBR re-creation");
        });

        runAt("16 April 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse loanDetails = loanTransactionHelper.getLoanDetails(loanId);
            logAllTransactions(loanId, "Final backdated CBR");
            assertNoReversedAccruals(loanDetails);
        });
    }

    /**
     * Cumulative loan WITHOUT compounding but with backdated CBR — tests the reprocessExistingAccruals path
     * specifically for cumulative loans during CBR reversal.
     */
    @Test
    public void testNoAccrualReversalOnCumulativeLoanWithBackdatedCBR() {
        AtomicLong loanIdRef = new AtomicLong();

        runAt("01 November 2024", () -> {
            final PostClientsResponse client = clientHelper.createClient(ClientHelper.defaultClientCreationRequest());

            final PostLoanProductsResponse loanProduct = loanProductHelper.createLoanProduct(create4ICumulative()//
                    .currencyCode("USD").principal(220.0).minPrincipal(100.0).maxPrincipal(1000.0)//
                    .numberOfRepayments(4).repaymentEvery(1).interestRatePerPeriod(12.0).interestRateFrequencyType(3)//
                    .enableAccrualActivityPosting(true)//
            );

            Long loanId = applyAndApproveCumulativeLoan(client.getClientId(), loanProduct.getResourceId(), "01 November 2024", 220.0, 12.0,
                    4, null);
            loanIdRef.set(loanId);
            disburseLoan(loanId, BigDecimal.valueOf(220.0), "01 November 2024");
            log.info("[Cumulative+Backdated] Loan disbursed: id={}", loanId);
        });

        Long loanId = loanIdRef.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

        runCobRange(loanId, fmt, LocalDate.of(2024, 11, 2), LocalDate.of(2024, 11, 30));
        payInstallment(loanId, 1, "01 December 2024");
        runCobRange(loanId, fmt, LocalDate.of(2024, 12, 2), LocalDate.of(2024, 12, 31));
        payInstallment(loanId, 2, "01 January 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 31));
        payInstallment(loanId, 3, "01 February 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 2, 2), LocalDate.of(2025, 2, 28));

        runAt("01 March 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            var installment = details.getRepaymentSchedule().getPeriods().stream().filter(p -> p.getPeriod() != null && p.getPeriod() == 4)
                    .findFirst().orElseThrow();
            addRepaymentForLoan(loanId, Utils.getDoubleValue(installment.getTotalDueForPeriod()), "01 March 2025");
            assertTrue(loanTransactionHelper.getLoanDetails(loanId).getStatus().getClosedObligationsMet());
            log.info("[Cumulative+Backdated] Loan closed");
        });

        runCobRange(loanId, fmt, LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 14));

        // Merchant refund → overpaid
        AtomicReference<Long> cbrIdRef = new AtomicReference<>();
        runAt("15 March 2025", () -> {
            loanTransactionHelper.makeMerchantIssuedRefund(loanId, new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN)
                    .transactionDate("15 March 2025").locale(LOCALE).transactionAmount(50.0));
            assertTrue(loanTransactionHelper.getLoanDetails(loanId).getStatus().getOverpaid());
        });

        // CBR
        runAt("16 March 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            double overpayment = Utils.getDoubleValue(details.getTotalOverpaid());
            PostLoansLoanIdTransactionsResponse cbr = loanTransactionHelper.makeCreditBalanceRefund(loanId,
                    new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("16 March 2025").locale(LOCALE)
                            .transactionAmount(overpayment));
            cbrIdRef.set(cbr.getResourceId());
            logAccrualState(loanId, "After CBR");
        });

        runCobRange(loanId, fmt, LocalDate.of(2025, 3, 17), LocalDate.of(2025, 3, 31));

        // Reverse and re-create backdated
        runAt("15 April 2025", () -> {
            log.info("[Cumulative+Backdated] Reversing CBR and re-creating backdated");
            loanTransactionHelper.reverseLoanTransaction(loanId, cbrIdRef.get(), new PostLoansLoanIdTransactionsTransactionIdRequest()
                    .dateFormat(DATETIME_PATTERN).transactionDate("15 April 2025").transactionAmount(0.0).locale(LOCALE));

            logAccrualState(loanId, "After CBR reversal");

            GetLoansLoanIdResponse afterReversal = loanTransactionHelper.getLoanDetails(loanId);
            double overpayment = Utils.getDoubleValue(afterReversal.getTotalOverpaid());
            if (overpayment > 0) {
                loanTransactionHelper.makeCreditBalanceRefund(loanId,
                        new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("16 March 2025") // BACKDATED
                                .locale(LOCALE).transactionAmount(overpayment));
            }
            logAccrualState(loanId, "After backdated CBR re-creation");
        });

        runAt("16 April 2025", () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse loanDetails = loanTransactionHelper.getLoanDetails(loanId);
            logAllTransactions(loanId, "Final cumulative+backdated");
            assertNoReversedAccruals(loanDetails);
        });
    }

    /**
     * REPRODUCTION: Cumulative loan where last installment interest is NOT accrued by COB, then loan becomes overpaid
     * after lastDueDate. The final accrual is placed at overpaidOnDate > lastDueDate. A subsequent CBR undo triggers
     * reprocessExistingAccruals → reverseTransactionsAfter(lastDueDate) which reverses the post-due-date accrual.
     *
     * Key mechanism: 1. Skip COB for last installment period → interest unaccrued 2. Overpay after lastDueDate →
     * processAccrualsOnLoanClosure creates ACCRUAL at overpaidOnDate > lastDueDate 3. CBR + CBR undo →
     * reprocessExistingAccruals → reverseTransactionsAfter(lastDueDate) → REVERSES the accrual
     */
    @Test
    public void testNoAccrualReversalOnCumulativeLoanClosedAfterLastDueDate() {
        AtomicLong loanIdRef = new AtomicLong();

        runAt("01 November 2024", () -> {
            final PostClientsResponse client = clientHelper.createClient(ClientHelper.defaultClientCreationRequest());

            final PostLoanProductsResponse loanProduct = loanProductHelper.createLoanProduct(create4ICumulative()//
                    .currencyCode("USD").principal(220.0).minPrincipal(100.0).maxPrincipal(1000.0)//
                    .numberOfRepayments(4).repaymentEvery(1).interestRatePerPeriod(12.0).interestRateFrequencyType(3)//
                    .enableAccrualActivityPosting(true)//
            );

            Long loanId = applyAndApproveCumulativeLoan(client.getClientId(), loanProduct.getResourceId(), "01 November 2024", 220.0, 12.0,
                    4, null);
            loanIdRef.set(loanId);
            disburseLoan(loanId, BigDecimal.valueOf(220.0), "01 November 2024");
            log.info("[AccrualFix] Loan disbursed: id={}", loanId);
        });

        Long loanId = loanIdRef.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

        // Pay installments 1-3 on time with COB — these get fully accrued
        runCobRange(loanId, fmt, LocalDate.of(2024, 11, 2), LocalDate.of(2024, 11, 30));
        payInstallment(loanId, 1, "01 December 2024");
        runCobRange(loanId, fmt, LocalDate.of(2024, 12, 2), LocalDate.of(2024, 12, 31));
        payInstallment(loanId, 2, "01 January 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 31));
        payInstallment(loanId, 3, "01 February 2025");

        // CRITICAL: Do NOT run COB for the last installment period (Feb 2 - Feb 28)
        // This leaves installment 4's interest unaccrued, so processAccrualsOnLoanClosure
        // will need to create a final accrual with non-zero interest amount
        log.info("[AccrualFix] Skipping COB for last period — installment 4 interest stays unaccrued");

        // Overpay AFTER lastDueDate (01 March) → overpaidOnDate > lastDueDate
        // For cumulative loans: accrualDate = overpaidOnDate (via getFinalAccrualTransactionDate)
        // Since installment 4 interest is unaccrued, the final accrual will have non-zero amount
        // and will be placed at overpaidOnDate (after lastDueDate) → POST-DUE-DATE ACCRUAL
        runAt("05 March 2025", () -> {
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            var installment = details.getRepaymentSchedule().getPeriods().stream().filter(p -> p.getPeriod() != null && p.getPeriod() == 4)
                    .findFirst().orElseThrow();
            double installmentAmount = Utils.getDoubleValue(installment.getTotalDueForPeriod());
            // Overpay by 50 to ensure OVERPAID status (extra covers any late interest)
            double overpayAmount = installmentAmount + 50.0;
            log.info("[AccrualFix] Installment 4 due: {}, paying: {} (overpaying by 50) on 05 March (due: 01 March)", installmentAmount,
                    overpayAmount);

            addRepaymentForLoan(loanId, overpayAmount, "05 March 2025");

            GetLoansLoanIdResponse afterPay = loanTransactionHelper.getLoanDetails(loanId);
            log.info("[AccrualFix] Status after overpayment: {}", afterPay.getStatus().getCode());
            assertTrue(afterPay.getStatus().getOverpaid(), "Loan should be OVERPAID but is: " + afterPay.getStatus().getCode());

            logAccrualState(loanId, "After overpayment — should have post-due-date accrual");
        });

        // CBR to refund overpayment
        AtomicReference<Long> cbrIdRef = new AtomicReference<>();
        runAt("15 March 2025", () -> {
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            double overpayment = Utils.getDoubleValue(details.getTotalOverpaid());
            log.info("[AccrualFix] Overpayment: {}, creating CBR", overpayment);
            assertTrue(overpayment > 0, "Should have overpayment");
            PostLoansLoanIdTransactionsResponse cbr = loanTransactionHelper.makeCreditBalanceRefund(loanId,
                    new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN).transactionDate("15 March 2025").locale(LOCALE)
                            .transactionAmount(overpayment));
            cbrIdRef.set(cbr.getResourceId());
            logAccrualState(loanId, "After CBR");
        });

        // Undo CBR → triggers reprocessExistingAccruals → reverseTransactionsAfter(lastDueDate)
        // The post-due-date accrual from the overpayment closure should get REVERSED here
        runAt("20 March 2025", () -> {
            log.info("[AccrualFix] === Undoing CBR {} ===", cbrIdRef.get());

            loanTransactionHelper.reverseLoanTransaction(loanId, cbrIdRef.get(), new PostLoansLoanIdTransactionsTransactionIdRequest()
                    .dateFormat(DATETIME_PATTERN).transactionDate("20 March 2025").transactionAmount(0.0).locale(LOCALE));

            logAccrualState(loanId, "After CBR undo — checking for accrual reversal");
            logAllTransactions(loanId, "After CBR undo");

            GetLoansLoanIdResponse loanDetails = loanTransactionHelper.getLoanDetails(loanId);

            // This assertion should FAIL if the bug exists — accruals should NOT be reversed
            assertNoReversedAccruals(loanDetails);
        });
    }

    /**
     * REPRODUCTION v2: Cumulative loan WITHOUT interest recalculation, closed after lastDueDate.
     *
     * Previous tests used loans with interest recalculation, which always creates additional schedule periods when
     * paying late, shifting lastDueDate to match the accrual date. This test uses a FIXED schedule (no interest
     * recalculation) so lastDueDate stays at the original installment date.
     *
     * Key mechanism: 1. Fixed-schedule cumulative loan (no interest recalculation) 2. Skip COB for last installment
     * period → interest unaccrued 3. Pay EXACT amount after lastDueDate → CLOSED with closedOnDate > lastDueDate 4.
     * processAccrualsOnLoanClosure creates ACCRUAL at closedOnDate (post-due-date) 5. Goodwill credit → OVERPAID →
     * LoanBalanceChangedListener → addAccruals(isFinal=true) → reverseTransactionsAfter(lastDueDate) → REVERSES the
     * post-due-date accrual (the bug)
     */
    @Test
    public void testNoAccrualReversalOnCumulativeLoanExactPaymentAfterDueDate() {
        AtomicLong loanIdRef = new AtomicLong();

        runAt("01 November 2024", () -> {
            final PostClientsResponse client = clientHelper.createClient(ClientHelper.defaultClientCreationRequest());

            // Cumulative loan WITHOUT interest recalculation → fixed schedule, no additional installments
            final PostLoanProductsResponse loanProduct = loanProductHelper.createLoanProduct(createFixedScheduleCumulative()//
                    .currencyCode("USD").principal(220.0).minPrincipal(100.0).maxPrincipal(1000.0)//
                    .numberOfRepayments(4).repaymentEvery(1).interestRatePerPeriod(12.0).interestRateFrequencyType(3)//
                    .enableAccrualActivityPosting(true)//
            );

            Long loanId = applyAndApproveCumulativeLoan(client.getClientId(), loanProduct.getResourceId(), "01 November 2024", 220.0, 12.0,
                    4, null);
            loanIdRef.set(loanId);
            disburseLoan(loanId, BigDecimal.valueOf(220.0), "01 November 2024");
            log.info("[AccrualFix] Loan disbursed: id={}", loanId);
        });

        Long loanId = loanIdRef.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

        // Pay installments 1-3 on time with COB
        runCobRange(loanId, fmt, LocalDate.of(2024, 11, 2), LocalDate.of(2024, 11, 30));
        payInstallment(loanId, 1, "01 December 2024");
        runCobRange(loanId, fmt, LocalDate.of(2024, 12, 2), LocalDate.of(2024, 12, 31));
        payInstallment(loanId, 2, "01 January 2025");
        runCobRange(loanId, fmt, LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 31));
        payInstallment(loanId, 3, "01 February 2025");

        // CRITICAL: Do NOT run COB for the last installment period
        log.info("[AccrualFix] Skipping COB for last period — installment 4 interest stays unaccrued");

        // Pay EXACT amount AFTER lastDueDate → CLOSED_OBLIGATIONS_MET
        // With no interest recalculation, the schedule is fixed — no additional installments for late payment
        // closedOnDate = March 5 > lastDueDate = March 1
        // addAccruals(isFinal=true) creates ACCRUAL at closedOnDate (post-due-date)
        runAt("05 March 2025", () -> {
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            var installment = details.getRepaymentSchedule().getPeriods().stream().filter(p -> p.getPeriod() != null && p.getPeriod() == 4)
                    .findFirst().orElseThrow();
            double installmentAmount = Utils.getDoubleValue(installment.getTotalDueForPeriod());
            log.info("[AccrualFix] Installment 4 due: {}, paying EXACT amount on 05 March (due: 01 March)", installmentAmount);

            addRepaymentForLoan(loanId, installmentAmount, "05 March 2025");

            GetLoansLoanIdResponse afterPay = loanTransactionHelper.getLoanDetails(loanId);
            log.info("[AccrualFix] Status after exact payment: {}", afterPay.getStatus().getCode());
            assertTrue(afterPay.getStatus().getClosedObligationsMet(), "Loan should be CLOSED but is: " + afterPay.getStatus().getCode());

            logAccrualState(loanId, "After exact payment — check for post-due-date accrual");

            // CHECK: Does a post-due-date accrual exist?
            LocalDate origLastDueDate = LocalDate.of(2025, 3, 1);
            List<GetLoansLoanIdTransactions> postDueAccruals = afterPay.getTransactions().stream()
                    .filter(tx -> "loanTransactionType.accrual".equals(tx.getType().getCode()))
                    .filter(tx -> tx.getDate() != null && tx.getDate().isAfter(origLastDueDate)).toList();
            log.info("[AccrualFix] Post-due-date accruals after exact payment: {} (expecting >= 1 at closedOnDate=March 5)",
                    postDueAccruals.size());
            for (var tx : postDueAccruals) {
                log.info("[AccrualFix]   id={}, date={}, amount={}, interest={}, reversed={}", tx.getId(), tx.getDate(), tx.getAmount(),
                        tx.getInterestPortion(), tx.getManuallyReversed());
            }

            // Also log the last 5 accruals by id to see the most recent ones
            List<GetLoansLoanIdTransactions> allAccruals = afterPay.getTransactions().stream()
                    .filter(tx -> "loanTransactionType.accrual".equals(tx.getType().getCode()))
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId())).limit(5).toList();
            log.info("[AccrualFix] Last 5 accruals by id:");
            for (var tx : allAccruals) {
                log.info("[AccrualFix]   id={}, date={}, amount={}, interest={}, reversed={}", tx.getId(), tx.getDate(), tx.getAmount(),
                        tx.getInterestPortion(), tx.getManuallyReversed());
            }

            // Verify no additional installment — fixed schedule should have exactly 4 periods
            long periodCount = afterPay.getRepaymentSchedule().getPeriods().stream().filter(p -> p.getPeriod() != null).count();
            log.info("[AccrualFix] Schedule periods: {} (expected 4)", periodCount);
            for (var p : afterPay.getRepaymentSchedule().getPeriods()) {
                if (p.getPeriod() != null) {
                    log.info("  Period {}: dueDate={}, complete={}", p.getPeriod(), p.getDueDate(), p.getComplete());
                }
            }
        });

        // Record the accrual count before goodwill credit — the post-due-date accrual at March 5 should exist
        AtomicReference<Long> accrualCountBefore = new AtomicReference<>();
        AtomicReference<Long> postDueAccrualIdRef = new AtomicReference<>();
        runAt("06 March 2025", () -> {
            GetLoansLoanIdResponse beforeGc = loanTransactionHelper.getLoanDetails(loanId);
            LocalDate origLastDueDate = LocalDate.of(2025, 3, 1);
            List<GetLoansLoanIdTransactions> accrualsBefore = beforeGc.getTransactions().stream()
                    .filter(tx -> "loanTransactionType.accrual".equals(tx.getType().getCode())).toList();
            accrualCountBefore.set((long) accrualsBefore.size());
            log.info("[AccrualFix] Accrual count before goodwill credit: {}", accrualsBefore.size());

            // Find the post-due-date accrual (should be at March 5)
            List<GetLoansLoanIdTransactions> postDueAccruals = accrualsBefore.stream()
                    .filter(tx -> tx.getDate() != null && tx.getDate().isAfter(origLastDueDate)).toList();
            log.info("[AccrualFix] Post-due-date accruals before goodwill credit: {}", postDueAccruals.size());
            assertTrue(postDueAccruals.size() >= 1,
                    "Expected at least 1 post-due-date accrual at closedOnDate=March 5 but found " + postDueAccruals.size());
            postDueAccrualIdRef.set(postDueAccruals.get(0).getId());
            log.info("[AccrualFix] Post-due-date accrual: id={}, date={}, amount={}", postDueAccruals.get(0).getId(),
                    postDueAccruals.get(0).getDate(), postDueAccruals.get(0).getAmount());
        });

        // Add goodwill credit → OVERPAID
        // LoanBalanceChangedListener → processAccrualsOnLoanClosure → addAccruals(isFinal=true)
        // line 341: reverseTransactionsAfter(lastDueDate=March1) → creates ACCRUAL_ADJUSTMENT instead of reversing
        runAt("10 March 2025", () -> {
            log.info("[AccrualFix] === Adding goodwill credit 10.0 ===");
            loanTransactionHelper.makeGoodwillCredit(loanId, new PostLoansLoanIdTransactionsRequest().dateFormat(DATETIME_PATTERN)
                    .transactionDate("10 March 2025").locale(LOCALE).transactionAmount(10.0));

            GetLoansLoanIdResponse afterGc = loanTransactionHelper.getLoanDetails(loanId);
            log.info("[AccrualFix] Status after goodwill credit: {}", afterGc.getStatus().getCode());

            List<GetLoansLoanIdTransactions> accrualsAfter = afterGc.getTransactions().stream()
                    .filter(tx -> "loanTransactionType.accrual".equals(tx.getType().getCode())).toList();
            log.info("[AccrualFix] Accrual count after goodwill credit: {} (was {} before)", accrualsAfter.size(),
                    accrualCountBefore.get());

            // FIX: The post-due-date accrual must remain visible (NOT reversed)
            boolean postDueAccrualStillVisible = accrualsAfter.stream().anyMatch(tx -> tx.getId().equals(postDueAccrualIdRef.get()));
            log.info("[AccrualFix] Post-due-date accrual id={} still visible: {}", postDueAccrualIdRef.get(), postDueAccrualStillVisible);
            assertTrue(postDueAccrualStillVisible, "Post-due-date accrual id=" + postDueAccrualIdRef.get()
                    + " should remain visible (not reversed). The fix creates ACCRUAL_ADJUSTMENT instead.");

            // The original accrual count should be preserved (accruals are never reversed)
            assertEquals(accrualCountBefore.get().longValue(), accrualsAfter.size(),
                    "Accrual count should not change — accruals must never be reversed");

            // An ACCRUAL_ADJUSTMENT should have been created to cancel the post-due-date accrual
            List<GetLoansLoanIdTransactions> accrualAdjustments = afterGc.getTransactions().stream()
                    .filter(tx -> "loanTransactionType.accrualAdjustment".equals(tx.getType().getCode())).toList();
            log.info("[AccrualFix] Accrual adjustments after goodwill credit: {}", accrualAdjustments.size());
            assertTrue(!accrualAdjustments.isEmpty(),
                    "Expected ACCRUAL_ADJUSTMENT transaction to be created instead of reversing the accrual");

            // Verify the ACCRUAL_ADJUSTMENT amount matches the original post-due-date accrual
            GetLoansLoanIdTransactions originalAccrual = accrualsAfter.stream().filter(tx -> tx.getId().equals(postDueAccrualIdRef.get()))
                    .findFirst().orElseThrow();
            GetLoansLoanIdTransactions adjustment = accrualAdjustments.get(accrualAdjustments.size() - 1);
            assertEquals(originalAccrual.getAmount(), adjustment.getAmount(),
                    "ACCRUAL_ADJUSTMENT amount should match the original post-due-date accrual amount");
        });
    }

    /**
     * Creates a cumulative loan product WITHOUT interest recalculation (fixed schedule). Same as create4ICumulative()
     * but with isInterestRecalculationEnabled=false.
     */
    protected PostLoanProductsRequest createFixedScheduleCumulative() {
        return new PostLoanProductsRequest().name(Utils.uniqueRandomStringGenerator("4I_CUM_FIXED_", 6))//
                .shortName(Utils.uniqueRandomStringGenerator("", 4))//
                .description("4 installment cumulative - fixed schedule")//
                .includeInBorrowerCycle(false)//
                .useBorrowerCycle(false)//
                .currencyCode("EUR")//
                .digitsAfterDecimal(2)//
                .principal(1000.0)//
                .minPrincipal(100.0)//
                .maxPrincipal(10000.0)//
                .numberOfRepayments(4)//
                .repaymentEvery(1)//
                .repaymentFrequencyType(RepaymentFrequencyType.MONTHS_L)//
                .interestRatePerPeriod(10D)//
                .minInterestRatePerPeriod(0D)//
                .maxInterestRatePerPeriod(120D)//
                .interestRateFrequencyType(InterestRateFrequencyType.YEARS)//
                .isLinkedToFloatingInterestRates(false)//
                .allowVariableInstallments(false)//
                .amortizationType(AmortizationType.EQUAL_INSTALLMENTS)//
                .interestType(InterestType.DECLINING_BALANCE)//
                .interestCalculationPeriodType(InterestCalculationPeriodType.DAILY)//
                .allowPartialPeriodInterestCalculation(false)//
                .creditAllocation(List.of())//
                .overdueDaysForNPA(179)//
                .daysInMonthType(30)//
                .daysInYearType(360)//
                .isInterestRecalculationEnabled(false)// NO interest recalculation → fixed schedule
                .canDefineInstallmentAmount(true)//
                .repaymentStartDateType(1)//
                .charges(List.of())//
                .principalVariationsForBorrowerCycle(List.of())//
                .interestRateVariationsForBorrowerCycle(List.of())//
                .numberOfRepaymentVariationsForBorrowerCycle(List.of())//
                .accountingRule(3)// ACCRUAL_PERIODIC
                .dateFormat(DATETIME_PATTERN)//
                .locale("en")//
                .canUseForTopup(false)//
                .fundSourceAccountId(fundSource.getAccountID().longValue())//
                .loanPortfolioAccountId(loansReceivableAccount.getAccountID().longValue())//
                .transfersInSuspenseAccountId(suspenseAccount.getAccountID().longValue())//
                .interestOnLoanAccountId(interestIncomeAccount.getAccountID().longValue())//
                .incomeFromFeeAccountId(feeIncomeAccount.getAccountID().longValue())//
                .incomeFromPenaltyAccountId(penaltyIncomeAccount.getAccountID().longValue())//
                .incomeFromRecoveryAccountId(recoveriesAccount.getAccountID().longValue())//
                .writeOffAccountId(writtenOffAccount.getAccountID().longValue())//
                .overpaymentLiabilityAccountId(overpaymentAccount.getAccountID().longValue())//
                .receivableInterestAccountId(interestReceivableAccount.getAccountID().longValue())//
                .receivableFeeAccountId(feeReceivableAccount.getAccountID().longValue())//
                .receivablePenaltyAccountId(penaltyReceivableAccount.getAccountID().longValue())//
                .incomeFromChargeOffInterestAccountId(interestIncomeChargeOffAccount.getAccountID().longValue())//
                .incomeFromChargeOffFeesAccountId(feeChargeOffAccount.getAccountID().longValue())//
                .chargeOffExpenseAccountId(chargeOffExpenseAccount.getAccountID().longValue())//
                .chargeOffFraudExpenseAccountId(chargeOffFraudExpenseAccount.getAccountID().longValue())//
                .incomeFromChargeOffPenaltyAccountId(penaltyChargeOffAccount.getAccountID().longValue())//
                .incomeFromGoodwillCreditInterestAccountId(goodwillIncomeAccount.getAccountID().longValue())//
                .incomeFromGoodwillCreditFeesAccountId(goodwillIncomeAccount.getAccountID().longValue())//
                .incomeFromGoodwillCreditPenaltyAccountId(goodwillIncomeAccount.getAccountID().longValue())//
                .goodwillCreditAccountId(goodwillExpenseAccount.getAccountID().longValue())//
                .delinquencyBucketId(DelinquencyBucketsHelper.createDefaultBucket())//
                .transactionProcessingStrategyCode(
                        LoanProductTestBuilder.DUE_PENALTY_FEE_INTEREST_PRINCIPAL_IN_ADVANCE_PRINCIPAL_PENALTY_FEE_INTEREST_STRATEGY)//
                .loanScheduleType(LoanScheduleType.CUMULATIVE.toString());
    }

    // --- Helper methods ---

    private void runCobRange(Long loanId, DateTimeFormatter fmt, LocalDate from, LocalDate to) {
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            final String dateStr = fmt.format(date);
            runAt(dateStr, () -> executeInlineCOB(loanId));
        }
    }

    private void payInstallment(Long loanId, int installmentNumber, String date) {
        runAt(date, () -> {
            executeInlineCOB(loanId);
            GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
            var installment = details.getRepaymentSchedule().getPeriods().stream()
                    .filter(p -> p.getPeriod() != null && p.getPeriod() == installmentNumber).findFirst().orElseThrow();
            double amount = Utils.getDoubleValue(installment.getTotalDueForPeriod());
            log.info("Paying installment #{}: {}", installmentNumber, amount);
            addRepaymentForLoan(loanId, amount, date);
        });
    }

    private void assertNoReversedAccruals(GetLoansLoanIdResponse loanDetails) {
        List<GetLoansLoanIdTransactions> allAccruals = loanDetails.getTransactions().stream()
                .filter(tx -> "loanTransactionType.accrual".equals(tx.getType().getCode())).toList();

        List<GetLoansLoanIdTransactions> reversedAccruals = allAccruals.stream().filter(tx -> Boolean.TRUE.equals(tx.getManuallyReversed()))
                .toList();

        // Log detailed breakdown: count by date and reversed status
        LocalDate lastDueDate = loanDetails.getRepaymentSchedule().getPeriods().stream().filter(p -> p.getPeriod() != null)
                .map(p -> p.getDueDate()).max(LocalDate::compareTo).orElse(null);
        long postDueDateCount = allAccruals.stream()
                .filter(tx -> tx.getDate() != null && lastDueDate != null && tx.getDate().isAfter(lastDueDate)).count();
        long postDueDateReversed = allAccruals.stream().filter(tx -> tx.getDate() != null && lastDueDate != null
                && tx.getDate().isAfter(lastDueDate) && Boolean.TRUE.equals(tx.getManuallyReversed())).count();
        log.info("Accrual analysis: total={}, reversed={}, lastDueDate={}, postDueDate={}, postDueDateReversed={}", allAccruals.size(),
                reversedAccruals.size(), lastDueDate, postDueDateCount, postDueDateReversed);

        // Log each post-due-date accrual
        allAccruals.stream().filter(tx -> tx.getDate() != null && lastDueDate != null && tx.getDate().isAfter(lastDueDate)).forEach(tx -> {
            log.info("  POST-DUE-DATE accrual: id={}, date={}, amount={}, interest={}, reversed={}", tx.getId(), tx.getDate(),
                    tx.getAmount(), tx.getInterestPortion(), tx.getManuallyReversed());
        });

        if (!reversedAccruals.isEmpty()) {
            StringBuilder msg = new StringBuilder("Found reversed accrual transactions:\n");
            for (GetLoansLoanIdTransactions reversed : reversedAccruals) {
                msg.append(String.format("  id=%d, date=%s, amount=%s%n", reversed.getId(), reversed.getDate(), reversed.getAmount()));
            }
            msg.append("Accruals should NEVER be reversed. Use ACCRUAL_ADJUSTMENT (type 34) instead.");
            assertEquals(0, reversedAccruals.size(), msg.toString());
        }

        log.info("Assertion passed: {} accrual transactions, {} reversed", allAccruals.size(), reversedAccruals.size());
    }

    private void logAccrualState(Long loanId, String label) {
        GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
        List<GetLoansLoanIdTransactions> accruals = details.getTransactions().stream()
                .filter(tx -> "loanTransactionType.accrual".equals(tx.getType().getCode())
                        || "loanTransactionType.accrualAdjustment".equals(tx.getType().getCode()))
                .toList();
        log.info("=== Accrual state [{}]: {} transactions ===", label, accruals.size());
        for (GetLoansLoanIdTransactions a : accruals) {
            log.info("  {}: id={}, date={}, amount={}, interest={}, reversed={}", a.getType().getValue(), a.getId(), a.getDate(),
                    a.getAmount(), a.getInterestPortion(), a.getManuallyReversed());
        }
    }

    private void logAllTransactions(Long loanId, String label) {
        GetLoansLoanIdResponse details = loanTransactionHelper.getLoanDetails(loanId);
        log.info("=== All transactions [{}] ===", label);
        for (GetLoansLoanIdTransactions tx : details.getTransactions()) {
            log.info("  {}: id={}, date={}, amount={}, reversed={}", tx.getType().getValue(), tx.getId(), tx.getDate(), tx.getAmount(),
                    tx.getManuallyReversed());
        }
    }
}
