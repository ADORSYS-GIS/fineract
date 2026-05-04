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

import com.adorsys.fineract.userandstaff.data.EmployeeData;
import com.adorsys.fineract.userandstaff.service.AdorsysUserAndStaffReadPlatformService;
import com.adorsys.fineract.userandstaff.service.AdorsysUserAndStaffWritePlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.google.gson.JsonElement;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.stereotype.Component;

@Path("/v1/adorsys/employees")
@Component
@Tag(name = "Employees", description = "Unified API for managing employees (Staff and AppUser)")
@RequiredArgsConstructor
public class AdorsysUserAndStaffApiResource {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "EMPLOYEE";

    private final PlatformSecurityContext context;
    private final AdorsysUserAndStaffWritePlatformService adorsysUserAndStaffWritePlatformService;
    private final AdorsysUserAndStaffReadPlatformService adorsysUserAndStaffReadPlatformService;
    private final DefaultToApiJsonSerializer<EmployeeData> toApiJsonSerializer;
    private final FromJsonHelper fromJsonHelper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create an employee", description = "Creates an employee.\n\nMandatory Fields:\nofficeId, firstname, lastname, username, roles\n\nOptional Fields:\nisLoanOfficer, mobileNo, externalId, joiningDate")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AdorsysUserAndStaffApiResourceSwagger.CreateEmployeeRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AdorsysUserAndStaffApiResourceSwagger.CreateEmployeeResponse.class)))
    })
    public CommandProcessingResult createEmployee(
            @Parameter(hidden = true) final AdorsysUserAndStaffApiResourceSwagger.CreateEmployeeRequest request) {
        this.context.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME_FOR_PERMISSIONS);
        final String json = this.toApiJsonSerializer.serialize(request);
        final JsonElement jsonElement = this.fromJsonHelper.parse(json);
        final JsonCommand command = JsonCommand.fromJsonElement(null, jsonElement, this.fromJsonHelper);
        return this.adorsysUserAndStaffWritePlatformService.createEmployee(command);
    }

    @GET
    @Path("{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve an Employee", description = "Returns the details of an Employee.")
    public EmployeeData retrieveEmployee(@PathParam("userId") @Parameter(description = "userId") final Long userId) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);
        return this.adorsysUserAndStaffReadPlatformService.retrieveEmployee(userId);
    }

    @PUT
    @Path("{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update an Employee", description = "Updates the details of an employee.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AdorsysUserAndStaffApiResourceSwagger.PutEmployeeRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AdorsysUserAndStaffApiResourceSwagger.PutEmployeeResponse.class)))
    })
    public CommandProcessingResult updateEmployee(@PathParam("userId") @Parameter(description = "userId") final Long userId,
                                                  @Parameter(hidden = true) final AdorsysUserAndStaffApiResourceSwagger.PutEmployeeRequest request) {
        this.context.authenticatedUser().validateHasUpdatePermission(RESOURCE_NAME_FOR_PERMISSIONS);
        final String json = this.toApiJsonSerializer.serialize(request);
        final JsonElement jsonElement = this.fromJsonHelper.parse(json);
        final JsonCommand command = JsonCommand.fromJsonElement(userId, jsonElement, this.fromJsonHelper);
        return this.adorsysUserAndStaffWritePlatformService.updateEmployee(userId, command);
    }
}
