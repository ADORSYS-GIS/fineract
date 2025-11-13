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
package org.apache.fineract.administration.userstaffext.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Swagger documentation schemas for UserStaffExt API.
 */
public final class UserStaffExtApiResourceSwagger {

    private UserStaffExtApiResourceSwagger() {}

    @Schema(description = "PostUserStaffExtRequest")
    public static final class PostUserStaffExtRequest {

        private PostUserStaffExtRequest() {}

        @Schema(example = "jdoe", required = true)
        public String username;

        @Schema(example = "Password123!", required = true)
        public String password;

        @Schema(example = "Password123!", required = true)
        public String repeatPassword;

        @Schema(example = "john.doe@example.com", required = true)
        public String email;

        @Schema(example = "[1, 2]", required = true)
        public List<Long> roles;

        @Schema(example = "false")
        public Boolean passwordNeverExpires;

        @Schema(example = "false")
        public Boolean isSelfServiceUser;

        @Schema(example = "false")
        public Boolean sendPasswordToEmail;

        @Schema(example = "John", required = true)
        public String firstname;

        @Schema(example = "Doe", required = true)
        public String lastname;

        @Schema(example = "+1234567890")
        public String mobileNo;

        @Schema(example = "EXT001")
        public String externalId;

        @Schema(example = "true")
        public Boolean isLoanOfficer;

        @Schema(example = "true")
        public Boolean isActive;

        @Schema(example = "01 January 2024")
        public String joiningDate;

        @Schema(example = "john.doe@example.com")
        public String emailAddress;

        @Schema(example = "1", required = true)
        public Long officeId;

        @Schema(example = "en")
        public String locale;

        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
    }

    @Schema(description = "PostUserStaffExtResponse")
    public static final class PostUserStaffExtResponse {

        private PostUserStaffExtResponse() {}

        @Schema(example = "1")
        public Long officeId;

        @Schema(example = "100")
        public Long resourceId;

        @Schema(example = "50")
        public Long subResourceId;
    }

    @Schema(description = "PutUserStaffExtRequest")
    public static final class PutUserStaffExtRequest {

        private PutUserStaffExtRequest() {}

        @Schema(example = "john.doe@example.com")
        public String email;

        @Schema(example = "[1, 2]")
        public List<Long> roles;

        @Schema(example = "false")
        public Boolean passwordNeverExpires;

        @Schema(example = "John")
        public String firstname;

        @Schema(example = "Doe")
        public String lastname;

        @Schema(example = "+1234567890")
        public String mobileNo;

        @Schema(example = "EXT001")
        public String externalId;

        @Schema(example = "true")
        public Boolean isLoanOfficer;

        @Schema(example = "true")
        public Boolean isActive;

        @Schema(example = "01 January 2024")
        public String joiningDate;

        @Schema(example = "john.doe@example.com")
        public String emailAddress;

        @Schema(example = "en")
        public String locale;

        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
    }

    @Schema(description = "PutUserStaffExtResponse")
    public static final class PutUserStaffExtResponse {

        private PutUserStaffExtResponse() {}

        @Schema(example = "1")
        public Long officeId;

        @Schema(example = "100")
        public Long resourceId;

        @Schema(example = "50")
        public Long subResourceId;

        public Object changes;
    }

    @Schema(description = "GetUserStaffExtResponse")
    public static final class GetUserStaffExtResponse {

        private GetUserStaffExtResponse() {}

        @Schema(example = "100")
        public Long userId;

        @Schema(example = "jdoe")
        public String username;

        @Schema(example = "john.doe@example.com")
        public String email;

        @Schema(example = "false")
        public Boolean passwordNeverExpires;

        @Schema(example = "false")
        public Boolean isSelfServiceUser;

        @Schema(example = "true")
        public Boolean enabled;

        public List<RoleDataSwagger> selectedRoles;

        @Schema(example = "50")
        public Long staffId;

        @Schema(example = "John")
        public String firstname;

        @Schema(example = "Doe")
        public String lastname;

        @Schema(example = "John Doe")
        public String displayName;

        @Schema(example = "+1234567890")
        public String mobileNo;

        @Schema(example = "EXT001")
        public String externalId;

        @Schema(example = "true")
        public Boolean isLoanOfficer;

        @Schema(example = "true")
        public Boolean isActive;

        @Schema(example = "2024-01-01")
        public LocalDate joiningDate;

        @Schema(example = "john.doe@example.com")
        public String emailAddress;

        @Schema(example = "1")
        public Long officeId;

        @Schema(example = "Head Office")
        public String officeName;
    }

    @Schema(description = "GetUserStaffExtTemplateResponse")
    public static final class GetUserStaffExtTemplateResponse {

        private GetUserStaffExtTemplateResponse() {}

        public List<OfficeDataSwagger> allowedOffices;
        public List<RoleDataSwagger> availableRoles;
    }

    @Schema(description = "RoleData")
    public static final class RoleDataSwagger {

        private RoleDataSwagger() {}

        @Schema(example = "1")
        public Long id;

        @Schema(example = "Super User")
        public String name;

        @Schema(example = "This role provides all application permissions.")
        public String description;
    }

    @Schema(description = "OfficeData")
    public static final class OfficeDataSwagger {

        private OfficeDataSwagger() {}

        @Schema(example = "1")
        public Long id;

        @Schema(example = "Head Office")
        public String name;

        @Schema(example = "Head Office")
        public String nameDecorated;
    }
}
