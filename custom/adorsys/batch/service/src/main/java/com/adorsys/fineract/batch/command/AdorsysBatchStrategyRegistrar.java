package com.adorsys.fineract.batch.command;

import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;

import java.util.Map;
import org.apache.fineract.batch.command.CommandContext;
import org.apache.fineract.batch.command.CommandStrategyRegistrar;
import org.springframework.stereotype.Component;

/**
 * Registers batch command strategies for operations not supported by upstream Apache Fineract:
 * <ul>
 *   <li>POST /journalentries — GL journal entry creation</li>
 *   <li>POST /accounttransfers — savings-to-savings transfers</li>
 *   <li>PUT /savingsaccounts/{id}?command=approve — savings account approval</li>
 *   <li>PUT /savingsaccounts/{id}?command=activate — savings account activation</li>
 * </ul>
 */
@Component
public class AdorsysBatchStrategyRegistrar implements CommandStrategyRegistrar {

    private static final String NUMBER_REGEX = "\\d+";
    private static final String OPTIONAL_COMMAND_PARAM_REGEX = "(\\?command=[\\w\\-]+)?";

    @Override
    public void register(Map<CommandContext, String> strategies) {
        // Journal entries: POST /v1/journalentries or POST /v1/journalentries?command=...
        strategies.put(
                CommandContext.resource("v1\\/journalentries" + OPTIONAL_COMMAND_PARAM_REGEX).method(POST).build(),
                "createJournalEntryCommandStrategy");

        // Account transfers: POST /v1/accounttransfers
        strategies.put(
                CommandContext.resource("v1\\/accounttransfers").method(POST).build(),
                "createAccountTransferCommandStrategy");

        // Savings account approval: POST /v1/savingsaccounts/{id}?command=approve
        // Note: Fineract API uses PUT for these, but batch requests come as POST
        strategies.put(
                CommandContext.resource("v1\\/savingsaccounts\\/" + NUMBER_REGEX + "\\?command=approve").method(POST).build(),
                "approveSavingsAccountCommandStrategy");

        // Savings account activation: POST /v1/savingsaccounts/{id}?command=activate
        strategies.put(
                CommandContext.resource("v1\\/savingsaccounts\\/" + NUMBER_REGEX + "\\?command=activate").method(POST).build(),
                "activateSavingsAccountCommandStrategy");
    }
}
