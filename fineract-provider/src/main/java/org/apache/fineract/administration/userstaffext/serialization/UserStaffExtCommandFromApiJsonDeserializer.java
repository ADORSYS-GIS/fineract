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
package org.apache.fineract.administration.userstaffext.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validator for UserStaffExt create and update operations. Validates both user and staff fields
 * together.
 */
@Component
public final class UserStaffExtCommandFromApiJsonDeserializer {

    // User fields
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String REPEAT_PASSWORD = "repeatPassword";
    public static final String EMAIL = "email";
    public static final String ROLES = "roles";
    public static final String PASSWORD_NEVER_EXPIRES = "passwordNeverExpires";
    public static final String IS_SELF_SERVICE_USER = "isSelfServiceUser";
    public static final String SEND_PASSWORD_TO_EMAIL = "sendPasswordToEmail";

    // Staff fields
    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";
    public static final String MOBILE_NO = "mobileNo";
    public static final String EXTERNAL_ID = "externalId";
    public static final String IS_LOAN_OFFICER = "isLoanOfficer";
    public static final String IS_ACTIVE = "isActive";
    public static final String JOINING_DATE = "joiningDate";
    public static final String EMAIL_ADDRESS = "emailAddress";

    // Common fields
    public static final String OFFICE_ID = "officeId";
    public static final String LOCALE = "locale";
    public static final String DATE_FORMAT = "dateFormat";

    /**
     * The parameters supported for create command.
     */
    private static final Set<String> CREATE_SUPPORTED_PARAMETERS = new HashSet<>(
            Arrays.asList(USERNAME, PASSWORD, REPEAT_PASSWORD, EMAIL, ROLES, PASSWORD_NEVER_EXPIRES, IS_SELF_SERVICE_USER,
                    SEND_PASSWORD_TO_EMAIL, FIRSTNAME, LASTNAME, MOBILE_NO, EXTERNAL_ID, IS_LOAN_OFFICER, IS_ACTIVE, JOINING_DATE,
                    EMAIL_ADDRESS, OFFICE_ID, LOCALE, DATE_FORMAT));

    /**
     * The parameters supported for update command.
     */
    private static final Set<String> UPDATE_SUPPORTED_PARAMETERS = new HashSet<>(
            Arrays.asList(EMAIL, ROLES, PASSWORD_NEVER_EXPIRES, FIRSTNAME, LASTNAME, MOBILE_NO, EXTERNAL_ID, IS_LOAN_OFFICER, IS_ACTIVE,
                    JOINING_DATE, EMAIL_ADDRESS, LOCALE, DATE_FORMAT));

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public UserStaffExtCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    /**
     * Validates JSON for creating a new User-Staff entity.
     */
    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CREATE_SUPPORTED_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("userstaffext");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        // Validate User fields (required for create)
        final String username = this.fromApiJsonHelper.extractStringNamed(USERNAME, element);
        baseDataValidator.reset().parameter(USERNAME).value(username).notBlank().notExceedingLengthOf(100);

        final String password = this.fromApiJsonHelper.extractStringNamed(PASSWORD, element);
        baseDataValidator.reset().parameter(PASSWORD).value(password).notBlank();

        final String repeatPassword = this.fromApiJsonHelper.extractStringNamed(REPEAT_PASSWORD, element);
        baseDataValidator.reset().parameter(REPEAT_PASSWORD).value(repeatPassword).notBlank().equalToParameter(PASSWORD, password);

        final String email = this.fromApiJsonHelper.extractStringNamed(EMAIL, element);
        baseDataValidator.reset().parameter(EMAIL).value(email).notBlank().notExceedingLengthOf(100);

        // Validate roles array
        if (this.fromApiJsonHelper.parameterExists(ROLES, element)) {
            final JsonArray rolesArray = this.fromApiJsonHelper.extractJsonArrayNamed(ROLES, element);
            baseDataValidator.reset().parameter(ROLES).value(rolesArray).notNull().jsonArrayNotEmpty();
        }

