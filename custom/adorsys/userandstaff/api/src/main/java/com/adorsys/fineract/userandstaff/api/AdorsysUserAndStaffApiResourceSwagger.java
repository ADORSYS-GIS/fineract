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
package com.adorsys.fineract.userandstaff.api;



import io.swagger.v3.oas.annotations.media.Schema;

final class AdorsysUserAndStaffApiResourceSwagger {

    private AdorsysUserAndStaffApiResourceSwagger() {
    }

    @Schema(description = "CreateEmployeeRequest")
    public static final class CreateEmployeeRequest {
        private CreateEmployeeRequest() {}

        @Schema(example = "1")
        public Long officeId;
        @Schema(example = "John")
        public String firstname;
        @Schema(example = "Doe")
        public String lastname;
        @Schema(example = "01 January 2023")
        public String joiningDate;
        @Schema(example = "1234567890")
        public String mobileNo;
        @Schema(example = "true")
        public boolean isLoanOfficer;
        @Schema(example = "JD001")
        public String externalId;
        @Schema(example = "johndoe")
        public String username;
        @Schema(example = "johndoe@example.com")
        public String email;
        @Schema(example = "[1]")
        public Long[] roles;
    }

    @Schema(description = "CreateEmployeeResponse")
    public static final class CreateEmployeeResponse {
        private CreateEmployeeResponse() {}

        @Schema(example = "1")
        public Long officeId;
        @Schema(example = "1")
        public Long resourceId;
    }

    @Schema(description = "PutEmployeeRequest")
    public static final class PutEmployeeRequest {
        private PutEmployeeRequest() {}

        @Schema(example = "1")
        public Long officeId;
        @Schema(example = "John")
        public String firstname;
        @Schema(example = "Doe")
        public String lastname;
        @Schema(example = "1234567890")
        public String mobileNo;
        @Schema(example = "true")
        public boolean isLoanOfficer;
        @Schema(example = "JD001")
        public String externalId;
        @Schema(example = "[1]")
        public Long[] roles;
    }

    @Schema(description = "PutEmployeeResponse")
    public static final class PutEmployeeResponse {
        private PutEmployeeResponse() {}

        @Schema(example = "1")
        public Long officeId;
        @Schema(example = "1")
        public Long resourceId;
        public PutEmployeeResponseChanges changes;

        public static final class PutEmployeeResponseChanges {
            private PutEmployeeResponseChanges() {}
            @Schema(example = "1")
            public Long officeId;
            @Schema(example = "John")
            public String firstname;
            @Schema(example = "Doe")
            public String lastname;
        }
    }
}