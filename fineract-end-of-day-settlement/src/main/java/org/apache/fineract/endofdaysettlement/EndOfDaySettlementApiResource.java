package org.apache.fineract.endofdaysettlement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.endofdaysettlement.api.EndOfDaySettlementApiResourceSwagger;

@RestController
@RequestMapping("/v1/cashiers/{cashierId}/settle")
@Tag(name = "Cash Settlement", description = "Manages end-of-day cash settlement for cashiers, including discrepancy handling.")
public class EndOfDaySettlementApiResource {

    private final DefaultToApiJsonSerializer<CommandProcessingResult> toApiJsonSerializer;
    private final EndOfDaySettlementService endOfDaySettlementService;
    private final PlatformSecurityContext context;

    @Autowired
    public EndOfDaySettlementApiResource(DefaultToApiJsonSerializer<CommandProcessingResult> toApiJsonSerializer, EndOfDaySettlementService endOfDaySettlementService, PlatformSecurityContext context) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.endOfDaySettlementService = endOfDaySettlementService;
        this.context = context;
    }

    @PostMapping
    @Operation(summary = "Perform end-of-day cash settlement for a cashier", description = "This API is used to perform end-of-day cash settlement for a cashier. It handles discrepancies (overages and shortages) by creating appropriate journal entries.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = EndOfDaySettlementApiResourceSwagger.PostCashiersCashierIdSettleRequest.class)))
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EndOfDaySettlementApiResourceSwagger.PostCashiersCashierIdSettleResponse.class)))
    public String settle(@PathVariable("cashierId") final Long cashierId, @org.springframework.web.bind.annotation.RequestBody final String apiRequestBodyAsJson, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission("SETTLECASHFROMCASHIER_TELLER");

        final JsonCommand command = new JsonCommand().fromJson(apiRequestBodyAsJson);
        final CommandProcessingResult result = this.endOfDaySettlementService.settle(cashierId, command);
        return this.toApiJsonSerializer.serialize(result);
    }
}
