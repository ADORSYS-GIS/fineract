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

import static jakarta.ws.rs.HttpMethod.POST;

import java.util.Map;
import org.apache.fineract.batch.command.CommandContext;
import org.apache.fineract.batch.command.CommandStrategyRegistrar;
import org.springframework.stereotype.Component;

/**
 * Registers batch command strategies for operations not supported by upstream Apache Fineract:
 * <ul>
 * <li>POST /journalentries — GL journal entry creation</li>
 * <li>POST /accounttransfers — savings-to-savings transfers (delegates to upstream
 * createAccountTransferCommandStrategy)</li>
 * <li>PUT /savingsaccounts/{id}?command=approve — savings account approval</li>
 * <li>PUT /savingsaccounts/{id}?command=activate — savings account activation</li>
 * </ul>
 */
@Component
public class AdorsysBatchStrategyRegistrar implements CommandStrategyRegistrar {

    private static final String NUMBER_REGEX = "\\d+";
    private static final String OPTIONAL_COMMAND_PARAM_REGEX = "(\\?command=[\\w\\-]+)?";

    @Override
    public void register(Map<CommandContext, String> strategies) {
        // Journal entries: POST /v1/journalentries or POST /v1/journalentries?command=...
        strategies.put(CommandContext.resource("v1\\/journalentries" + OPTIONAL_COMMAND_PARAM_REGEX).method(POST).build(),
                "createJournalEntryCommandStrategy");

        // Account transfers: POST /v1/accounttransfers
        strategies.put(CommandContext.resource("v1\\/accounttransfers").method(POST).build(), "createAccountTransferCommandStrategy");

        // Savings account approval: POST /v1/savingsaccounts/{id}?command=approve
        // Fineract's batch API convention uses POST for state-transition commands
        // (same as activateClientCommandStrategy, approveLoanCommandStrategy in core).
        // The strategy delegates to SavingsAccountsApiResource.update() directly.
        strategies.put(CommandContext.resource("v1\\/savingsaccounts\\/" + NUMBER_REGEX + "\\?command=approve").method(POST).build(),
                "approveSavingsAccountCommandStrategy");

        // Savings account activation: POST /v1/savingsaccounts/{id}?command=activate
        strategies.put(CommandContext.resource("v1\\/savingsaccounts\\/" + NUMBER_REGEX + "\\?command=activate").method(POST).build(),
                "activateSavingsAccountCommandStrategy");
    }
}
