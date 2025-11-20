package com.adorsys.fineract.userandstaff.service;

import com.adorsys.fineract.userandstaff.data.EmployeeData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.useradministration.data.AppUserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDataMapper {

    private final ToApiJsonSerializer<AppUserData> appUserJsonSerializer;
    private final ToApiJsonSerializer<StaffData> staffJsonSerializer;
    private final Gson gson = new Gson();

    @Autowired
    public EmployeeDataMapper(final ToApiJsonSerializer<AppUserData> appUserJsonSerializer,
            final ToApiJsonSerializer<StaffData> staffJsonSerializer) {
        this.appUserJsonSerializer = appUserJsonSerializer;
        this.staffJsonSerializer = staffJsonSerializer;
    }

    public Map<String, Object> map(AppUserData userData, StaffData staffData) {
        // Serialize AppUserData to JSON to access its fields
        String appUserJson = this.appUserJsonSerializer.serialize(userData);
        Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> appUserMap = this.gson.fromJson(appUserJson, typeOfMap);

        // Extract the required fields from the map
        Long id = ((Number) appUserMap.get("id")).longValue();
        String username = (String) appUserMap.get("username");
        Long officeId = ((Number) appUserMap.get("officeId")).longValue();
        String officeName = (String) appUserMap.get("officeName");
        String firstname = (String) appUserMap.get("firstname");
        String lastname = (String) appUserMap.get("lastname");
        String email = (String) appUserMap.get("email");
        Boolean passwordNeverExpires = (Boolean) appUserMap.get("passwordNeverExpires");
        Number staffIdNumber = (Number) appUserMap.get("staffId");
        Long staffId = staffIdNumber != null ? staffIdNumber.longValue() : null;


        // Extract staff-related fields
        String mobileNo = null;
        boolean isLoanOfficer = false;
        String externalId = null;

        if (staffData != null) {
            String staffJson = this.staffJsonSerializer.serialize(staffData);
            Map<String, Object> staffMap = this.gson.fromJson(staffJson, typeOfMap);
            mobileNo = (String) staffMap.get("mobileNo");
            isLoanOfficer = (Boolean) staffMap.get("isLoanOfficer");
            externalId = (String) staffMap.get("externalId");
        }

        // The collections are not exposed via getters, so we cannot access them directly.
        // For now, we will pass null. A more complete solution would require a custom query.
        // Create and return the new EmployeeData object
        EmployeeData employeeData = new EmployeeData(id, username, officeId, officeName, firstname, lastname, email, passwordNeverExpires,
                null, null, null, staffData, mobileNo, isLoanOfficer, externalId);
        Map<String, Object> result = new HashMap<>();
        result.put("employeeData", employeeData);
        result.put("staffId", staffId);
        return result;
    }
}