/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay._1pay;

import com.vinplay._1pay.SerialsObj;
import java.util.List;

public class TransactionObj {
    private String id;
    private String cif;
    private String amount;
    private String commision;
    private String fee;
    private String price;
    private String quantity;
    private String realAmount;
    private String lastBalance;
    private String currentBalance;
    private String info;
    private String serviceType;
    private String subject;
    private String errorCode;
    private String errorMessage;
    private String transactionStatus;
    private String creationDate;
    private String serviceCode;
    private String serviceName;
    private String telcoType;
    private List<SerialsObj> serials;

    public TransactionObj(String id, String cif, String amount, String commision, String fee, String price, String quantity, String realAmount, String lastBalance, String currentBalance, String info, String serviceType, String subject, String errorCode, String errorMessage, String transactionStatus, String creationDate, String serviceCode, String serviceName, String telcoType, List<SerialsObj> serials) {
        this.id = id;
        this.cif = cif;
        this.amount = amount;
        this.commision = commision;
        this.fee = fee;
        this.price = price;
        this.quantity = quantity;
        this.realAmount = realAmount;
        this.lastBalance = lastBalance;
        this.currentBalance = currentBalance;
        this.info = info;
        this.serviceType = serviceType;
        this.subject = subject;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.transactionStatus = transactionStatus;
        this.creationDate = creationDate;
        this.serviceCode = serviceCode;
        this.serviceName = serviceName;
        this.telcoType = telcoType;
        this.serials = serials;
    }

    public TransactionObj() {
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCif() {
        return this.cif;
    }

    public void setCif(String cif) {
        this.cif = cif;
    }

    public String getAmount() {
        return this.amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCommision() {
        return this.commision;
    }

    public void setCommision(String commision) {
        this.commision = commision;
    }

    public String getFee() {
        return this.fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public String getPrice() {
        return this.price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getQuantity() {
        return this.quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getRealAmount() {
        return this.realAmount;
    }

    public void setRealAmount(String realAmount) {
        this.realAmount = realAmount;
    }

    public String getLastBalance() {
        return this.lastBalance;
    }

    public void setLastBalance(String lastBalance) {
        this.lastBalance = lastBalance;
    }

    public String getCurrentBalance() {
        return this.currentBalance;
    }

    public void setCurrentBalance(String currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getInfo() {
        return this.info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getServiceType() {
        return this.serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTransactionStatus() {
        return this.transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public String getCreationDate() {
        return this.creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTelcoType() {
        return this.telcoType;
    }

    public void setTelcoType(String telcoType) {
        this.telcoType = telcoType;
    }

    public List<SerialsObj> getSerials() {
        return this.serials;
    }

    public void setSerials(List<SerialsObj> serials) {
        this.serials = serials;
    }
}

