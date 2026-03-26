package com.adorsys.fineract.batch.command;

import com.google.gson.Gson;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Batch command strategy for creating account transfers between savings accounts.
 * Uses the same command processing pipeline as {@code AccountTransfersApiResource.create()}.
 */
@Component
@RequiredArgsConstructor
public class CreateAccountTransferCommandStrategy implements CommandStrategy {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Override
    public BatchResponse execute(BatchRequest request, @SuppressWarnings("unused") UriInfo uriInfo) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .createAccountTransfer()
                .withJson(request.getBody())
                .build();

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
        final String responseBody = new Gson().toJson(result);

        return new BatchResponse().setRequestId(request.getRequestId()).setStatusCode(HttpStatus.SC_OK).setBody(responseBody)
                .setHeaders(request.getHeaders());
    }
}
