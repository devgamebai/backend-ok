package com.vinplay.lucky79;

public class TheCaoResponse {
    private String response_code;
    private String message;
    private String serial;
    private String status;
    private String code;
    private String telco;
    private String date;
    private double declared_value;
    private double amount;
    private String trans_id;
    private String request_id;

    public TheCaoResponse(String response_code, String message, String code, int declared_value, int amount, String trans_id, String serial, String status, String telco, String date, String request_id) {
        this.response_code = response_code;
        this.message = message;
        this.serial = serial;
        this.status = status;
        this.telco = telco;
        this.date = date;
        this.code = code;
        this.declared_value = declared_value;
        this.amount = amount;
        this.trans_id = trans_id;
        this.request_id = request_id;
    }

    public TheCaoResponse() {
    }

    public String getTrans_id() {
        return this.trans_id;
    }

    public void setTrans_id(String trans_id) {
        this.trans_id = trans_id;
    }

    public String getResponse_code() {
        return this.response_code;
    }

    public void setResponse_code(String response_code) {
        this.response_code = response_code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDeclared_value() {
        return this.declared_value;
    }

    public void setDeclared_value(double declared_value) {
        this.declared_value = declared_value;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTelco() {
        return this.telco;
    }

    public void setTelco(String telco) {
        this.telco = telco;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequest_id() {
        return this.request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }
}
