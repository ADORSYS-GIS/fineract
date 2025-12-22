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
package com.adorsys.fineract.teller.service;

import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.accounting.common.AccountingConstants.FinancialActivity;
import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccount;
import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccountRepositoryWrapper;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.organisation.teller.data.CashierTransactionDataValidator;
import org.apache.fineract.organisation.teller.data.CashierTransactionsWithSummaryData;
import org.apache.fineract.organisation.teller.domain.Cashier;
import org.apache.fineract.organisation.teller.domain.CashierRepository;
import org.apache.fineract.organisation.teller.domain.CashierTransaction;
import org.apache.fineract.organisation.teller.domain.CashierTransactionRepository;
import org.apache.fineract.organisation.teller.domain.CashierTxnType;
import org.apache.fineract.organisation.teller.domain.TellerRepositoryWrapper;
import org.apache.fineract.organisation.teller.exception.CashierNotFoundException;
import org.apache.fineract.organisation.teller.serialization.TellerCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.teller.service.TellerManagementReadPlatformService;
import org.apache.fineract.organisation.teller.service.TellerWritePlatformServiceJpaImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;

@Service
@Primary
@Slf4j
public class CustomTellerWritePlatformServiceJpaImpl extends TellerWritePlatformServiceJpaImpl {

    private final TellerCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final CashierRepository cashierRepository;
    private final CashierTransactionRepository cashierTxnRepository;
    private final JournalEntryRepository glJournalEntryRepository;
    private final FinancialActivityAccountRepositoryWrapper financialActivityAccountRepositoryWrapper;
    private final CashierTransactionDataValidator cashierTransactionDataValidator;
    private final GLAccountRepository glAccountRepository;
    private final TellerManagementReadPlatformService tellerManagementReadPlatformService;

    public CustomTellerWritePlatformServiceJpaImpl(PlatformSecurityContext context,
            TellerCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            TellerRepositoryWrapper tellerRepositoryWrapper,
            org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper officeRepositoryWrapper,
            StaffRepository staffRepository,
            CashierRepository cashierRepository,
            CashierTransactionRepository cashierTxnRepository,
            JournalEntryRepository glJournalEntryRepository,
            FinancialActivityAccountRepositoryWrapper financialActivityAccountRepositoryWrapper,
            CashierTransactionDataValidator cashierTransactionDataValidator,
            GLAccountRepository glAccountRepository,
            TellerManagementReadPlatformService tellerManagementReadPlatformService) {
        super(context, fromApiJsonDeserializer, tellerRepositoryWrapper, officeRepositoryWrapper, staffRepository,
                cashierRepository, cashierTxnRepository, glJournalEntryRepository, financialActivityAccountRepositoryWrapper,
                cashierTransactionDataValidator);
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.cashierRepository = cashierRepository;
        this.cashierTxnRepository = cashierTxnRepository;
        this.glJournalEntryRepository = glJournalEntryRepository;
        this.financialActivityAccountRepositoryWrapper = financialActivityAccountRepositoryWrapper;
        this.cashierTransactionDataValidator = cashierTransactionDataValidator;
        this.glAccountRepository = glAccountRepository;
        this.tellerManagementReadPlatformService = tellerManagementReadPlatformService;
    }

