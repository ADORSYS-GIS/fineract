package com.adorsys.fineract.batch.command;

import static org.apache.fineract.batch.command.CommandStrategyUtils.relativeUrlWithoutVersion;

import com.google.common.base.Splitter;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.portfolio.savings.api.SavingsAccountsApiResource;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Batch command strategy for activating savings accounts.
 * Delegates to {@link SavingsAccountsApiResource#update(Long, String, String)} with command=activate.
 */
@Component
@RequiredArgsConstructor
public class ActivateSavingsAccountCommandStrategy implements CommandStrategy {

    private final SavingsAccountsApiResource savingsAccountsApiResource;

    @Override
    public BatchResponse execute(BatchRequest request, @SuppressWarnings("unused") UriInfo uriInfo) {
        String relativeUrl = relativeUrlWithoutVersion(request);
        // URL: savingsaccounts/{id}?command=activate
        String pathPart = relativeUrl.contains("?") ? relativeUrl.substring(0, relativeUrl.indexOf('?')) : relativeUrl;
        List<String> pathParameters = Splitter.on('/').splitToList(pathPart);
        Long savingsAccountId = Long.parseLong(pathParameters.get(1));

        String responseBody = savingsAccountsApiResource.update(savingsAccountId, request.getBody(), "activate");

        return new BatchResponse().setRequestId(request.getRequestId()).setStatusCode(HttpStatus.SC_OK).setBody(responseBody)
                .setHeaders(request.getHeaders());
    }
}
