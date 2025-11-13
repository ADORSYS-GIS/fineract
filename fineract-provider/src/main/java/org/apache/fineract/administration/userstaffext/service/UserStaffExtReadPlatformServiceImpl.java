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
package org.apache.fineract.administration.userstaffext.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.administration.userstaffext.data.UserStaffExtData;
import org.apache.fineract.administration.userstaffext.exception.UserStaffExtNotFoundException;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.useradministration.data.RoleData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.service.RoleReadPlatformService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Implementation of {@link UserStaffExtReadPlatformService} using JDBC for efficient queries.
 */
@RequiredArgsConstructor
public class UserStaffExtReadPlatformServiceImpl implements UserStaffExtReadPlatformService {

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final AppUserRepository appUserRepository;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final RoleReadPlatformService roleReadPlatformService;

    @Override
    public Collection<UserStaffExtData> retrieveAll() {
        this.context.authenticatedUser();

        final UserStaffExtMapper mapper = new UserStaffExtMapper(this.roleReadPlatformService);
        final String sql = "select " + mapper.schema() + " where u.staff_id is not null and u.is_deleted = false order by u.username";

        return this.jdbcTemplate.query(sql, mapper); // NOSONAR
    }

    @Override
    public UserStaffExtData retrieveOne(final Long userId) {
        this.context.authenticatedUser();

        final AppUser user = this.appUserRepository.findById(userId).orElseThrow(() -> new UserStaffExtNotFoundException(userId));

        if (user.isDeleted()) {
            throw new UserStaffExtNotFoundException(userId);
        }

        if (user.getStaff() == null) {
            throw new UserStaffExtNotFoundException(userId);
        }

        final UserStaffExtMapper mapper = new UserStaffExtMapper(this.roleReadPlatformService);
        final String sql = "select " + mapper.schema() + " where u.id = ? and u.staff_id is not null and u.is_deleted = false";

        final Collection<UserStaffExtData> results = this.jdbcTemplate.query(sql, mapper, new Object[] { userId }); // NOSONAR

        if (results.isEmpty()) {
            throw new UserStaffExtNotFoundException(userId);
        }

        return results.iterator().next();
    }

    @Override
    public UserStaffExtData retrieveTemplate() {
        this.context.authenticatedUser();

        final Collection<OfficeData> offices = this.officeReadPlatformService.retrieveAllOfficesForDropdown();
        final Collection<RoleData> availableRoles = this.roleReadPlatformService.retrieveAllActiveRoles();

        return UserStaffExtData.template(offices, availableRoles);
    }

    private static final class UserStaffExtMapper implements RowMapper<UserStaffExtData> {

        private final RoleReadPlatformService roleReadPlatformService;

        UserStaffExtMapper(final RoleReadPlatformService roleReadPlatformService) {
            this.roleReadPlatformService = roleReadPlatformService;
        }

        @Override
        public UserStaffExtData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            // User fields
            final Long userId = rs.getLong("userId");
            final String username = rs.getString("username");
            final String email = rs.getString("email");
            final Boolean passwordNeverExpires = rs.getBoolean("passwordNeverExpires");
            final Boolean isSelfServiceUser = rs.getBoolean("isSelfServiceUser");
            final Boolean enabled = rs.getBoolean("enabled");

            // Staff fields
            final Long staffId = JdbcSupport.getLong(rs, "staffId");
            final String firstname = rs.getString("firstname");
            final String lastname = rs.getString("lastname");
            final String displayName = rs.getString("displayName");
            final String mobileNo = rs.getString("mobileNo");
            final String externalId = rs.getString("externalId");
            final Boolean isLoanOfficer = rs.getBoolean("isLoanOfficer");
            final Boolean isActive = rs.getBoolean("isActive");
            final LocalDate joiningDate = JdbcSupport.getLocalDate(rs, "joiningDate");
            final String emailAddress = rs.getString("emailAddress");

            // Common fields
            final Long officeId = JdbcSupport.getLong(rs, "officeId");
            final String officeName = rs.getString("officeName");

            // Get user roles
            final Collection<RoleData> selectedRoles = this.roleReadPlatformService.retrieveAppUserRoles(userId);

            return new UserStaffExtData(userId, username, email, passwordNeverExpires, isSelfServiceUser, enabled, selectedRoles, staffId,
                    firstname, lastname, displayName, mobileNo, externalId, isLoanOfficer, isActive, joiningDate, emailAddress, officeId,
                    officeName);
        }

        public String schema() {
            return " u.id as userId, u.username as username, u.email as email, "
                    + "u.password_never_expires as passwordNeverExpires, u.is_self_service_user as isSelfServiceUser, "
                    + "u.enabled as enabled, " + "s.id as staffId, s.firstname as firstname, s.lastname as lastname, "
                    + "s.display_name as displayName, s.mobile_no as mobileNo, s.external_id as externalId, "
                    + "s.is_loan_officer as isLoanOfficer, s.is_active as isActive, s.joining_date as joiningDate, "
                    + "s.email_address as emailAddress, " + "o.id as officeId, o.name as officeName "
                    + "from m_appuser u join m_staff s on s.id = u.staff_id " + "join m_office o on o.id = u.office_id ";
        }
    }
}
