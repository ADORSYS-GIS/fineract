package org.apache.fineract.endofdaysettlement;

import org.apache.fineract.accounting.common.AccountingConstants;
import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccount;
import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccountRepositoryWrapper;
import org.apache.fineract.accounting.gl.domain.GLAccount;
import org.apache.fineract.accounting.gl.domain.GLJournalEntry;
import org.apache.fineract.accounting.gl.domain.GLJournalEntryRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.endofdaysettlement.config.EndOfDaySettlementProperties;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.teller.data.CashierTransactionData;
import org.apache.fineract.organisation.teller.domain.Cashier;
import org.apache.fineract.organisation.teller.domain.CashierRepository;
import org.apache.fineract.organisation.teller.exception.CashierNotFoundException;
import org.apache.fineract.organisation.teller.service.TellerManagementReadPlatformService;
import org.apache.fineract.organisation.teller.service.TellerWritePlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

@Service
public class EndOfDaySettlementService {

    private static final Logger log = LoggerFactory.getLogger(EndOfDaySettlementService.class);

    private final CashierRepository cashierRepository;
    private final TellerManagementReadPlatformService tellerManagementReadPlatformService;
    private final TellerWritePlatformService tellerWritePlatformService;
    private final GLJournalEntryRepository glJournalEntryRepository;
    private final FinancialActivityAccountRepositoryWrapper financialActivityAccountRepositoryWrapper;
    private final ExternalIdFactory externalIdFactory;
    private final EndOfDaySettlementProperties properties;

    @Autowired
    public EndOfDaySettlementService(CashierRepository cashierRepository, TellerManagementReadPlatformService tellerManagementReadPlatformService, TellerWritePlatformService tellerWritePlatformService, GLJournalEntryRepository glJournalEntryRepository, FinancialActivityAccountRepositoryWrapper financialActivityAccountRepositoryWrapper, ExternalIdFactory externalIdFactory, EndOfDaySettlementProperties properties) {
        this.cashierRepository = cashierRepository;
        this.tellerManagementReadPlatformService = tellerManagementReadPlatformService;
        this.tellerWritePlatformService = tellerWritePlatformService;
        this.glJournalEntryRepository = glJournalEntryRepository;
        this.financialActivityAccountRepositoryWrapper = financialActivityAccountRepositoryWrapper;
        this.externalIdFactory = externalIdFactory;
        this.properties = properties;
    }

    @Transactional
    public CommandProcessingResult settle(final Long cashierId, final JsonCommand command) {
        final Cashier cashier = this.cashierRepository.findById(cashierId)
                .orElseThrow(() -> new CashierNotFoundException(cashierId));

        final LocalDate today = LocalDate.now();
        final Collection<CashierTransactionData> cashierTransactions = this.tellerManagementReadPlatformService.retrieveCashierTransactions(cashierId, false, today, today, null, null).getPageItems();

        BigDecimal expectedSettlementAmount = BigDecimal.ZERO;
        for (final CashierTransactionData transaction : cashierTransactions) {
            expectedSettlementAmount = expectedSettlementAmount.add(transaction.getTxnAmount());
        }

        final BigDecimal actualSettlementAmount = command.bigDecimalValueOfParameterNamed("amount");

        final BigDecimal discrepancy = actualSettlementAmount.subtract(expectedSettlementAmount);

        if (discrepancy.compareTo(BigDecimal.ZERO) != 0) {
            createJournalEntryForDiscrepancy(cashier, discrepancy, command);
        }

        if (discrepancy.compareTo(BigDecimal.ZERO) > 0) {
            // Overage
            JsonCommand newCommand = new JsonCommand().fromJson(command.json());
            newCommand.addProperty("amount", expectedSettlementAmount);
            return this.tellerWritePlatformService.settleCashFromCashier(cashierId, newCommand);
        } else {
            // Shortage or no discrepancy
            return this.tellerWritePlatformService.settleCashFromCashier(cashierId, command);
        }
    }

    private void createJournalEntryForDiscrepancy(Cashier cashier, BigDecimal discrepancy, JsonCommand command) {
        final Office office = cashier.getTeller().getOffice();
        final LocalDate transactionDate = command.localDateValueOfParameterNamed("txnDate");

        final Long cashOverageAccountId = properties.getCashOverageAccountId();
        final Long cashShortageAccountId = properties.getCashShortageAccountId();

        if (cashOverageAccountId == null || cashShortageAccountId == null) {
            log.error("Cash overage or shortage account ID is not configured. Skipping journal entry creation.");
            return;
        }

        final FinancialActivityAccount tellerFinancialActivityAccount = this.financialActivityAccountRepositoryWrapper
                .findByFinancialActivityTypeWithNotFoundDetection(AccountingConstants.FinancialActivity.CASH_AT_TELLER.getValue());
        final GLAccount tellerGLAccount = tellerFinancialActivityAccount.getGlAccount();


        final GLAccount creditAccount;
        final GLAccount debitAccount;

        if (discrepancy.compareTo(BigDecimal.ZERO) > 0) {
            // Overage
            log.info("Cashier id: {}. Creating journal entry for overage of: {}", cashier.getId(), discrepancy);
            creditAccount = glJournalEntryRepository.getAccount(cashOverageAccountId);
            debitAccount = tellerGLAccount;
        } else {
            // Shortage
            log.info("Cashier id: {}. Creating journal entry for shortage of: {}", cashier.getId(), discrepancy.abs());
            creditAccount = tellerGLAccount;
            debitAccount = glJournalEntryRepository.getAccount(cashShortageAccountId);
        }

        final GLJournalEntry journalEntry = GLJournalEntry.createNew(office, null, transactionDate, command.stringValueOfParameterNamed("currencyCode"),
                "CASH_SETTLEMENT_DISCREPANCY", JournalEntryType.DEBIT.getValue().longValue(), discrepancy.abs(), "Cash settlement discrepancy",
                externalIdFactory.create(), cashier.getId(), null);

        journalEntry.addCreditEntry(creditAccount, discrepancy.abs());
        journalEntry.addDebitEntry(debitAccount, discrepancy.abs());

        glJournalEntryRepository.save(journalEntry);
    }
}