        // Validate Staff fields (required for create)
        final String firstname = this.fromApiJsonHelper.extractStringNamed(FIRSTNAME, element);
        baseDataValidator.reset().parameter(FIRSTNAME).value(firstname).notBlank().notExceedingLengthOf(50);

        final String lastname = this.fromApiJsonHelper.extractStringNamed(LASTNAME, element);
        baseDataValidator.reset().parameter(LASTNAME).value(lastname).notBlank().notExceedingLengthOf(50);

        // Validate common fields
        final Long officeId = this.fromApiJsonHelper.extractLongNamed(OFFICE_ID, element);
        baseDataValidator.reset().parameter(OFFICE_ID).value(officeId).notNull().integerGreaterThanZero();

        // Optional fields
        if (this.fromApiJsonHelper.parameterExists(MOBILE_NO, element)) {
            final String mobileNo = this.fromApiJsonHelper.extractStringNamed(MOBILE_NO, element);
            baseDataValidator.reset().parameter(MOBILE_NO).value(mobileNo).ignoreIfNull().notExceedingLengthOf(50);
        }

        if (this.fromApiJsonHelper.parameterExists(EXTERNAL_ID, element)) {
            final String externalId = this.fromApiJsonHelper.extractStringNamed(EXTERNAL_ID, element);
            baseDataValidator.reset().parameter(EXTERNAL_ID).value(externalId).ignoreIfNull().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(EMAIL_ADDRESS, element)) {
            final String emailAddress = this.fromApiJsonHelper.extractStringNamed(EMAIL_ADDRESS, element);
            baseDataValidator.reset().parameter(EMAIL_ADDRESS).value(emailAddress).ignoreIfNull().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(IS_LOAN_OFFICER, element)) {
            final String loanOfficerFlag = this.fromApiJsonHelper.extractStringNamed(IS_LOAN_OFFICER, element);
            baseDataValidator.reset().parameter(IS_LOAN_OFFICER).trueOrFalseRequired(loanOfficerFlag);
        }

        if (this.fromApiJsonHelper.parameterExists(IS_ACTIVE, element)) {
            final String activeFlag = this.fromApiJsonHelper.extractStringNamed(IS_ACTIVE, element);
            baseDataValidator.reset().parameter(IS_ACTIVE).trueOrFalseRequired(activeFlag);
        }

        if (this.fromApiJsonHelper.parameterExists(PASSWORD_NEVER_EXPIRES, element)) {
            final String passwordNeverExpiresFlag = this.fromApiJsonHelper.extractStringNamed(PASSWORD_NEVER_EXPIRES, element);
            baseDataValidator.reset().parameter(PASSWORD_NEVER_EXPIRES).trueOrFalseRequired(passwordNeverExpiresFlag);
        }

        if (this.fromApiJsonHelper.parameterExists(IS_SELF_SERVICE_USER, element)) {
            final String isSelfServiceUserFlag = this.fromApiJsonHelper.extractStringNamed(IS_SELF_SERVICE_USER, element);
            baseDataValidator.reset().parameter(IS_SELF_SERVICE_USER).trueOrFalseRequired(isSelfServiceUserFlag);
        }

        if (this.fromApiJsonHelper.parameterExists(JOINING_DATE, element)) {
            final LocalDate joiningDate = this.fromApiJsonHelper.extractLocalDateNamed(JOINING_DATE, element);
            baseDataValidator.reset().parameter(JOINING_DATE).value(joiningDate).ignoreIfNull();
        }

        if (this.fromApiJsonHelper.parameterExists(DATE_FORMAT, element)) {
            final String dateFormat = this.fromApiJsonHelper.extractStringNamed(DATE_FORMAT, element);
            baseDataValidator.reset().parameter(DATE_FORMAT).value(dateFormat).notBlank();
        }

