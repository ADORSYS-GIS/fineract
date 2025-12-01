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

import com.adorsys.fineract.userandstaff.data.EmployeeData;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AdorsysUserAndStaffReadPlatformServiceImpl implements AdorsysUserAndStaffReadPlatformService {

    private final AppUserReadPlatformService appUserReadPlatformService;
    private final StaffReadPlatformService staffReadPlatformService;
    private final AppUserRepository appUserRepository;
    private final EmployeeDataMapper employeeDataMapper;

    @Override
    public EmployeeData retrieveEmployee(Long userId) {
        // 1. Retrieve the base user data DTO
        final AppUserData userData = this.appUserReadPlatformService.retrieveUser(userId);

        // 2. Retrieve the full user entity to get the reliable staff link
        final AppUser user = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        final Staff staff = user.getStaff();

        // 3. Retrieve staff data if a link exists
        StaffData staffData = null;
        if (staff != null) {
            staffData = this.staffReadPlatformService.retrieveStaff(staff.getId());
        }

        // 4. Map all data into the final EmployeeData object
        Map<String, Object> mappedData = this.employeeDataMapper.map(userData, staffData);
        return (EmployeeData) mappedData.get("employeeData");
    }
}