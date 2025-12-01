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
package com.adorsys.fineract.userandstaff.data;

import java.util.Collection;
import lombok.Data;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.data.RoleData;

@Data
public class EmployeeData {

    private final Long id;
    private final String username;
    private final Long officeId;
    private final String officeName;
    private final String firstname;
    private final String lastname;
    private final String email;
    private final Boolean passwordNeverExpires;
    private final Collection<OfficeData> allowedOffices;
    private final Collection<RoleData> availableRoles;
    private final Collection<RoleData> selectedRoles;
    private final StaffData staff;
    private final String mobileNo;
    private final boolean isLoanOfficer;
    private final String externalId;

    // The constructor is now public and will be called by the EmployeeDataMapper.
    public EmployeeData(Long id, String username, Long officeId, String officeName, String firstname, String lastname,
            String email, Boolean passwordNeverExpires, Collection<OfficeData> allowedOffices,
            Collection<RoleData> availableRoles, Collection<RoleData> selectedRoles, StaffData staff, String mobileNo,
            boolean isLoanOfficer, String externalId) {
        this.id = id;
        this.username = username;
        this.officeId = officeId;
        this.officeName = officeName;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.passwordNeverExpires = passwordNeverExpires;
        this.allowedOffices = allowedOffices;
        this.availableRoles = availableRoles;
        this.selectedRoles = selectedRoles;
        this.staff = staff;
        this.mobileNo = mobileNo;
        this.isLoanOfficer = isLoanOfficer;
        this.externalId = externalId;
    }
}