        if (this.fromApiJsonHelper.parameterExists(LOCALE, element)) {
            final String locale = this.fromApiJsonHelper.extractStringNamed(LOCALE, element);
            baseDataValidator.reset().parameter(LOCALE).value(locale).notBlank();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    /**
     * Validates JSON for updating an existing User-Staff entity.
     */
    public void validateForUpdate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, UPDATE_SUPPORTED_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("userstaffext");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        // User fields (all optional for update)
        if (this.fromApiJsonHelper.parameterExists(EMAIL, element)) {
            final String email = this.fromApiJsonHelper.extractStringNamed(EMAIL, element);
            baseDataValidator.reset().parameter(EMAIL).value(email).notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(ROLES, element)) {
            final JsonArray rolesArray = this.fromApiJsonHelper.extractJsonArrayNamed(ROLES, element);
            baseDataValidator.reset().parameter(ROLES).value(rolesArray).notNull().jsonArrayNotEmpty();
        }

        if (this.fromApiJsonHelper.parameterExists(PASSWORD_NEVER_EXPIRES, element)) {
            final String passwordNeverExpiresFlag = this.fromApiJsonHelper.extractStringNamed(PASSWORD_NEVER_EXPIRES, element);
            baseDataValidator.reset().parameter(PASSWORD_NEVER_EXPIRES).trueOrFalseRequired(passwordNeverExpiresFlag);
        }

        // Staff fields (all optional for update)
        if (this.fromApiJsonHelper.parameterExists(FIRSTNAME, element)) {
            final String firstname = this.fromApiJsonHelper.extractStringNamed(FIRSTNAME, element);
            baseDataValidator.reset().parameter(FIRSTNAME).value(firstname).notBlank().notExceedingLengthOf(50);
        }

        if (this.fromApiJsonHelper.parameterExists(LASTNAME, element)) {
            final String lastname = this.fromApiJsonHelper.extractStringNamed(LASTNAME, element);
            baseDataValidator.reset().parameter(LASTNAME).value(lastname).notBlank().notExceedingLengthOf(50);
        }

        if (this.fromApiJsonHelper.parameterExists(MOBILE_NO, element)) {
            final String mobileNo = this.fromApiJsonHelper.extractStringNamed(MOBILE_NO, element);
            baseDataValidator.reset().parameter(MOBILE_NO).value(mobileNo).notExceedingLengthOf(50);
        }

        if (this.fromApiJsonHelper.parameterExists(EXTERNAL_ID, element)) {
            final String externalId = this.fromApiJsonHelper.extractStringNamed(EXTERNAL_ID, element);
            baseDataValidator.reset().parameter(EXTERNAL_ID).value(externalId).notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(EMAIL_ADDRESS, element)) {
            final String emailAddress = this.fromApiJsonHelper.extractStringNamed(EMAIL_ADDRESS, element);
            baseDataValidator.reset().parameter(EMAIL_ADDRESS).value(emailAddress).notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.parameterExists(IS_LOAN_OFFICER, element)) {
            final String loanOfficerFlag = this.fromApiJsonHelper.extractStringNamed(IS_LOAN_OFFICER, element);
            baseDataValidator.reset().parameter(IS_LOAN_OFFICER).trueOrFalseRequired(loanOfficerFlag);
        }

        if (this.fromApiJsonHelper.parameterExists(IS_ACTIVE, element)) {
            final String activeFlag = this.fromApiJsonHelper.extractStringNamed(IS_ACTIVE, element);
            baseDataValidator.reset().parameter(IS_ACTIVE).trueOrFalseRequired(activeFlag);
        }

        if (this.fromApiJsonHelper.parameterExists(JOINING_DATE, element)) {
            final LocalDate joiningDate = this.fromApiJsonHelper.extractLocalDateNamed(JOINING_DATE, element);
            baseDataValidator.reset().parameter(JOINING_DATE).value(joiningDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(DATE_FORMAT, element)) {
            final String dateFormat = this.fromApiJsonHelper.extractStringNamed(DATE_FORMAT, element);
            baseDataValidator.reset().parameter(DATE_FORMAT).value(dateFormat).notBlank();
        }

        if (this.fromApiJsonHelper.parameterExists(LOCALE, element)) {
            final String locale = this.fromApiJsonHelper.extractStringNamed(LOCALE, element);
            baseDataValidator.reset().parameter(LOCALE).value(locale).notBlank();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }
}
