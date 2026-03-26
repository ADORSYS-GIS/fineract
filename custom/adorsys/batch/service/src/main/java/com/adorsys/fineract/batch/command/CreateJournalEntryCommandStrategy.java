package com.adorsys.fineract.batch.command;

import static org.apache.fineract.batch.command.CommandStrategyUtils.relativeUrlWithoutVersion;

import jakarta.ws.rs.core.UriInfo;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.accounting.journalentry.api.JournalEntriesApiResource;
import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.command.CommandStrategyUtils;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Batch command strategy for creating GL journal entries.
 * Delegates to {@link JournalEntriesApiResource#createGLJournalEntry(String, String)}.
 */
@Component
@RequiredArgsConstructor
public class CreateJournalEntryCommandStrategy implements CommandStrategy {

    private final JournalEntriesApiResource journalEntriesApiResource;

    @Override
    public BatchResponse execute(BatchRequest request, @SuppressWarnings("unused") UriInfo uriInfo) {
        String relativeUrl = relativeUrlWithoutVersion(request);
        String command = null;
        if (relativeUrl.indexOf('?') > 0) {
            Map<String, String> queryParameters = CommandStrategyUtils.getQueryParameters(relativeUrl);
            command = queryParameters.get("command");
        }

        String responseBody = journalEntriesApiResource.createGLJournalEntry(request.getBody(), command);

        return new BatchResponse().setRequestId(request.getRequestId()).setStatusCode(HttpStatus.SC_OK).setBody(responseBody)
                .setHeaders(request.getHeaders());
    }
}
