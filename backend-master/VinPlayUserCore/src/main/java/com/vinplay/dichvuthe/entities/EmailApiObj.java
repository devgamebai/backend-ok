/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

import java.util.List;

public class EmailApiObj {

    private String subject;
    private String fromName;
    private String siteName;
    private String domain;
    private String fromEmail;
    private String toEmail;
    private String params;
    private String template;
    private String module;
    private String description;

    public EmailApiObj() {}

    public EmailApiObj(String subject, String fromName, String siteName, String domain, String fromEmail,
        String toEmail, String params, String template, String module, String description) {
        this.subject = subject;
        this.fromName = fromName;
        this.siteName = siteName;
        this.domain = domain;
        this.fromEmail = fromEmail;
        this.toEmail = toEmail;
        this.params = params;
        this.template = template;
        this.module = module;
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

