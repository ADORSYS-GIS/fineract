package com.adorsys.fineract.batch.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan("com.adorsys.fineract.batch")
@ConditionalOnProperty(name = "adorsys.batch.enabled", havingValue = "true", matchIfMissing = true)
public class AdorsysBatchAutoConfiguration {
}
