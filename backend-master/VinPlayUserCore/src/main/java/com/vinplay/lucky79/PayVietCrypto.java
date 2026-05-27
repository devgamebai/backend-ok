package com.vinplay.lucky79;

public class PayVietCrypto {
    private Boolean status;
    private String result;
    private String transaction_id;


    public PayVietCrypto(Boolean status, String result, String transaction_id) {
        this.status = status;
        this.result = result;
        this.transaction_id = transaction_id;
    }

    public PayVietCrypto() {
    }

    public Boolean getStatus() {
        return this.status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getTransaction_id() {
        return this.transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
    }

    public String getResult() {
        return this.result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
