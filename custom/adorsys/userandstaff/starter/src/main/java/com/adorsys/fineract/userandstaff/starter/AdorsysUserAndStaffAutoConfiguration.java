package com.adorsys.fineract.userandstaff.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan({"com.adorsys.fineract.userandstaff.service", "com.adorsys.fineract.userandstaff.api"})
public class AdorsysUserAndStaffAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AdorsysUserAndStaffAutoConfiguration.class);

    public AdorsysUserAndStaffAutoConfiguration() {
        LOG.info("Custom User and Staff Auto-Configuration has been loaded.");
    }
}