    @Override
    public CommandProcessingResult allocateCashToCashier(final Long cashierId, JsonCommand command) {
        return doTransactionForCashier(cashierId, CashierTxnType.ALLOCATE, command);
    }
@Override
public CommandProcessingResult settleCashFromCashier(final Long cashierId, JsonCommand command) {

    this.cashierTransactionDataValidator.validateSettleCashAndCashOutTransactions(cashierId, command);

    return doTransactionForCashier(cashierId, CashierTxnType.SETTLE, command);
}

@Override
public CommandProcessingResult endOfDaySettlement(final Long cashierId, JsonCommand command) {
    log.info("Starting end-of-day settlement for cashier ID: {}", cashierId);
    try {
        // Validate end of day time (after 6:00 AM UTC for testing)
        LocalTime currentTime = LocalTime.now(ZoneOffset.UTC);
        LocalTime endOfDayTime = LocalTime.of(6, 0);
        log.info("Current time: {}, Required time: {}", currentTime, endOfDayTime);
        if (currentTime.isBefore(endOfDayTime)) {
            throw new PlatformDataIntegrityException("error.msg.end.of.day.settlement.not.allowed",
                    "End of day settlement is only allowed after 6:00 AM UTC", "time", currentTime.toString());
        }

        log.info("Time validation passed, retrieving cashier");
        final Cashier cashier = this.cashierRepository.findById(cashierId).orElseThrow(() -> new CashierNotFoundException(cashierId));
        this.fromApiJsonDeserializer.validateForCashTxnForCashier(command.json());

        // Get net cash
        String currencyCode = command.stringValueOfParameterNamed("currencyCode");
        log.info("Getting net cash for cashier {} with currency {}", cashierId, currencyCode);
        CashierTransactionsWithSummaryData summary = this.tellerManagementReadPlatformService
                .retrieveCashierTransactionsWithSummary(cashierId, false, null, null, currencyCode, null);
        BigDecimal netCash = summary.getNetCash();
        log.info("Net cash calculated: {}", netCash);

        BigDecimal settlementAmount = BigDecimal.ZERO;
        boolean hasShortage = false;
        BigDecimal shortageAmount = BigDecimal.ZERO;

        if (netCash.compareTo(BigDecimal.ZERO) >= 0) {
            // Positive or zero: settle all
            settlementAmount = netCash;
        } else {
            // Negative: shortage
            hasShortage = true;
            shortageAmount = netCash.negate(); // positive amount
        }

        // Create settlement transaction to record the end-of-day settlement
        CashierTransaction cashierTxn = CashierTransaction.fromJson(cashier, command);
        cashierTxn.setTxnType(CashierTxnType.SETTLE.getId());
        // Set the actual settlement amount (could be 0)
        cashierTxn.setTxnAmount(settlementAmount);
        this.cashierTxnRepository.save(cashierTxn);

        // Journal entries
        FinancialActivityAccount mainVaultAccount = this.financialActivityAccountRepositoryWrapper
                .findByFinancialActivityTypeWithNotFoundDetection(FinancialActivity.CASH_AT_MAINVAULT.getValue());
        FinancialActivityAccount tellerCashAccount = this.financialActivityAccountRepositoryWrapper
                .findByFinancialActivityTypeWithNotFoundDetection(FinancialActivity.CASH_AT_TELLER.getValue());

        final Office cashierOffice = cashier.getTeller().getOffice();
        final String transactionId = UUID.randomUUID().toString();

        if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Debit main vault, credit teller cash
            JournalEntry debitEntry = JournalEntry.createNew(cashierOffice, null, mainVaultAccount.getGlAccount(),
                    cashierTxn.getCurrencyCode(), transactionId, false, cashierTxn.getTxnDate(), JournalEntryType.DEBIT,
                    settlementAmount, "End of day settlement", null, null, null, null, null, null, null);
            JournalEntry creditEntry = JournalEntry.createNew(cashierOffice, null, tellerCashAccount.getGlAccount(),
                    cashierTxn.getCurrencyCode(), transactionId, false, cashierTxn.getTxnDate(), JournalEntryType.CREDIT,
                    settlementAmount, "End of day settlement", null, null, null, null, null, null, null);
            this.glJournalEntryRepository.saveAndFlush(debitEntry);
            this.glJournalEntryRepository.saveAndFlush(creditEntry);
        }

        if (hasShortage) {
            // Shortage: debit shortage account, credit teller cash
            GLAccount shortageAccount = this.glAccountRepository.findOneByGlCode("98")
                    .orElseThrow(() -> new PlatformDataIntegrityException("error.msg.gl.account.not.found",
                            "GL Account with code 98 not found", "glCode", "98"));

            String shortageTxnId = UUID.randomUUID().toString();
            JournalEntry debitShortage = JournalEntry.createNew(cashierOffice, null, shortageAccount,
                    currencyCode, shortageTxnId, false, command.localDateValueOfParameterNamed("txnDate"), JournalEntryType.DEBIT,
                    shortageAmount, "Cash shortage at end of day", null, null, null, null, null, null, null);
            JournalEntry creditTeller = JournalEntry.createNew(cashierOffice, null, tellerCashAccount.getGlAccount(),
                    currencyCode, shortageTxnId, false, command.localDateValueOfParameterNamed("txnDate"), JournalEntryType.CREDIT,
                    shortageAmount, "Cash shortage at end of day", null, null, null, null, null, null, null);
            this.glJournalEntryRepository.saveAndFlush(debitShortage);
            this.glJournalEntryRepository.saveAndFlush(creditTeller);
        }

        // Delete cashier assignment
        this.cashierRepository.delete(cashier);

        return new CommandProcessingResultBuilder()
                .withCommandId(command.commandId())
                .withEntityId(cashier.getId())
                .withSubEntityId(cashierTxn != null ? cashierTxn.getId() : null)
                .build();
    } catch (final JpaSystemException | DataIntegrityViolationException dve) {
        handleTellerDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        return CommandProcessingResult.empty();
    } catch (final PersistenceException dve) {
        Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
        handleTellerDataIntegrityIssues(command, throwable, dve);
        return CommandProcessingResult.empty();
    }
}

