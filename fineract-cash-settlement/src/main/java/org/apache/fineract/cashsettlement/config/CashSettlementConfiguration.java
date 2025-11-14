package org.apache.fineract.cashsettlement.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties(CashSettlementProperties.class)
@ComponentScan(basePackages = "org.apache.fineract.cashsettlement")
@PropertySource("classpath:cash-settlement.properties")
public class CashSettlementConfiguration {

}