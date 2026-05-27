package com.vinplay.usercore.utils;

public class TelegramConfig {
    private String chatId;
    private String bootToken;

    public TelegramConfig() {}

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getBootToken() { return bootToken; }
    public void setBootToken(String bootToken) { this.bootToken = bootToken; }
}