private CommandProcessingResult doTransactionForCashier(final Long cashierId, final CashierTxnType txnType, JsonCommand command) {
    try {
        final Cashier cashier = this.cashierRepository.findById(cashierId).orElseThrow(() -> new CashierNotFoundException(cashierId));
        this.fromApiJsonDeserializer.validateForCashTxnForCashier(command.json());

        final CashierTransaction cashierTxn = CashierTransaction.fromJson(cashier, command);
            cashierTxn.setTxnType(txnType.getId());

            this.cashierTxnRepository.saveAndFlush(cashierTxn);

            // Pass the journal entries
            FinancialActivityAccount mainVaultFinancialActivityAccount = this.financialActivityAccountRepositoryWrapper
                    .findByFinancialActivityTypeWithNotFoundDetection(FinancialActivity.CASH_AT_MAINVAULT.getValue());
            FinancialActivityAccount tellerCashFinancialActivityAccount = this.financialActivityAccountRepositoryWrapper
                    .findByFinancialActivityTypeWithNotFoundDetection(FinancialActivity.CASH_AT_TELLER.getValue());
            GLAccount creditAccount = null;
            GLAccount debitAccount = null;
            if (txnType.equals(CashierTxnType.ALLOCATE)) {
                debitAccount = tellerCashFinancialActivityAccount.getGlAccount();
                creditAccount = mainVaultFinancialActivityAccount.getGlAccount();
            } else if (txnType.equals(CashierTxnType.SETTLE)) {
                debitAccount = mainVaultFinancialActivityAccount.getGlAccount();
                creditAccount = tellerCashFinancialActivityAccount.getGlAccount();
            }

            final Office cashierOffice = cashier.getTeller().getOffice();

            final String transactionId = UUID.randomUUID().toString();

            final JournalEntry debitJournalEntry = JournalEntry.createNew(cashierOffice, null, // payment
                                                                                                // detail
                    debitAccount, cashierTxn.getCurrencyCode(),

                    transactionId, false, // manual entry
                    cashierTxn.getTxnDate(), JournalEntryType.DEBIT, cashierTxn.getTxnAmount(), cashierTxn.getTxnNote(), // Description
                    null, null, null, // entity Type, entityId, reference number
                    null, null, null, null); // Loan
                                              // and
                                              // Savings
                                              // Txn

            final JournalEntry creditJournalEntry = JournalEntry.createNew(cashierOffice, null, // payment
                                                                                                 // detail
                    creditAccount, cashierTxn.getCurrencyCode(),

                    transactionId, false, // manual entry
                    cashierTxn.getTxnDate(), JournalEntryType.CREDIT, cashierTxn.getTxnAmount(), cashierTxn.getTxnNote(), // Description
                    null, null, null, // entity Type, entityId, reference number
                    null, null, null, null); // Loan
                                              // and
                                              // Savings
                                              // Txn

            this.glJournalEntryRepository.saveAndFlush(debitJournalEntry);
            this.glJournalEntryRepository.saveAndFlush(creditJournalEntry);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(cashier.getId()) //
                    .withSubEntityId(cashierTxn.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleTellerDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleTellerDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void handleTellerDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("m_tellers_name_unq")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.teller.duplicate.name", "Teller with name `" + name + "` already exists",
                    "name", name);
        }

        log.error("Error occured.", dve);
        throw ErrorHandler.getMappable(dve, "error.msg.teller.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }

}
