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
package org.apache.fineract.administration.userstaffext.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for updating both User and Staff information.
 */
@Data
@NoArgsConstructor
@Schema(description = "Request to update User and Staff together")
public class UpdateUserStaffExtRequest implements Serializable {

    // User fields
    @Schema(example = "john.doe@example.com", description = "Email address")
    private String email;

    @Schema(example = "[1, 2]", description = "List of role IDs to assign")
    private List<Long> roles;

    @Schema(example = "false", description = "Whether password never expires")
    private Boolean passwordNeverExpires;

    // Staff fields
    @Schema(example = "John", description = "Staff member's first name")
    private String firstname;

    @Schema(example = "Doe", description = "Staff member's last name")
    private String lastname;

    @Schema(example = "+1234567890", description = "Mobile phone number")
    private String mobileNo;

    @Schema(example = "EXT001", description = "External system ID")
    private String externalId;

    @Schema(example = "true", description = "Whether this staff member is a loan officer")
    private Boolean isLoanOfficer;

    @Schema(example = "true", description = "Whether this staff member is active")
    private Boolean isActive;

    @Schema(example = "01 January 2024", description = "Date the staff member joined")
    private String joiningDate;

    @Schema(example = "john.doe@example.com", description = "Staff email address")
    private String emailAddress;

    @Schema(example = "en", description = "Locale for date formatting")
    private String locale;

    @Schema(example = "dd MMMM yyyy", description = "Date format pattern")
    private String dateFormat;
}
