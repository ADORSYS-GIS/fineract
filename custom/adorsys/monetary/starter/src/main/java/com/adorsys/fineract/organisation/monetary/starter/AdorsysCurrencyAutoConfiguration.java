package com.adorsys.fineract.organisation.monetary.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan("com.adorsys.fineract.organisation.monetary")
@ConditionalOnProperty("adorsys.currency.enabled")
public class AdorsysCurrencyAutoConfiguration {}
