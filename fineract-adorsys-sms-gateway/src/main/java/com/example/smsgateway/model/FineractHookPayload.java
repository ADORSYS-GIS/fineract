package com.example.smsgateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class FineractHookPayload {
    private String entityName;
    private String actionName;
    private Long clientId;
    private Request request;
    private Response response;

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {
        private java.math.BigDecimal transactionAmount;
    }

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Long savingsId;
    }
}
