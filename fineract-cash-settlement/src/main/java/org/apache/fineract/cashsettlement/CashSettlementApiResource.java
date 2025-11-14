package org.apache.fineract.cashsettlement;

import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@RestController
@RequestMapping("/v1/cashiers/{cashierId}/settle")
public class CashSettlementApiResource {

    private final DefaultToApiJsonSerializer<CommandProcessingResult> toApiJsonSerializer;
    private final CashSettlementService cashSettlementService;

    @Autowired
    public CashSettlementApiResource(DefaultToApiJsonSerializer<CommandProcessingResult> toApiJsonSerializer, CashSettlementService cashSettlementService) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.cashSettlementService = cashSettlementService;
    }

    @PostMapping
    public String settle(@PathVariable("cashierId") final Long cashierId, @RequestBody final String apiRequestBodyAsJson, @Context final UriInfo uriInfo) {
        final JsonCommand command = new JsonCommand().fromJson(apiRequestBodyAsJson);
        final CommandProcessingResult result = this.cashSettlementService.settle(cashierId, command);
        return this.toApiJsonSerializer.serialize(result);
    }
}
