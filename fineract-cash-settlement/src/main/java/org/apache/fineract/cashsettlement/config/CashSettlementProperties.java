package org.apache.fineract.cashsettlement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fineract.cash-settlement")
public class CashSettlementProperties {

    private Long cashOverageAccountId;
    private Long cashShortageAccountId;

    public Long getCashOverageAccountId() {
        return cashOverageAccountId;
    }

    public void setCashOverageAccountId(Long cashOverageAccountId) {
        this.cashOverageAccountId = cashOverageAccountId;
    }

    public Long getCashShortageAccountId() {
        return cashShortageAccountId;
    }

    public void setCashShortageAccountId(Long cashShortageAccountId) {
        this.cashShortageAccountId = cashShortageAccountId;
    }
}
