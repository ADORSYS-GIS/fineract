package com.adorsys.fineract.organisation.monetary.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepository;
import org.apache.fineract.organisation.monetary.exception.CurrencyNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST endpoint for managing custom (non-ISO) currencies.
 * Provides DELETE for cleanup during asset provisioning rollback.
 */
@Slf4j
@Path("/v1/currencies/custom")
@Component
@RequiredArgsConstructor
@ConditionalOnProperty("adorsys.currency.enabled")
@Tag(name = "Custom Currency", description = "Manage custom token currencies for asset tokenization.")
public class CustomCurrencyApiResource {

    private final ApplicationCurrencyRepository currencyRepository;

    @DELETE
    @Path("/{currencyCode}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Transactional
    @Operation(summary = "Delete Custom Currency",
            description = "Removes a custom currency from the reference table. Used during asset rollback.")
    public void deleteCustomCurrency(@PathParam("currencyCode") final String currencyCode) {
        final ApplicationCurrency currency = currencyRepository.findOneByCode(currencyCode);
        if (currency == null) {
            throw new CurrencyNotFoundException(currencyCode);
        }
        currencyRepository.delete(currency);
        log.info("Deleted custom currency: {}", currencyCode);
    }
}
