package org.apache.fineract.endofdaysettlement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

final class EndOfDaySettlementApiResourceSwagger {

    private EndOfDaySettlementApiResourceSwagger() {}

    @Data
    @Schema(description = "PostCashiersCashierIdSettleRequest")
    public static final class PostCashiersCashierIdSettleRequest {
        @Schema(example = "1000.00")
        public Double amount;
        @Schema(example = "01 January 2023")
        public String txnDate;
        @Schema(example = "en")
        public String locale;
        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
        @Schema(example = "USD")
        public String currencyCode;
    }

    @Data
    @Schema(description = "PostCashiersCashierIdSettleResponse")
    public static final class PostCashiersCashierIdSettleResponse {
        @Schema(example = "1")
        public Long officeId;
        @Schema(example = "1")
        public Long cashierId;
        @Schema(example = "1")
        public Long resourceId;
    }
}
