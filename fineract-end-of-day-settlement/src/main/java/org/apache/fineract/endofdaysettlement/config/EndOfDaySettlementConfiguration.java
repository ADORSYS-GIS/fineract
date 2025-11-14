package org.apache.fineract.endofdaysettlement.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties(EndOfDaySettlementProperties.class)
@ComponentScan(basePackages = "org.apache.fineract.endofdaysettlement")
@PropertySource("classpath:end-of-day-settlement.properties")
public class EndOfDaySettlementConfiguration {

}