package com.adorsys.fineract.organisation.monetary.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.organisation.monetary.data.CurrencyUpdateRequest;
import org.apache.fineract.organisation.monetary.data.CurrencyUpdateResponse;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepository;
import org.apache.fineract.organisation.monetary.domain.OrganisationCurrency;
import org.apache.fineract.organisation.monetary.domain.OrganisationCurrencyRepository;
import org.apache.fineract.organisation.monetary.exception.CurrencyInUseException;
import org.apache.fineract.organisation.monetary.service.CurrencyWritePlatformService;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsProductReadPlatformService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom CurrencyWritePlatformService that auto-creates unknown currencies in the m_currency reference table when they
 * are submitted via PUT /currencies. This enables tokenized asset platforms to register custom currency codes (e.g.
 * DTT, YMT) without needing direct database access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty("adorsys.currency.enabled")
public class CustomCurrencyWritePlatformService implements CurrencyWritePlatformService, InitializingBean {

    private final ApplicationCurrencyRepository currencyRepository;
    private final OrganisationCurrencyRepository organisationCurrencyRepository;
    private final LoanProductReadPlatformService loanProductService;
    private final SavingsProductReadPlatformService savingsProductService;
    private final ChargeReadPlatformService chargeService;

    @Override
    public void afterPropertiesSet() {
        log.info("Custom Currency Write Service active: auto-creation of unknown currencies enabled");
    }

    @Transactional
    @Override
    public CurrencyUpdateResponse updateAllowedCurrencies(final CurrencyUpdateRequest request) {
        final var currencies = request.getCurrencies();

        final List<String> allowedCurrencyCodes = new ArrayList<>();
        final Set<OrganisationCurrency> allowedCurrencies = new HashSet<>();

        for (final String currencyCode : currencies) {
            ApplicationCurrency currency = currencyRepository.findOneByCode(currencyCode);

            if (currency == null) {
                // Auto-create the currency in the reference table
                currency = new ApplicationCurrency(currencyCode, currencyCode, 0, 1, "currency." + currencyCode, currencyCode);
                currency = currencyRepository.save(currency);
                log.info("Auto-created custom currency in m_currency: {}", currencyCode);
            }

            final OrganisationCurrency allowedCurrency = currency.toOrganisationCurrency();
            allowedCurrencyCodes.add(currencyCode);
            allowedCurrencies.add(allowedCurrency);
        }

        // Validate that currencies being removed are not in use
        for (OrganisationCurrency priorCurrency : organisationCurrencyRepository.findAll()) {
            if (!allowedCurrencyCodes.contains(priorCurrency.getCode())) {
                if (!loanProductService.retrieveAllLoanProductsForCurrency(priorCurrency.getCode()).isEmpty()
                        || !savingsProductService.retrieveAllForCurrency(priorCurrency.getCode()).isEmpty()
                        || !chargeService.retrieveAllChargesForCurrency(priorCurrency.getCode()).isEmpty()) {
                    throw new CurrencyInUseException(priorCurrency.getCode());
                }
            }
        }

        organisationCurrencyRepository.deleteAll();
        organisationCurrencyRepository.saveAll(allowedCurrencies);

        return CurrencyUpdateResponse.builder().currencies(allowedCurrencyCodes).build();
    }
}
