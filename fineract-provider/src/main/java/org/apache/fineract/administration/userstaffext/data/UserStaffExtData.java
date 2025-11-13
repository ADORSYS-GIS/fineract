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

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.useradministration.data.RoleData;

/**
 * Immutable data object representing combined User and Staff information.
 */
@Data
@NoArgsConstructor
public class UserStaffExtData implements Serializable {

    // User fields
    private Long userId;
    private String username;
    private String email;
    private Boolean passwordNeverExpires;
    private Boolean isSelfServiceUser;
    private Boolean enabled;
    private Collection<RoleData> selectedRoles;

    // Staff fields
    private Long staffId;
    private String firstname;
    private String lastname;
    private String displayName;
    private String mobileNo;
    private String externalId;
    private Boolean isLoanOfficer;
    private Boolean isActive;
    private LocalDate joiningDate;
    private String emailAddress;

    // Common fields
    private Long officeId;
    private String officeName;

    // Template data (for create/edit forms)
    private Collection<OfficeData> allowedOffices;
    private Collection<RoleData> availableRoles;

    /**
     * Constructor for complete user-staff data
     */
    public UserStaffExtData(Long userId, String username, String email, Boolean passwordNeverExpires,
            Boolean isSelfServiceUser, Boolean enabled, Collection<RoleData> selectedRoles, Long staffId, String firstname,
            String lastname, String displayName, String mobileNo, String externalId, Boolean isLoanOfficer, Boolean isActive,
            LocalDate joiningDate, String emailAddress, Long officeId, String officeName) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordNeverExpires = passwordNeverExpires;
        this.isSelfServiceUser = isSelfServiceUser;
        this.enabled = enabled;
        this.selectedRoles = selectedRoles;
        this.staffId = staffId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.displayName = displayName;
        this.mobileNo = mobileNo;
        this.externalId = externalId;
        this.isLoanOfficer = isLoanOfficer;
        this.isActive = isActive;
        this.joiningDate = joiningDate;
        this.emailAddress = emailAddress;
        this.officeId = officeId;
        this.officeName = officeName;
    }

    /**
     * Creates template data with options for create/edit operations
     */
    public static UserStaffExtData template(Collection<OfficeData> allowedOffices, Collection<RoleData> availableRoles) {
        UserStaffExtData data = new UserStaffExtData();
        data.allowedOffices = allowedOffices;
        data.availableRoles = availableRoles;
        return data;
    }

    /**
     * Merges existing data with template options
     */
    public UserStaffExtData withTemplate(Collection<OfficeData> allowedOffices, Collection<RoleData> availableRoles) {
        this.allowedOffices = allowedOffices;
        this.availableRoles = availableRoles;
        return this;
    }
}
