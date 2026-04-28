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
package com.adorsys.fineract.userandstaff.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.staff.data.StaffCreateRequest;
import org.apache.fineract.organisation.staff.data.StaffCreateResponse;
import org.apache.fineract.organisation.staff.data.StaffUpdateRequest;
import org.apache.fineract.organisation.staff.data.StaffUpdateResponse;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.service.StaffWriteService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.apache.fineract.useradministration.service.AppUserWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AdorsysUserAndStaffWritePlatformServiceImpl implements AdorsysUserAndStaffWritePlatformService {

    private final StaffWriteService staffWriteService;
    private final AppUserWritePlatformService appUserWritePlatformService;
    private final AppUserRepository appUserRepository;
    private final FromJsonHelper fromJsonHelper;

    private static final String OFFICE_ID = "officeId";
    private static final String FIRSTNAME = "firstname";
    private static final String LASTNAME = "lastname";
    private static final String JOINING_DATE = "joiningDate";
    private static final String MOBILE_NO = "mobileNo";
    private static final String IS_LOAN_OFFICER = "isLoanOfficer";
    private static final String EXTERNAL_ID = "externalId";
    private static final String USERNAME = "username";
    private static final String EMAIL = "email";
    private static final String ROLES = "roles";

    @Override
    @Transactional
    public CommandProcessingResult createEmployee(JsonCommand command) {
        final JsonObject jsonObject = command.parsedJson().getAsJsonObject();

        final Long officeId = jsonObject.get(OFFICE_ID).getAsLong();
        final String firstname = jsonObject.get(FIRSTNAME).getAsString();
        final String lastname = jsonObject.get(LASTNAME).getAsString();
        final String joiningDate = jsonObject.get(JOINING_DATE).getAsString();
        final String mobileNo = jsonObject.has(MOBILE_NO) ? jsonObject.get(MOBILE_NO).getAsString() : null;
        final boolean isLoanOfficer = jsonObject.has(IS_LOAN_OFFICER) && jsonObject.get(IS_LOAN_OFFICER).getAsBoolean();
        final String externalId = jsonObject.has(EXTERNAL_ID) ? jsonObject.get(EXTERNAL_ID).getAsString() : null;

        StaffCreateRequest request = StaffCreateRequest.builder().officeId(officeId).firstname(firstname).lastname(lastname)
                .joiningDate(joiningDate).mobileNo(mobileNo).isLoanOfficer(isLoanOfficer).externalId(externalId).dateFormat("dd MMMM yyyy")
                .locale("en").build();

        final StaffCreateResponse staffResult = this.staffWriteService.createStaff(request);

        final JsonObject userObject = new JsonObject();
        userObject.add(OFFICE_ID, jsonObject.get(OFFICE_ID));
        userObject.add(USERNAME, jsonObject.get(USERNAME));
        userObject.add(FIRSTNAME, jsonObject.get(FIRSTNAME));
        userObject.add(LASTNAME, jsonObject.get(LASTNAME));
        userObject.add(EMAIL, jsonObject.get(EMAIL));
        userObject.add(ROLES, jsonObject.get(ROLES));
        userObject.addProperty("staffId", staffResult.getResourceId());
        userObject.addProperty("sendPasswordToEmail", false);
        final String userJson = this.fromJsonHelper.toJson(userObject);
        final JsonElement userJsonElement = this.fromJsonHelper.parse(userJson);
        final JsonCommand userCommand = JsonCommand.from(userJson, userJsonElement, fromJsonHelper, "USER", null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        return this.appUserWritePlatformService.createUser(userCommand);
    }

    @Override
    @Transactional
    public CommandProcessingResult updateEmployee(Long userId, JsonCommand command) {
        CommandProcessingResult result = updateUser(userId, command);

        CommandProcessingResult staffResult = updateStaff(userId, command);
        if (staffResult != null) {
            result = staffResult;
        }

        return result;
    }

    private CommandProcessingResult updateUser(Long userId, JsonCommand command) {
        final JsonObject jsonObject = command.parsedJson().getAsJsonObject();
        final JsonObject userObject = new JsonObject();
        if (jsonObject.has(USERNAME)) {
            userObject.add(USERNAME, jsonObject.get(USERNAME));
        }
        if (jsonObject.has(FIRSTNAME)) {
            userObject.add(FIRSTNAME, jsonObject.get(FIRSTNAME));
        }
        if (jsonObject.has(LASTNAME)) {
            userObject.add(LASTNAME, jsonObject.get(LASTNAME));
        }
        if (jsonObject.has(EMAIL)) {
            userObject.add(EMAIL, jsonObject.get(EMAIL));
        }
        if (jsonObject.has(ROLES)) {
            userObject.add(ROLES, jsonObject.get(ROLES));
        }
        if (jsonObject.has(OFFICE_ID)) {
            userObject.add(OFFICE_ID, jsonObject.get(OFFICE_ID));
        }

        if (userObject.size() == 0) {
            return CommandProcessingResult.empty();
        }

        final String userJson = this.fromJsonHelper.toJson(userObject);
        final JsonElement userJsonElement = this.fromJsonHelper.parse(userJson);
        final JsonCommand userCommand = JsonCommand.from(userJson, userJsonElement, fromJsonHelper, "USER", userId, null, null, null, null,
                null, null, null, null, null, null, null, null);
        return this.appUserWritePlatformService.updateUser(userId, userCommand);
    }

    private CommandProcessingResult updateStaff(Long userId, JsonCommand command) {
        final JsonObject jsonObject = command.parsedJson().getAsJsonObject();
        final StaffUpdateRequest request = new StaffUpdateRequest();
        boolean staffUpdateRequired = false;

        if (jsonObject.has(OFFICE_ID)) {
            request.setOfficeId(jsonObject.get(OFFICE_ID).getAsLong());
            staffUpdateRequired = true;
        }
        if (jsonObject.has(FIRSTNAME)) {
            request.setFirstname(jsonObject.get(FIRSTNAME).getAsString());
            staffUpdateRequired = true;
        }
        if (jsonObject.has(LASTNAME)) {
            request.setLastname(jsonObject.get(LASTNAME).getAsString());
            staffUpdateRequired = true;
        }
        if (jsonObject.has(JOINING_DATE)) {
            request.setJoiningDate(jsonObject.get(JOINING_DATE).getAsString());
            staffUpdateRequired = true;
        }
        if (jsonObject.has(MOBILE_NO)) {
            request.setMobileNo(jsonObject.get(MOBILE_NO).getAsString());
            staffUpdateRequired = true;
        }
        if (jsonObject.has(IS_LOAN_OFFICER)) {
            request.setIsLoanOfficer(jsonObject.get(IS_LOAN_OFFICER).getAsBoolean());
            staffUpdateRequired = true;
        }
        if (jsonObject.has(EXTERNAL_ID)) {
            request.setExternalId(jsonObject.get(EXTERNAL_ID).getAsString());
            staffUpdateRequired = true;
        }

        if (staffUpdateRequired) {
            final AppUser user = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
            final Staff staff = user.getStaff();
            if (staff != null) {
                final Long staffId = staff.getId();
                request.setId(staffId);
                final StaffUpdateResponse staffResult = this.staffWriteService.updateStaff(request);
                return new CommandProcessingResultBuilder() //
                        .withCommandId(command.commandId()) //
                        .withEntityId(staffResult.getResourceId()) //
                        .withOfficeId(staffResult.getOfficeId()) //
                        .with(staffResult.getChanges()) //
                        .build();
            }
        }
        return null;
    }
}
