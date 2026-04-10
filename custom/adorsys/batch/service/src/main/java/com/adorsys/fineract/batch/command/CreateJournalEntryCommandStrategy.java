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
