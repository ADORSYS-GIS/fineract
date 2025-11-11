# SMS Gateway

This project is a Spring Boot application that acts as a gateway between Fineract and an SMS provider (Twilio). It listens for webhook notifications from Fineract and sends SMS messages to clients based on the events.

## Setup

### Environment Variables

Before running the application, you need to set the following environment variables:

| Variable | Description |
|---|---|
| `TWILIO_ACCOUNT_SID` | Your Twilio account SID. |
| `TWILIO_AUTH_TOKEN` | Your Twilio auth token. |
| `TWILIO_SENDER_NUMBER` | Your Twilio phone number. |
| `FINERACT_API_URL` | The URL of your Fineract instance. |
| `FINERACT_API_USER` | The username for your Fineract instance. |
| `FINERACT_API_PASSWORD` | The password for your Fineract instance. |

### Running the Application

You can run the application using the following command:

```bash
mvn spring-boot:run
```

## Flow of the Messaging System

1. **Fineract Webhook:** Fineract is configured to send a webhook notification to the `/sms/` endpoint of this application when a transaction (deposit or withdrawal) occurs.
2. **SmsController:** The `SmsController` receives the webhook payload, which is a JSON object containing information about the transaction.
3. **MessageService:** The `SmsController` passes the payload to the `MessageService`, which is responsible for creating and sending the SMS message.
4. **FineractService:** The `MessageService` uses the `FineractService` to fetch the SMS template and the client's phone number from the Fineract API.
5. **SmsService:** The `MessageService` uses the `SmsService` to send the SMS message to the client using the Twilio API.

## How it was Setup

The application is a standard Spring Boot application. It uses the following dependencies:

* **spring-boot-starter-web:** For creating the REST endpoint.
* **spring-boot-starter-test:** For testing.
* **twilio:** For sending SMS messages.
* **jackson-databind:** For parsing JSON.

The application is configured in the `application.properties` file. The Twilio and Fineract credentials are read from environment variables to avoid storing them in the codebase.
