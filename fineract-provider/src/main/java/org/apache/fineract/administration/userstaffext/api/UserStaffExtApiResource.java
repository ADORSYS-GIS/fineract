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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.administration.userstaffext.data.CreateUserStaffExtRequest;
import org.apache.fineract.administration.userstaffext.data.UpdateUserStaffExtRequest;
import org.apache.fineract.administration.userstaffext.data.UserStaffExtData;
import org.apache.fineract.administration.userstaffext.service.UserStaffExtReadPlatformService;
import org.apache.fineract.administration.userstaffext.service.UserStaffExtWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.stereotype.Component;

/**
 * REST API resource for managing User-Staff extensions. This resource provides endpoints to create,
 * update, and retrieve users with associated staff members in atomic operations.
 */
@Path("/v1/userstaffext")
@Component
@Tag(name = "User Staff Extension", description = "API for creating and managing users with associated staff members in a single atomic operation. "
        + "This extension ensures that users are always linked to staff members.")
@RequiredArgsConstructor
public class UserStaffExtApiResource {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "USERSTAFFEXT";

    private final PlatformSecurityContext context;
    private final UserStaffExtReadPlatformService readPlatformService;
    private final UserStaffExtWritePlatformService writePlatformService;
    private final DefaultToApiJsonSerializer<UserStaffExtData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final FromJsonHelper fromJsonHelper;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve All User-Staff Entities", description = "Returns a list of all users that have linked staff members.\n\n"
            + "Example Requests:\n\n" + "userstaffext")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserStaffExtApiResourceSwagger.GetUserStaffExtResponse.class, type = "array"))) })
    public String retrieveAll(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final Collection<UserStaffExtData> userStaffExtData = this.readPlatformService.retrieveAll();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, userStaffExtData);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve User-Staff Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for user-staff applications. "
            + "The template data returned consists of allowed offices and available roles.\n\n" + "Example Requests:\n\n"
            + "userstaffext/template")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserStaffExtApiResourceSwagger.GetUserStaffExtTemplateResponse.class))) })
    public String template(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final UserStaffExtData userStaffExtData = this.readPlatformService.retrieveTemplate();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, userStaffExtData);
    }

    @GET
    @Path("{userId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a User-Staff Entity", description = "Returns the details of a user with associated staff information.\n\n"
            + "Example Requests:\n\n" + "userstaffext/1")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserStaffExtApiResourceSwagger.GetUserStaffExtResponse.class))) })
    public String retrieveOne(@PathParam("userId") @Parameter(description = "userId") final Long userId, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final UserStaffExtData userStaffExtData = this.readPlatformService.retrieveOne(userId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, userStaffExtData);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create User and Staff", description = "Creates both a staff member and a user account, linking them together in a single atomic transaction. "
            + "If either the staff or user creation fails, the entire transaction is rolled back.\n\n" + "Mandatory Fields: \n"
            + "username, password, repeatPassword, email, roles, firstname, lastname, officeId\n\n" + "Optional Fields: \n"
            + "passwordNeverExpires, isSelfServiceUser, sendPasswordToEmail, mobileNo, externalId, isLoanOfficer, isActive, joiningDate, emailAddress\n\n"
            + "Example Requests:\n\n" + "userstaffext")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = UserStaffExtApiResourceSwagger.PostUserStaffExtRequest.class)))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserStaffExtApiResourceSwagger.PostUserStaffExtResponse.class))) })
    public String create(@Parameter(hidden = true) final CreateUserStaffExtRequest request) {
        this.context.authenticatedUser().validateHasCreatePermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final String json = this.toApiJsonSerializer.serializeRequest(request);
        final JsonCommand command = JsonCommand.fromExistingCommand(null, this.fromJsonHelper.parse(json));

        final CommandProcessingResult result = this.writePlatformService.createUserStaffExt(command);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{userId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update User and Staff", description = "Updates both the user and staff information in a single atomic transaction. "
            + "Only the fields provided in the request will be updated.\n\n" + "Optional Fields: \n"
            + "email, roles, passwordNeverExpires, firstname, lastname, mobileNo, externalId, isLoanOfficer, isActive, joiningDate, emailAddress\n\n"
            + "Example Requests:\n\n" + "userstaffext/1")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = UserStaffExtApiResourceSwagger.PutUserStaffExtRequest.class)))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserStaffExtApiResourceSwagger.PutUserStaffExtResponse.class))) })
    public String update(@PathParam("userId") @Parameter(description = "userId") final Long userId,
            @Parameter(hidden = true) final UpdateUserStaffExtRequest request) {
        this.context.authenticatedUser().validateHasUpdatePermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final String json = this.toApiJsonSerializer.serializeRequest(request);
        final JsonCommand command = JsonCommand.fromExistingCommand(userId, this.fromJsonHelper.parse(json));

        final CommandProcessingResult result = this.writePlatformService.updateUserStaffExt(userId, command);

        return this.toApiJsonSerializer.serialize(result);
    }
}
