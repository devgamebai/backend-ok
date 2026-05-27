/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vinplay.dichvuthe.client;

/**
 *
 * @author HA
 */
public class GachTheException
extends Exception {
    private static final long serialVersionUID = 1L;
    private String message;
    private int errorCode;

    public GachTheException(String message, int errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}