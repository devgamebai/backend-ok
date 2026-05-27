/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.entities;

import java.util.List;

public class EmailObj {
    private String subject;
    private String content;
    private List<String> receives;

    public EmailObj(String subject, String content, List<String> receives) {
        this.subject = subject;
        this.content = content;
        this.receives = receives;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getReceives() {
        return this.receives;
    }

    public void setReceives(List<String> receives) {
        this.receives = receives;
    }
}

