# Fineract Hooks and SMS Gateway Integration

This document provides a detailed explanation of how the SMS gateway integrates with Fineract using webhooks to send SMS notifications to clients.

## Architectural Diagram

```
+-----------------+      +------------------+      +-----------------+
| Fineract        |----->| SMS Gateway      |----->| Twilio API      |
| (Webhook Event) |      | (Spring Boot App)|      | (SMS Provider)  |
+-----------------+      +------------------+      +-----------------+
```

## Fineract Hooks for Messaging

Fineract provides a webhook mechanism that allows you to send notifications to a specified URL when certain events occur. In this project, we are using webhooks to notify the SMS gateway when a deposit or withdrawal transaction occurs.

### Configuration

To configure the Fineract hooks, you need to do the following:

1.  **Create a Webhook:** In Fineract, navigate to the "Admin" -> "System" -> "Hooks" section and create a new hook.
2.  **Configure the Hook:**
    *   **Name:** Give the hook a descriptive name, such as "SMS Gateway".
    *   **URL:** Set the URL to the `/sms/` endpoint of the SMS gateway application (e.g., `http://localhost:8080/sms/`).
    *   **Events:** Select the events that you want to trigger the webhook. In this case, we are interested in the "Deposit" and "Withdrawal" events for savings accounts.
    *   **Entities:** Select the entities that the webhook should apply to. In this case, we are interested in "Savings Account" entities.
    *   **Payload URL:** The URL to which the payload will be sent.

### Entities Used

The following Fineract entities are used in this integration:

*   **Client:** Represents a client of the financial institution. We use the client's ID to fetch their phone number.
*   **Savings Account:** Represents a client's savings account. We use the savings account ID to fetch the account balance.
*   **Transaction:** Represents a financial transaction, such as a deposit or withdrawal.

### Events

The following Fineract events are used to trigger the SMS notifications:

*   **Deposit:** This event is triggered when a client makes a deposit into their savings account.
*   **Withdrawal:** This event is triggered when a client makes a withdrawal from their savings account.

## SMS Provider Integration

The SMS gateway is integrated with the Twilio API to send SMS messages.

### How the SMS Gateway Receives Events

1.  **Webhook Notification:** When a deposit or withdrawal event occurs in Fineract, a webhook notification is sent to the `/sms/` endpoint of the SMS gateway.
2.  **SmsController:** The `SmsController` receives the webhook payload, which is a JSON object containing information about the transaction.
3.  **Payload Deserialization:** The `SmsController` uses the Jackson library to deserialize the JSON payload into a `FineractHookPayload` object.

### How the SMS Gateway Sends Messages

1.  **MessageService:** The `SmsController` passes the `FineractHookPayload` object to the `MessageService`.
2.  **Data Fetching:** The `MessageService` uses the `FineractService` to fetch the following information from the Fineract API:
    *   The SMS template for the transaction type (deposit or withdrawal).
    *   The client's phone number.
    *   The client's account balance.
3.  **Message Construction:** The `MessageService` constructs the SMS message by replacing the placeholders in the template with the fetched data.
4.  **SmsService:** The `MessageService` uses the `SmsService` to send the SMS message to the client using the Twilio API.
