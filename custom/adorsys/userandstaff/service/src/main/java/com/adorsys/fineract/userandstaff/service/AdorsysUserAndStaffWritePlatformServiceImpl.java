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
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.service.StaffWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.apache.fineract.useradministration.service.AppUserWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AdorsysUserAndStaffWritePlatformServiceImpl implements AdorsysUserAndStaffWritePlatformService {

    private final StaffWritePlatformService staffWritePlatformService;
    private final AppUserWritePlatformService appUserWritePlatformService;
    private final AppUserRepository appUserRepository;
    private final FromJsonHelper fromJsonHelper;

    @Override
    @Transactional
    public CommandProcessingResult createEmployee(JsonCommand command) {
        final JsonObject jsonObject = command.parsedJson().getAsJsonObject();

        // 1. Segregate and create the staff member
        final JsonObject staffObject = new JsonObject();
        staffObject.add("officeId", jsonObject.get("officeId"));
        staffObject.add("firstname", jsonObject.get("firstname"));
        staffObject.add("lastname", jsonObject.get("lastname"));
        staffObject.add("joiningDate", jsonObject.get("joiningDate"));
        staffObject.add("mobileNo", jsonObject.get("mobileNo"));
        staffObject.add("isLoanOfficer", jsonObject.get("isLoanOfficer"));
        staffObject.add("externalId", jsonObject.get("externalId"));
        staffObject.addProperty("dateFormat", "dd MMMM yyyy");
        staffObject.addProperty("locale", "en");
        final String staffJson = this.fromJsonHelper.toJson(staffObject);
        final JsonElement staffJsonElement = this.fromJsonHelper.parse(staffJson);
        final JsonCommand staffCommand = JsonCommand.from(staffJson, staffJsonElement, fromJsonHelper, "STAFF", null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        final CommandProcessingResult staffResult = this.staffWritePlatformService.createStaff(staffCommand);

        // 2. Segregate, create the user, and link to the staff member
        final JsonObject userObject = new JsonObject();
        userObject.add("officeId", jsonObject.get("officeId"));
        userObject.add("username", jsonObject.get("username"));
        userObject.add("firstname", jsonObject.get("firstname"));
        userObject.add("lastname", jsonObject.get("lastname"));
        userObject.add("email", jsonObject.get("email"));
        userObject.add("roles", jsonObject.get("roles"));
        userObject.addProperty("staffId", staffResult.getResourceId());
        userObject.addProperty("sendPasswordToEmail", false);
        final String userJson = this.fromJsonHelper.toJson(userObject);
        final JsonElement userJsonElement = this.fromJsonHelper.parse(userJson);
        final JsonCommand userCommand = JsonCommand.from(userJson, userJsonElement, fromJsonHelper, "USER", null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        return this.appUserWritePlatformService.createUser(userCommand);
    }

    @Override
    @Transactional
    public CommandProcessingResult updateEmployee(Long userId, JsonCommand command) {
        final JsonObject jsonObject = command.parsedJson().getAsJsonObject();

        // 1. Segregate and update the user
        final JsonObject userObject = new JsonObject();
        if (jsonObject.has("username")) userObject.add("username", jsonObject.get("username"));
        if (jsonObject.has("firstname")) userObject.add("firstname", jsonObject.get("firstname"));
        if (jsonObject.has("lastname")) userObject.add("lastname", jsonObject.get("lastname"));
        if (jsonObject.has("email")) userObject.add("email", jsonObject.get("email"));
        if (jsonObject.has("roles")) userObject.add("roles", jsonObject.get("roles"));
        final String userJson = this.fromJsonHelper.toJson(userObject);
        final JsonElement userJsonElement = this.fromJsonHelper.parse(userJson);
        final JsonCommand userCommand = JsonCommand.from(userJson, userJsonElement, fromJsonHelper, "USER", userId, null, null, null,
                null, null, null, null, null, null, null, null, null);
        final CommandProcessingResult userResult = this.appUserWritePlatformService.updateUser(userId, userCommand);

        // 2. Segregate and update the staff member only if there are staff-related changes
        final JsonObject staffObject = new JsonObject();
        if (jsonObject.has("officeId")) staffObject.add("officeId", jsonObject.get("officeId"));
        if (jsonObject.has("firstname")) staffObject.add("firstname", jsonObject.get("firstname"));
        if (jsonObject.has("lastname")) staffObject.add("lastname", jsonObject.get("lastname"));
        if (jsonObject.has("joiningDate")) staffObject.add("joiningDate", jsonObject.get("joiningDate"));
        if (jsonObject.has("mobileNo")) staffObject.add("mobileNo", jsonObject.get("mobileNo"));
        if (jsonObject.has("isLoanOfficer")) staffObject.add("isLoanOfficer", jsonObject.get("isLoanOfficer"));
        if (jsonObject.has("externalId")) staffObject.add("externalId", jsonObject.get("externalId"));

        if (staffObject.size() > 0) {
            final AppUser user = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
            final Staff staff = user.getStaff();
            if (staff != null) {
                final Long staffId = staff.getId();
                final String staffJson = this.fromJsonHelper.toJson(staffObject);
                final JsonElement staffJsonElement = this.fromJsonHelper.parse(staffJson);
                final JsonCommand staffCommand = JsonCommand.from(staffJson, staffJsonElement, fromJsonHelper, "STAFF", staffId, null, null,
                        null, null, null, null, null, null, null, null, null, null);
                return this.staffWritePlatformService.updateStaff(staffId, staffCommand);
            }
        }

        return userResult;
    }
}