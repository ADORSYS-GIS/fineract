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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.persistence.PersistenceException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.administration.userstaffext.serialization.UserStaffExtCommandFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.useradministration.domain.UserDomainService;
import org.apache.fineract.useradministration.exception.RoleNotFoundException;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link UserStaffExtWritePlatformService} that creates and updates User-Staff
 * entities together in atomic transactions.
 */
@Slf4j
@RequiredArgsConstructor
public class UserStaffExtWritePlatformServiceImpl implements UserStaffExtWritePlatformService {

    private final PlatformSecurityContext context;
    private final UserStaffExtCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final FromJsonHelper fromJsonHelper;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final StaffRepository staffRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserDomainService userDomainService;

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult createUserStaffExt(final JsonCommand command) {
        try {
            this.context.authenticatedUser();

            // Validate input
            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final JsonElement element = this.fromJsonHelper.parse(command.json());

            // Get the office (common to both user and staff)
            final Long officeId = command.longValueOfParameterNamed("officeId");
            final Office office = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);

            // Step 1: Create Staff first
            final Staff staff = createStaffFromCommand(command, office, element);
            this.staffRepository.saveAndFlush(staff);

            // Step 2: Create User and link to newly created Staff
            final Set<Role> roles = assembleSetOfRoles(element);
            final AppUser appUser = createUserFromCommand(command, office, staff, roles, element);

            final Boolean sendPasswordToEmail = command.booleanObjectValueOfParameterNamed("sendPasswordToEmail");
            this.userDomainService.create(appUser, sendPasswordToEmail);

            // Return both IDs
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(appUser.getId()) //
                    .withOfficeId(office.getId()) //
                    .withSubEntityId(staff.getId()) // Staff ID as sub-entity
                    .build();

        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final JpaSystemException | PersistenceException dve) {
            log.error("createUserStaffExt: JpaSystemException | PersistenceException", dve);
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult updateUserStaffExt(final Long userId, final JsonCommand command) {
        try {
            this.context.authenticatedUser();

            // Validate input
            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final JsonElement element = this.fromJsonHelper.parse(command.json());

            // Find the user
            final AppUser userForUpdate = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

            // Ensure user has a linked staff (required for this extension)
            final Staff staffForUpdate = userForUpdate.getStaff();
            if (staffForUpdate == null) {
                throw new PlatformDataIntegrityException("error.msg.userstaffext.user.has.no.staff",
                        "User with ID " + userId + " does not have a linked staff member", "userId", userId);
            }

            final Map<String, Object> changes = new HashMap<>();

            // Step 1: Update Staff fields
            final Map<String, Object> staffChanges = updateStaffFromCommand(staffForUpdate, command, element);
            if (!staffChanges.isEmpty()) {
                this.staffRepository.saveAndFlush(staffForUpdate);
                changes.putAll(staffChanges);
            }

            // Step 2: Update User fields
            final Map<String, Object> userChanges = updateUserFromCommand(userForUpdate, command, element);
            if (!userChanges.isEmpty()) {
                this.appUserRepository.saveAndFlush(userForUpdate);
                changes.putAll(userChanges);
            }

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(userId) //
                    .withOfficeId(userForUpdate.getOffice().getId()) //
                    .withSubEntityId(staffForUpdate.getId()) //
                    .with(changes) //
                    .build();

        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final JpaSystemException | PersistenceException dve) {
            log.error("updateUserStaffExt: JpaSystemException | PersistenceException", dve);
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    /**
     * Creates a Staff entity from the command
     */
    private Staff createStaffFromCommand(final JsonCommand command, final Office office, final JsonElement element) {
        // Build a JSON command for Staff creation
        final JsonObject staffJson = new JsonObject();
        staffJson.addProperty("officeId", office.getId());

        if (this.fromJsonHelper.parameterExists("firstname", element)) {
            staffJson.addProperty("firstname", this.fromJsonHelper.extractStringNamed("firstname", element));
        }
        if (this.fromJsonHelper.parameterExists("lastname", element)) {
            staffJson.addProperty("lastname", this.fromJsonHelper.extractStringNamed("lastname", element));
        }
        if (this.fromJsonHelper.parameterExists("mobileNo", element)) {
            staffJson.addProperty("mobileNo", this.fromJsonHelper.extractStringNamed("mobileNo", element));
        }
        if (this.fromJsonHelper.parameterExists("externalId", element)) {
            staffJson.addProperty("externalId", this.fromJsonHelper.extractStringNamed("externalId", element));
        }
        if (this.fromJsonHelper.parameterExists("emailAddress", element)) {
            staffJson.addProperty("emailAddress", this.fromJsonHelper.extractStringNamed("emailAddress", element));
        }
        if (this.fromJsonHelper.parameterExists("isLoanOfficer", element)) {
            staffJson.addProperty("isLoanOfficer", this.fromJsonHelper.extractBooleanNamed("isLoanOfficer", element));
        }
        if (this.fromJsonHelper.parameterExists("isActive", element)) {
            staffJson.addProperty("isActive", this.fromJsonHelper.extractBooleanNamed("isActive", element));
        }
        if (this.fromJsonHelper.parameterExists("joiningDate", element)) {
            staffJson.addProperty("joiningDate", this.fromJsonHelper.extractStringNamed("joiningDate", element));
        }
        if (this.fromJsonHelper.parameterExists("dateFormat", element)) {
            staffJson.addProperty("dateFormat", this.fromJsonHelper.extractStringNamed("dateFormat", element));
        }
        if (this.fromJsonHelper.parameterExists("locale", element)) {
            staffJson.addProperty("locale", this.fromJsonHelper.extractStringNamed("locale", element));
        }

        final JsonCommand staffCommand = JsonCommand.from(staffJson.toString(), element, this.fromJsonHelper, command.commandId(),
                command.getProductId(), command.getCreditBureauId(), command.getOrganisationCreditBureauId(), command.getJobName());

        return Staff.fromJson(office, staffCommand);
    }

    /**
     * Creates an AppUser entity from the command
     */
    private AppUser createUserFromCommand(final JsonCommand command, final Office office, final Staff staff, final Set<Role> roles,
            final JsonElement element) {
        // Build a JSON command for User creation
        final JsonObject userJson = new JsonObject();
        userJson.addProperty("officeId", office.getId());
        userJson.addProperty("staffId", staff.getId());

        if (this.fromJsonHelper.parameterExists("username", element)) {
            userJson.addProperty("username", this.fromJsonHelper.extractStringNamed("username", element));
        }
        if (this.fromJsonHelper.parameterExists("password", element)) {
            userJson.addProperty("password", this.fromJsonHelper.extractStringNamed("password", element));
        }
        if (this.fromJsonHelper.parameterExists("repeatPassword", element)) {
            userJson.addProperty("repeatPassword", this.fromJsonHelper.extractStringNamed("repeatPassword", element));
        }
        if (this.fromJsonHelper.parameterExists("email", element)) {
            userJson.addProperty("email", this.fromJsonHelper.extractStringNamed("email", element));
        }
        if (this.fromJsonHelper.parameterExists("firstname", element)) {
            userJson.addProperty("firstname", this.fromJsonHelper.extractStringNamed("firstname", element));
        }
        if (this.fromJsonHelper.parameterExists("lastname", element)) {
            userJson.addProperty("lastname", this.fromJsonHelper.extractStringNamed("lastname", element));
        }
        if (this.fromJsonHelper.parameterExists("passwordNeverExpires", element)) {
            userJson.addProperty("passwordNeverExpires", this.fromJsonHelper.extractBooleanNamed("passwordNeverExpires", element));
        }
        if (this.fromJsonHelper.parameterExists("isSelfServiceUser", element)) {
            userJson.addProperty("isSelfServiceUser", this.fromJsonHelper.extractBooleanNamed("isSelfServiceUser", element));
        }

        final JsonCommand userCommand = JsonCommand.from(userJson.toString(), element, this.fromJsonHelper, command.commandId(),
                command.getProductId(), command.getCreditBureauId(), command.getOrganisationCreditBureauId(), command.getJobName());

        return AppUser.fromJson(office, staff, roles, null, userCommand);
    }

    /**
     * Updates Staff fields from command
     */
    private Map<String, Object> updateStaffFromCommand(final Staff staff, final JsonCommand command, final JsonElement element) {
        final JsonObject staffJson = new JsonObject();

        if (this.fromJsonHelper.parameterExists("firstname", element)) {
            staffJson.addProperty("firstname", this.fromJsonHelper.extractStringNamed("firstname", element));
        }
        if (this.fromJsonHelper.parameterExists("lastname", element)) {
            staffJson.addProperty("lastname", this.fromJsonHelper.extractStringNamed("lastname", element));
        }
        if (this.fromJsonHelper.parameterExists("mobileNo", element)) {
            staffJson.addProperty("mobileNo", this.fromJsonHelper.extractStringNamed("mobileNo", element));
        }
        if (this.fromJsonHelper.parameterExists("externalId", element)) {
            staffJson.addProperty("externalId", this.fromJsonHelper.extractStringNamed("externalId", element));
        }
        if (this.fromJsonHelper.parameterExists("emailAddress", element)) {
            staffJson.addProperty("emailAddress", this.fromJsonHelper.extractStringNamed("emailAddress", element));
        }
        if (this.fromJsonHelper.parameterExists("isLoanOfficer", element)) {
            staffJson.addProperty("isLoanOfficer", this.fromJsonHelper.extractBooleanNamed("isLoanOfficer", element));
        }
        if (this.fromJsonHelper.parameterExists("isActive", element)) {
            staffJson.addProperty("isActive", this.fromJsonHelper.extractBooleanNamed("isActive", element));
        }
        if (this.fromJsonHelper.parameterExists("joiningDate", element)) {
            staffJson.addProperty("joiningDate", this.fromJsonHelper.extractStringNamed("joiningDate", element));
        }
        if (this.fromJsonHelper.parameterExists("dateFormat", element)) {
            staffJson.addProperty("dateFormat", this.fromJsonHelper.extractStringNamed("dateFormat", element));
        }
        if (this.fromJsonHelper.parameterExists("locale", element)) {
            staffJson.addProperty("locale", this.fromJsonHelper.extractStringNamed("locale", element));
        }

        if (staffJson.size() == 0) {
            return new HashMap<>();
        }

        final JsonCommand staffCommand = JsonCommand.from(staffJson.toString(), element, this.fromJsonHelper, command.commandId(),
                command.getProductId(), command.getCreditBureauId(), command.getOrganisationCreditBureauId(), command.getJobName());

        return staff.update(staffCommand);
    }

    /**
     * Updates User fields from command
     */
    private Map<String, Object> updateUserFromCommand(final AppUser user, final JsonCommand command, final JsonElement element) {
        final JsonObject userJson = new JsonObject();

        if (this.fromJsonHelper.parameterExists("email", element)) {
            userJson.addProperty("email", this.fromJsonHelper.extractStringNamed("email", element));
        }
        if (this.fromJsonHelper.parameterExists("passwordNeverExpires", element)) {
            userJson.addProperty("passwordNeverExpires", this.fromJsonHelper.extractBooleanNamed("passwordNeverExpires", element));
        }
        if (this.fromJsonHelper.parameterExists("roles", element)) {
            final JsonArray rolesArray = this.fromJsonHelper.extractJsonArrayNamed("roles", element);
            userJson.add("roles", rolesArray);
        }

        if (userJson.size() == 0) {
            return new HashMap<>();
        }

        final JsonCommand userCommand = JsonCommand.from(userJson.toString(), element, this.fromJsonHelper, command.commandId(),
                command.getProductId(), command.getCreditBureauId(), command.getOrganisationCreditBureauId(), command.getJobName());

        final Map<String, Object> changes = user.update(userCommand);

        // Handle role changes
        if (this.fromJsonHelper.parameterExists("roles", element)) {
            final Set<Role> newRoles = assembleSetOfRoles(element);
            user.updateRoles(newRoles);
            changes.put("roles", newRoles);
        }

        return changes;
    }

    /**
     * Assembles a set of Role entities from the roles array in JSON
     */
    private Set<Role> assembleSetOfRoles(final JsonElement element) {
        final Set<Role> allRoles = new HashSet<>();

        if (this.fromJsonHelper.parameterExists("roles", element)) {
            final JsonArray rolesArray = this.fromJsonHelper.extractJsonArrayNamed("roles", element);
            for (JsonElement roleElement : rolesArray) {
                final Long roleId = roleElement.getAsLong();
                final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
                allRoles.add(role);
            }
        }

        return allRoles;
    }

    /**
     * Handles data integrity violations and throws appropriate exceptions
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        final String realCauseMessage = realCause.getMessage();

        // User-related integrity violations
        if (realCauseMessage.contains("username_org")) {
            final String username = command.stringValueOfParameterNamed("username");
            throw new PlatformDataIntegrityException("error.msg.user.duplicate.username",
                    "User with username `" + username + "` already exists", "username", username);
        }

        // Staff-related integrity violations
        if (realCauseMessage.contains("external_id")) {
            final String externalId = command.stringValueOfParameterNamed("externalId");
            throw new PlatformDataIntegrityException("error.msg.staff.duplicate.externalId",
                    "Staff with externalId `" + externalId + "` already exists", "externalId", externalId);
        }

        if (realCauseMessage.contains("display_name")) {
            final String displayName = command.stringValueOfParameterNamed("firstname") + " "
                    + command.stringValueOfParameterNamed("lastname");
            throw new PlatformDataIntegrityException("error.msg.staff.duplicate.displayName",
                    "Staff with display name `" + displayName + "` already exists", "displayName", displayName);
        }

        log.error("Error occurred while creating/updating user-staff extension", dve);
        throw ErrorHandler.getMappable(dve, "error.msg.userstaffext.unknown.data.integrity.issue",
                "Unknown data integrity issue with user-staff resource.");
    }
}
