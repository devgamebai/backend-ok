/*
 * Decompiled with CFR 0.152.
 */
package game.third.hooks.request;

public class BalanceRequest {
    private String memberAccount;
    private String operatorCode;
    private String productCode;
    private String currency;
    private String sign;
    private String requestTime;

    public String getMemberAccount() {
        return this.memberAccount;
    }

    public void setMemberAccount(String memberAccount) {
        this.memberAccount = memberAccount;
    }

    public String getOperatorCode() {
        return this.operatorCode;
    }

    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public String getProductCode() {
        return this.productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getRequestTime() {
        return this.requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }
}

