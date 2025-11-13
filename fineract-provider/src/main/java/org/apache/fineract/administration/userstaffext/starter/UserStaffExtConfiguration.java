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
package org.apache.fineract.administration.userstaffext.starter;

import org.apache.fineract.administration.userstaffext.serialization.UserStaffExtCommandFromApiJsonDeserializer;
import org.apache.fineract.administration.userstaffext.service.UserStaffExtReadPlatformService;
import org.apache.fineract.administration.userstaffext.service.UserStaffExtReadPlatformServiceImpl;
import org.apache.fineract.administration.userstaffext.service.UserStaffExtWritePlatformService;
import org.apache.fineract.administration.userstaffext.service.UserStaffExtWritePlatformServiceImpl;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.useradministration.domain.UserDomainService;
import org.apache.fineract.useradministration.service.RoleReadPlatformService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring Boot configuration for the User-Staff Extension module. This configuration provides beans
 * for reading and writing user-staff extension data.
 */
@Configuration
public class UserStaffExtConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserStaffExtReadPlatformService.class)
    public UserStaffExtReadPlatformService userStaffExtReadPlatformService(PlatformSecurityContext context, JdbcTemplate jdbcTemplate,
            AppUserRepository appUserRepository, OfficeReadPlatformService officeReadPlatformService,
            RoleReadPlatformService roleReadPlatformService) {
        return new UserStaffExtReadPlatformServiceImpl(context, jdbcTemplate, appUserRepository, officeReadPlatformService,
                roleReadPlatformService);
    }

    @Bean
    @ConditionalOnMissingBean(UserStaffExtWritePlatformService.class)
    public UserStaffExtWritePlatformService userStaffExtWritePlatformService(PlatformSecurityContext context,
            UserStaffExtCommandFromApiJsonDeserializer fromApiJsonDeserializer, FromJsonHelper fromJsonHelper,
            OfficeRepositoryWrapper officeRepositoryWrapper, StaffRepository staffRepository, AppUserRepository appUserRepository,
            RoleRepository roleRepository, UserDomainService userDomainService) {
        return new UserStaffExtWritePlatformServiceImpl(context, fromApiJsonDeserializer, fromJsonHelper, officeRepositoryWrapper,
                staffRepository, appUserRepository, roleRepository, userDomainService);
    }
}
