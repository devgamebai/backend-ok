/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payment.entities;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AdminBank
implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String bankName;
    private String customerName;
    private String bankNumber;
    private Integer status;
    private Timestamp createDate;
    private String branch;
    private Timestamp updateDate;
    private String lastEditor;

    public AdminBank() {
    }

    public AdminBank(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.bankName = rs.getString("bank_name");
        this.customerName = rs.getString("customer_name");
        this.bankNumber = rs.getString("bank_number");
        this.status = rs.getInt("status");
        this.createDate = rs.getTimestamp("create_date");
        this.branch = rs.getString("branch");
        this.updateDate = rs.getTimestamp("update_date");
        this.lastEditor = rs.getString("last_editor");
    }

    public AdminBank(Long id, String bankName, String customerName, String bankNumber, Integer status, Timestamp createDate, String branch, Timestamp updateDate, String lastEditor) {
        this.id = id;
        this.bankName = bankName;
        this.customerName = customerName;
        this.bankNumber = bankNumber;
        this.status = status;
        this.createDate = createDate;
        this.branch = branch;
        this.updateDate = updateDate;
        this.lastEditor = lastEditor;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBankName() {
        return this.bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getBankNumber() {
        return this.bankNumber;
    }

    public void setBankNumber(String bankNumber) {
        this.bankNumber = bankNumber;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Timestamp getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getBranch() {
        return this.branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Timestamp getUpdateDate() {
        return this.updateDate;
    }

    public void setUpdateDate(Timestamp updateDate) {
        this.updateDate = updateDate;
    }

    public String getLastEditor() {
        return this.lastEditor;
    }

    public void setLastEditor(String lastEditor) {
        this.lastEditor = lastEditor;
    }
}

