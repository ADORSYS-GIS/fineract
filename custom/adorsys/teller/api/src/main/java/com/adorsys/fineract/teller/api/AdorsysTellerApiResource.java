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
package com.adorsys.fineract.teller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Path("/v1/tellers")
@Component
@Scope("singleton")
@Tag(name = "Adorsys Tellers", description = "Adorsys Custom Teller Operations")
@RequiredArgsConstructor
public class AdorsysTellerApiResource {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @POST
    @Path("{tellerId}/cashiers/{cashierId}/endOfDaySettle")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "End of Day Settlement", description = "Custom implementation of End of Day Settlement")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK") })
    public CommandProcessingResult endOfDaySettlement(
            @PathParam("tellerId") @Parameter(description = "tellerId") final Long tellerId,
            @PathParam("cashierId") @Parameter(description = "cashierId") final Long cashierId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper request = new CommandWrapper(null, null, null, null, null,
                "ENDOFDAYSETTLEMENT", "TELLER", tellerId, cashierId,
                "/tellers/" + tellerId + "/cashiers/" + cashierId + "/endOfDaySettle",
                apiRequestBodyAsJson, null, null, null, null, null, null, null, null, null);

        return this.commandsSourceWritePlatformService.logCommandSource(request);
    }
}
